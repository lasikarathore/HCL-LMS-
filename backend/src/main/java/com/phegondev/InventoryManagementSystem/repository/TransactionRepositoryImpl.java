package com.phegondev.InventoryManagementSystem.repository;

import com.phegondev.InventoryManagementSystem.entity.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    private static final String BASE_WHERE = """
            FROM transactions t
            LEFT JOIN products p ON p.id = t.product_id
            WHERE (
              (COALESCE(CAST(:txType AS text), '') = '' AND CAST(t.transaction_type AS text) != 'PURCHASE')
              OR CAST(t.transaction_type AS text) = CAST(:txType AS text)
            )
            AND (CAST(:userId AS bigint) IS NULL OR t.user_id = :userId)
            AND (
              COALESCE(CAST(:status AS text), '') = ''
              OR (CAST(:status AS text) = 'COMPLETED' AND t.status IS NULL)
              OR CAST(t.status AS text) = CAST(:status AS text)
            )
            AND (
              COALESCE(CAST(:search AS text), '') = ''
              OR LOWER(COALESCE(CAST(t.description AS text), '')) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
              OR LOWER(COALESCE(CAST(p.name AS text), '')) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
              OR LOWER(COALESCE(CAST(p.sku AS text), '')) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
            )
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Page<Transaction> pageTransactionsFiltered(String transactionType, String searchText, String status, Long userId, Pageable pageable) {
        String select = """
                /* ims-entitymanager-native-tx-search */
                SELECT t.id, t.total_products, t.total_price, t.transaction_type, t.status, t.description,
                       t.updated_at, t.created_at, t.user_id, t.product_id, t.supplier_id
                """ + BASE_WHERE + " ORDER BY t.id DESC";

        Query q = entityManager.createNativeQuery(select, Transaction.class);
        q.setParameter("txType", transactionType);
        q.setParameter("search", searchText);
        q.setParameter("status", status);
        q.setParameter("userId", userId);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        List<Transaction> content = q.getResultList();

        String countSql = "SELECT count(t.id) " + BASE_WHERE;
        Query cq = entityManager.createNativeQuery(countSql);
        cq.setParameter("txType", transactionType);
        cq.setParameter("search", searchText);
        cq.setParameter("status", status);
        cq.setParameter("userId", userId);
        Number total = (Number) cq.getSingleResult();

        return new PageImpl<>(content, pageable, total.longValue());
    }
}
