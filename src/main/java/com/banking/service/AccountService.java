package com.banking.service;

import com.banking.dto.AccountDto;
import com.banking.dto.TransactionDto;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.BankingException;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepo;
    private final TransactionRepository txRepo;

    private final AtomicLong accountCounter = new AtomicLong(100);

    // ── Account CRUD ──────────────────────────────────────────────────────────

    @Transactional
    public AccountDto.Response createAccount(AccountDto.CreateRequest req) {
        String currency = req.getCurrency() != null ? req.getCurrency().toUpperCase() : "USD";
        String accountNumber = generateAccountNumber(req.getAccountType());

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .ownerName(req.getOwnerName())
                .accountType(req.getAccountType())
                .balance(req.getInitialDeposit())
                .currency(currency)
                .status(Account.AccountStatus.ACTIVE)
                .build();

        account = accountRepo.save(account);

        // Record initial deposit as a transaction
        if (req.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            saveTransaction(account, Transaction.TransactionType.CREDIT,
                    req.getInitialDeposit(), account.getBalance(), "Initial deposit", null);
        }

        log.info("Created account {} for {}", accountNumber, req.getOwnerName());
        return AccountDto.Response.from(account);
    }

    @Transactional(readOnly = true)
    public AccountDto.Response getAccount(Long id) {
        return AccountDto.Response.from(findById(id));
    }

    @Transactional(readOnly = true)
    public AccountDto.Response getAccountByNumber(String accountNumber) {
        return AccountDto.Response.from(findByNumber(accountNumber));
    }

    @Transactional(readOnly = true)
    public List<AccountDto.Response> getAllAccounts() {
        return accountRepo.findAll().stream()
                .map(AccountDto.Response::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccountDto.Response> searchAccounts(String ownerName) {
        return accountRepo.searchByOwnerName(ownerName).stream()
                .map(AccountDto.Response::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountDto.Response closeAccount(Long id) {
        Account account = findById(id);
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BankingException("Cannot close account with non-zero balance. Current balance: "
                    + account.getBalance() + " " + account.getCurrency());
        }
        account.setStatus(Account.AccountStatus.CLOSED);
        return AccountDto.Response.from(accountRepo.save(account));
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    @Transactional
    public TransactionDto.Response deposit(Long accountId, AccountDto.AmountRequest req) {
        Account account = findActiveById(accountId);
        account.setBalance(account.getBalance().add(req.getAmount()));
        accountRepo.save(account);

        Transaction tx = saveTransaction(account, Transaction.TransactionType.CREDIT,
                req.getAmount(), account.getBalance(),
                req.getDescription() != null ? req.getDescription() : "Deposit", null);

        log.info("Deposited {} to account {}", req.getAmount(), account.getAccountNumber());
        return TransactionDto.Response.from(tx);
    }

    @Transactional
    public TransactionDto.Response withdraw(Long accountId, AccountDto.AmountRequest req) {
        Account account = findActiveById(accountId);

        if (account.getBalance().compareTo(req.getAmount()) < 0) {
            throw new BankingException("Insufficient funds. Available: "
                    + account.getBalance() + " " + account.getCurrency());
        }

        account.setBalance(account.getBalance().subtract(req.getAmount()));
        accountRepo.save(account);

        Transaction tx = saveTransaction(account, Transaction.TransactionType.DEBIT,
                req.getAmount(), account.getBalance(),
                req.getDescription() != null ? req.getDescription() : "Withdrawal", null);

        log.info("Withdrew {} from account {}", req.getAmount(), account.getAccountNumber());
        return TransactionDto.Response.from(tx);
    }

    @Transactional
    public TransactionDto.TransferResponse transfer(Long fromAccountId, AccountDto.TransferRequest req) {
        Account from = findActiveById(fromAccountId);
        Account to = findByNumber(req.getToAccountNumber());

        if (to.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BankingException("Target account is not active.");
        }
        if (from.getAccountNumber().equals(to.getAccountNumber())) {
            throw new BankingException("Cannot transfer to the same account.");
        }
        if (from.getBalance().compareTo(req.getAmount()) < 0) {
            throw new BankingException("Insufficient funds. Available: "
                    + from.getBalance() + " " + from.getCurrency());
        }

        String sharedRef = "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String desc = req.getDescription() != null ? req.getDescription() : "Transfer";

        from.setBalance(from.getBalance().subtract(req.getAmount()));
        to.setBalance(to.getBalance().add(req.getAmount()));
        accountRepo.save(from);
        accountRepo.save(to);

        saveTransaction(from, Transaction.TransactionType.TRANSFER_OUT,
                req.getAmount(), from.getBalance(), desc, to.getAccountNumber(), sharedRef);
        saveTransaction(to, Transaction.TransactionType.TRANSFER_IN,
                req.getAmount(), to.getBalance(), desc, from.getAccountNumber(), sharedRef);

        log.info("Transferred {} from {} to {} [ref={}]",
                req.getAmount(), from.getAccountNumber(), to.getAccountNumber(), sharedRef);

        return TransactionDto.TransferResponse.builder()
                .referenceId(sharedRef)
                .fromAccount(from.getAccountNumber())
                .toAccount(to.getAccountNumber())
                .amount(req.getAmount())
                .fromBalanceAfter(from.getBalance())
                .toBalanceAfter(to.getBalance())
                .status("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto.Response> getTransactions(Long accountId, int page, int size) {
        findById(accountId); // verify account exists
        return txRepo.findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(page, size))
                .map(TransactionDto.Response::from);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long accountId) {
        return findById(accountId).getBalance();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Account findById(Long id) {
        return accountRepo.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
    }

    private Account findActiveById(Long id) {
        Account a = findById(id);
        if (a.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BankingException("Account " + a.getAccountNumber() + " is not active.");
        }
        return a;
    }

    private Account findByNumber(String number) {
        return accountRepo.findByAccountNumber(number)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + number));
    }

    private Transaction saveTransaction(Account account, Transaction.TransactionType type,
                                        BigDecimal amount, BigDecimal balanceAfter,
                                        String description, String counterpart) {
        return saveTransaction(account, type, amount, balanceAfter, description, counterpart,
                "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
    }

    private Transaction saveTransaction(Account account, Transaction.TransactionType type,
                                        BigDecimal amount, BigDecimal balanceAfter,
                                        String description, String counterpart, String ref) {
        Transaction tx = Transaction.builder()
                .referenceId(ref)
                .account(account)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .counterpartAccountNumber(counterpart)
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();
        return txRepo.save(tx);
    }

    private String generateAccountNumber(Account.AccountType type) {
        String prefix = switch (type) {
            case CHECKING   -> "CHK";
            case SAVINGS    -> "SAV";
            case INVESTMENT -> "INV";
        };
        return String.format("ACC-%03d-%s", accountCounter.incrementAndGet(), prefix);
    }
}
