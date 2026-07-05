package com.banking.service;

import com.banking.dto.AccountDto;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.BankingException;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accountRepo;
    @Mock TransactionRepository txRepo;

    @InjectMocks AccountService accountService;

    private Account activeAccount;

    @BeforeEach
    void setUp() {
        activeAccount = Account.builder()
                .id(1L)
                .accountNumber("ACC-001-CHK")
                .ownerName("Test User")
                .accountType(Account.AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .build();
    }

    // ── Create account ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createAccount")
    class CreateAccount {

        @Test
        @DisplayName("creates account with initial deposit")
        void createsAccountSuccessfully() {
            AccountDto.CreateRequest req = AccountDto.CreateRequest.builder()
                    .ownerName("Jane Doe")
                    .accountType(Account.AccountType.SAVINGS)
                    .initialDeposit(new BigDecimal("500.00"))
                    .currency("USD")
                    .build();

            when(accountRepo.save(any())).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.setId(99L);
                return a;
            });
            when(txRepo.save(any())).thenReturn(mock(Transaction.class));

            AccountDto.Response response = accountService.createAccount(req);

            assertThat(response.getOwnerName()).isEqualTo("Jane Doe");
            assertThat(response.getBalance()).isEqualByComparingTo("500.00");
            assertThat(response.getAccountType()).isEqualTo(Account.AccountType.SAVINGS);
            verify(accountRepo, times(1)).save(any());
            verify(txRepo, times(1)).save(any()); // initial deposit transaction
        }

        @Test
        @DisplayName("defaults currency to USD when not provided")
        void defaultsCurrencyToUSD() {
            AccountDto.CreateRequest req = AccountDto.CreateRequest.builder()
                    .ownerName("No Currency")
                    .accountType(Account.AccountType.CHECKING)
                    .initialDeposit(BigDecimal.ZERO)
                    .build();

            when(accountRepo.save(any())).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.setId(1L);
                return a;
            });

            AccountDto.Response resp = accountService.createAccount(req);
            assertThat(resp.getCurrency()).isEqualTo("USD");
        }
    }

    // ── Deposit ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        @DisplayName("increases balance correctly")
        void depositsSuccessfully() {
            when(accountRepo.findById(1L)).thenReturn(Optional.of(activeAccount));
            when(accountRepo.save(any())).thenReturn(activeAccount);
            when(txRepo.save(any())).thenReturn(mock(Transaction.class));

            AccountDto.AmountRequest req = new AccountDto.AmountRequest(new BigDecimal("250.00"), "Test");
            accountService.deposit(1L, req);

            assertThat(activeAccount.getBalance()).isEqualByComparingTo("1250.00");
            verify(txRepo).save(argThat(tx -> tx instanceof Transaction));
        }

        @Test
        @DisplayName("throws when account not found")
        void throwsWhenAccountNotFound() {
            when(accountRepo.findById(99L)).thenReturn(Optional.empty());
            AccountDto.AmountRequest req = new AccountDto.AmountRequest(new BigDecimal("100.00"), null);
            assertThatThrownBy(() -> accountService.deposit(99L, req))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    // ── Withdraw ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("decreases balance correctly")
        void withdrawsSuccessfully() {
            when(accountRepo.findById(1L)).thenReturn(Optional.of(activeAccount));
            when(accountRepo.save(any())).thenReturn(activeAccount);
            when(txRepo.save(any())).thenReturn(mock(Transaction.class));

            accountService.withdraw(1L, new AccountDto.AmountRequest(new BigDecimal("400.00"), null));
            assertThat(activeAccount.getBalance()).isEqualByComparingTo("600.00");
        }

        @Test
        @DisplayName("throws on insufficient funds")
        void throwsOnInsufficientFunds() {
            when(accountRepo.findById(1L)).thenReturn(Optional.of(activeAccount));
            AccountDto.AmountRequest req = new AccountDto.AmountRequest(new BigDecimal("9999.00"), null);
            assertThatThrownBy(() -> accountService.withdraw(1L, req))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("Insufficient funds");
        }
    }

    // ── Transfer ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("transfer")
    class Transfer {

        @Test
        @DisplayName("moves money between accounts")
        void transfersSuccessfully() {
            Account toAccount = Account.builder()
                    .id(2L).accountNumber("ACC-002-SAV")
                    .ownerName("Receiver").accountType(Account.AccountType.SAVINGS)
                    .balance(new BigDecimal("500.00")).currency("USD")
                    .status(Account.AccountStatus.ACTIVE).build();

            when(accountRepo.findById(1L)).thenReturn(Optional.of(activeAccount));
            when(accountRepo.findByAccountNumber("ACC-002-SAV")).thenReturn(Optional.of(toAccount));
            when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(txRepo.save(any())).thenReturn(mock(Transaction.class));

            AccountDto.TransferRequest req = new AccountDto.TransferRequest("ACC-002-SAV", new BigDecimal("300.00"), "Test transfer");
            var result = accountService.transfer(1L, req);

            assertThat(activeAccount.getBalance()).isEqualByComparingTo("700.00");
            assertThat(toAccount.getBalance()).isEqualByComparingTo("800.00");
            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            verify(txRepo, times(2)).save(any()); // one TRANSFER_OUT, one TRANSFER_IN
        }

        @Test
        @DisplayName("throws on self-transfer")
        void throwsOnSelfTransfer() {
            when(accountRepo.findById(1L)).thenReturn(Optional.of(activeAccount));
            when(accountRepo.findByAccountNumber("ACC-001-CHK")).thenReturn(Optional.of(activeAccount));

            AccountDto.TransferRequest req = new AccountDto.TransferRequest("ACC-001-CHK", new BigDecimal("100.00"), null);
            assertThatThrownBy(() -> accountService.transfer(1L, req))
                    .isInstanceOf(BankingException.class)
                    .hasMessageContaining("same account");
        }
    }
}
