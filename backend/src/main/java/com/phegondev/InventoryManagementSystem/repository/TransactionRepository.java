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

    @Query(value = """
            SELECT COALESCE(SUM(t.total_price), 0)
            FROM transactions t
            WHERE t.transaction_type = 'PURCHASE' AND t.supplier_id = :supplierId
            """, nativeQuery = true)
    BigDecimal sumPurchasesBySupplier(@Param("supplierId") Long supplierId);

    @Query(value = """
            SELECT COALESCE(SUM(t.total_products), 0)
            FROM transactions t
            WHERE t.transaction_type = 'PURCHASE' AND t.supplier_id = :supplierId
            """, nativeQuery = true)
    Long sumPurchaseUnitsBySupplier(@Param("supplierId") Long supplierId);

    @Query(value = """
            SELECT DATE_TRUNC('month', t.created_at) AS month_start,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'SALE' THEN t.total_price ELSE 0 END), 0) AS sales,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'PURCHASE' THEN t.total_price ELSE 0 END), 0) AS purchases
            FROM transactions t
            WHERE t.created_at >= :since AND t.created_at < :until
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> monthlySalesVsPurchasesBetween(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);

    @Query(value = """
            SELECT c.name,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'SALE' THEN t.total_price ELSE 0 END), 0) AS revenue
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            JOIN categories c ON p.category_id = c.id
            WHERE t.transaction_type = 'SALE' AND t.created_at >= :since AND t.created_at < :until
            GROUP BY c.id, c.name
            ORDER BY 2 DESC
            """, nativeQuery = true)
    List<Object[]> revenueByCategoryBetween(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);

    @Query(value = """
            SELECT p.id,
                   p.name,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'SALE' THEN t.total_products ELSE 0 END), 0) AS units_sold,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'PURCHASE' THEN t.total_products ELSE 0 END), 0) AS units_purchased
            FROM products p
            LEFT JOIN transactions t ON t.product_id = p.id
                 AND t.created_at >= :since AND t.created_at < :until
            GROUP BY p.id, p.name
            ORDER BY p.id DESC
            """, nativeQuery = true)
    List<Object[]> unitsSoldPurchasedByProductBetween(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(t.totalPrice), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.transactionType = :type " +
            "AND CAST(t.createdAt AS date) = :date")
    BigDecimal sumTotalPriceByUserIdAndTypeAndDate(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("date") java.time.LocalDate date);

    @Query(value = """
            SELECT DATE_TRUNC('month', t.created_at) AS m,
                   COALESCE(SUM(t.total_price), 0)
            FROM transactions t
            WHERE t.user_id = :userId AND t.transaction_type = 'SALE' AND t.created_at >= :since
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> sumSalesByUserIdLastMonths(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query(value = """
            SELECT CAST(t.created_at AS date) AS d,
                   COALESCE(SUM(t.total_price), 0)
            FROM transactions t
            WHERE t.user_id = :userId AND t.transaction_type = 'SALE' AND t.created_at >= :since
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> sumSalesByUserIdLastDays(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
