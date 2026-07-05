package com.banking.dto;

import com.banking.model.Transaction;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String referenceId;
        private String accountNumber;
        private Transaction.TransactionType type;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String description;
        private String counterpartAccountNumber;
        private Transaction.TransactionStatus status;
        private LocalDateTime createdAt;

        public static Response from(Transaction t) {
            return Response.builder()
                    .id(t.getId())
                    .referenceId(t.getReferenceId())
                    .accountNumber(t.getAccount().getAccountNumber())
                    .type(t.getType())
                    .amount(t.getAmount())
                    .balanceAfter(t.getBalanceAfter())
                    .description(t.getDescription())
                    .counterpartAccountNumber(t.getCounterpartAccountNumber())
                    .status(t.getStatus())
                    .createdAt(t.getCreatedAt())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TransferResponse {
        private String referenceId;
        private String fromAccount;
        private String toAccount;
        private BigDecimal amount;
        private BigDecimal fromBalanceAfter;
        private BigDecimal toBalanceAfter;
        private String status;
        private LocalDateTime timestamp;
    }
}
