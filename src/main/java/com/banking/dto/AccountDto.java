package com.banking.dto;

import com.banking.model.Account;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountDto {

    // ── Request: create account ───────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {

        @NotBlank(message = "Owner name is required")
        @Size(min = 2, max = 100)
        private String ownerName;

        @NotNull(message = "Account type is required")
        private Account.AccountType accountType;

        @NotNull(message = "Initial deposit is required")
        @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
        @Digits(integer = 13, fraction = 2)
        private BigDecimal initialDeposit;

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
        private String currency;
    }

    // ── Response ──────────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String accountNumber;
        private String ownerName;
        private Account.AccountType accountType;
        private BigDecimal balance;
        private String currency;
        private Account.AccountStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Account a) {
            return Response.builder()
                    .id(a.getId())
                    .accountNumber(a.getAccountNumber())
                    .ownerName(a.getOwnerName())
                    .accountType(a.getAccountType())
                    .balance(a.getBalance())
                    .currency(a.getCurrency())
                    .status(a.getStatus())
                    .createdAt(a.getCreatedAt())
                    .updatedAt(a.getUpdatedAt())
                    .build();
        }
    }

    // ── Request: deposit / withdraw ───────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AmountRequest {

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 13, fraction = 2)
        private BigDecimal amount;

        private String description;
    }

    // ── Request: transfer ─────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TransferRequest {

        @NotBlank(message = "Target account number is required")
        private String toAccountNumber;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 13, fraction = 2)
        private BigDecimal amount;

        private String description;
    }
}
