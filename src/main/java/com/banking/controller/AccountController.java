package com.banking.controller;

import com.banking.dto.AccountDto;
import com.banking.dto.TransactionDto;
import com.banking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // ── Account endpoints ─────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<AccountDto.Response> createAccount(
            @Valid @RequestBody AccountDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request));
    }

    @GetMapping
    public ResponseEntity<List<AccountDto.Response>> getAllAccounts(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(accountService.searchAccounts(search));
        }
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto.Response> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountDto.Response> getAccountByNumber(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable Long id) {
        BigDecimal balance = accountService.getBalance(id);
        return ResponseEntity.ok(Map.of(
                "accountId", id,
                "balance", balance
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AccountDto.Response> closeAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.closeAccount(id));
    }

    // ── Transaction endpoints ─────────────────────────────────────────────────

    @PostMapping("/{id}/deposit")
    public ResponseEntity<TransactionDto.Response> deposit(
            @PathVariable Long id,
            @Valid @RequestBody AccountDto.AmountRequest request) {
        return ResponseEntity.ok(accountService.deposit(id, request));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<TransactionDto.Response> withdraw(
            @PathVariable Long id,
            @Valid @RequestBody AccountDto.AmountRequest request) {
        return ResponseEntity.ok(accountService.withdraw(id, request));
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<TransactionDto.TransferResponse> transfer(
            @PathVariable Long id,
            @Valid @RequestBody AccountDto.TransferRequest request) {
        return ResponseEntity.ok(accountService.transfer(id, request));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<Page<TransactionDto.Response>> getTransactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(accountService.getTransactions(id, page, size));
    }
}
