package com.pharmacy.repository;

import com.pharmacy.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Find all items in order
    List<OrderItem> findByOrderId(Long orderId);

    // Find by product (to check product sales)
    List<OrderItem> findByProductId(Long productId);

    // Count how many times product was sold
    long countByProductId(Long productId);

    // Get total quantity sold for a product
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.product.id = :productId")
    Long getTotalQuantitySold(@Param("productId") Long productId);

    // Get best selling products for pharmacy
    @Query("SELECT oi.product.id, SUM(oi.quantity) as total FROM OrderItem oi " +
            "WHERE oi.order.pharmacy.id = :pharmacyId " +
            "GROUP BY oi.product.id ORDER BY total DESC")
    List<Object[]> findBestSellingProducts(@Param("pharmacyId") Long pharmacyId);
}
