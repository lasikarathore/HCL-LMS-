package com.phegondev.InventoryManagementSystem.repository;

import com.phegondev.InventoryManagementSystem.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionRepositoryCustom {

    /**
     * Programmatic native SQL — not derived from JPQL — so PostgreSQL never sees
     * {@code lower(bytea)} from Hibernate’s old search translation.
     */
    Page<Transaction> pageTransactionsFiltered(String transactionType, String searchText, String status, Long userId, Pageable pageable);
}
