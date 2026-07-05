package com.banking.repository;

import com.banking.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    List<Transaction> findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long accountId, LocalDateTime from, LocalDateTime to);

    Optional<Transaction> findByReferenceId(String referenceId);

    long countByAccountId(Long accountId);
}
