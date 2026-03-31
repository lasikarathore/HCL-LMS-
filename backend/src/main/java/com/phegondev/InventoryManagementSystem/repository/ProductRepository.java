package com.phegondev.InventoryManagementSystem.repository;

import com.phegondev.InventoryManagementSystem.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    long countByCategory_Id(Long categoryId);

    long countByStockQuantityLessThan(int threshold);

    List<Product> findByStockQuantityLessThanOrderByStockQuantityAsc(int threshold, Pageable pageable);

    List<Product> findByStockQuantityLessThanOrderByStockQuantityAsc(int threshold);

    @Query("SELECT COALESCE(SUM(p.price * p.stockQuantity), 0) FROM Product p")
    BigDecimal sumInventoryValue();


    @Query("SELECT c.name, COALESCE(SUM(p.price * p.stockQuantity), 0) FROM Product p JOIN p.category c GROUP BY c.id, c.name ORDER BY COALESCE(SUM(p.price * p.stockQuantity), 0) DESC")
    List<Object[]> sumInventoryValueByCategory();
}
