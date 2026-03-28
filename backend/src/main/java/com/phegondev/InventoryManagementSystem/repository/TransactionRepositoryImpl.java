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
            WHERE (:txType IS NULL OR CAST(:txType AS text) = '' OR CAST(t.transaction_type AS text) = CAST(:txType AS text))
            AND (
              :search IS NULL OR CAST(:search AS text) = ''
              OR LOWER(COALESCE(CAST(t.description AS text), '')) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
              OR LOWER(COALESCE(CAST(p.name AS text), '')) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
              OR LOWER(COALESCE(CAST(p.sku AS text), '')) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
            )
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Page<Transaction> pageTransactionsFiltered(String transactionType, String searchText, Pageable pageable) {
        String select = """
                /* ims-entitymanager-native-tx-search */
                SELECT t.id, t.total_products, t.total_price, t.transaction_type, t.status, t.description,
                       t.updated_at, t.created_at, t.user_id, t.product_id, t.supplier_id
                """ + BASE_WHERE + " ORDER BY t.id DESC";

        Query q = entityManager.createNativeQuery(select, Transaction.class);
        q.setParameter("txType", transactionType);
        q.setParameter("search", searchText);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        List<Transaction> content = q.getResultList();

        String countJpql = "SELECT count(t.id) " + BASE_WHERE;
        Query cq = entityManager.createNativeQuery(countJpql);
        cq.setParameter("txType", transactionType);
        cq.setParameter("search", searchText);
        Number total = (Number) cq.getSingleResult();

        return new PageImpl<>(content, pageable, total.longValue());
    }
}
