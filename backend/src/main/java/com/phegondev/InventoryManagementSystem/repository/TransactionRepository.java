package com.phegondev.InventoryManagementSystem.repository;

import com.phegondev.InventoryManagementSystem.entity.Transaction;
import com.phegondev.InventoryManagementSystem.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, TransactionRepositoryCustom {


    @Query("SELECT t FROM Transaction t " +
            "WHERE YEAR(t.createdAt) = :year AND MONTH(t.createdAt) = :month")
    List<Transaction> findAllByMonthAndYear(@Param("month") int month, @Param("year") int year);


    long countByProduct_Id(Long productId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionType = :type " +
            "AND YEAR(t.createdAt) = :year AND MONTH(t.createdAt) = :month")
    long countByTypeAndMonth(
            @Param("type") TransactionType type,
            @Param("year") int year,
            @Param("month") int month);

    @Query("SELECT COALESCE(SUM(t.totalPrice), 0) FROM Transaction t WHERE t.transactionType = :type " +
            "AND YEAR(t.createdAt) = :year AND MONTH(t.createdAt) = :month")
    BigDecimal sumTotalPriceByTypeAndMonth(
            @Param("type") TransactionType type,
            @Param("year") int year,
            @Param("month") int month);

    @Query("SELECT t.transactionType, COUNT(t), COALESCE(SUM(t.totalPrice), 0) FROM Transaction t GROUP BY t.transactionType")
    List<Object[]> aggregateCountAndAmountByType();

    @Query("SELECT YEAR(t.createdAt), MONTH(t.createdAt), t.transactionType, COALESCE(SUM(t.totalPrice), 0) " +
            "FROM Transaction t GROUP BY YEAR(t.createdAt), MONTH(t.createdAt), t.transactionType " +
            "ORDER BY YEAR(t.createdAt) ASC, MONTH(t.createdAt) ASC")
    List<Object[]> aggregateAmountByYearMonthAndType();

    @Query(value = """
            SELECT CAST(t.created_at AS date),
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'SALE' THEN t.total_price ELSE 0 END), 0),
                   COALESCE(SUM(t.total_price), 0),
                   COUNT(*)
            FROM transactions t
            WHERE t.created_at >= :start AND t.created_at < :end
            GROUP BY CAST(t.created_at AS date)
            ORDER BY CAST(t.created_at AS date)
            """, nativeQuery = true)
    List<Object[]> dailyStatsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT t.transactionType, COALESCE(SUM(t.totalPrice), 0) FROM Transaction t WHERE t.createdAt >= :since GROUP BY t.transactionType")
    List<Object[]> sumByTypeSince(@Param("since") LocalDateTime since);

    @Query(value = """
            SELECT p.name, COALESCE(SUM(t.total_price), 0)
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            WHERE t.created_at >= :since
            GROUP BY p.id, p.name
            ORDER BY 2 DESC
            LIMIT 6
            """, nativeQuery = true)
    List<Object[]> topProductVolumeSince(@Param("since") LocalDateTime since);
}
