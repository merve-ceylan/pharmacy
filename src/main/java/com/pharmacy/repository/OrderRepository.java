package com.pharmacy.repository;

import com.pharmacy.entity.Order;
import com.pharmacy.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Find by order number
    Optional<Order> findByOrderNumber(String orderNumber);

    // Find by pharmacy (multi-tenant)
    Page<Order> findByPharmacyId(Long pharmacyId, Pageable pageable);

    // Find by pharmacy and status
    List<Order> findByPharmacyIdAndStatus(Long pharmacyId, OrderStatus status);

    // Find by customer
    List<Order> findByCustomerId(Long customerId);

    // Find by customer with pagination
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    // Find by customer and pharmacy
    List<Order> findByCustomerIdAndPharmacyId(Long customerId, Long pharmacyId);

    // Find orders by date range
    @Query("SELECT o FROM Order o WHERE o.pharmacy.id = :pharmacyId AND o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByPharmacyIdAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Find pending orders (for pharmacy dashboard)
    List<Order> findByPharmacyIdAndStatusOrderByCreatedAtAsc(Long pharmacyId, OrderStatus status);

    // Count orders by status
    long countByPharmacyIdAndStatus(Long pharmacyId, OrderStatus status);

    // Count today's orders
    @Query("SELECT COUNT(o) FROM Order o WHERE o.pharmacy.id = :pharmacyId AND o.createdAt >= :startOfDay")
    long countTodayOrders(@Param("pharmacyId") Long pharmacyId, @Param("startOfDay") LocalDateTime startOfDay);

    // Find recent orders
    List<Order> findTop10ByPharmacyIdOrderByCreatedAtDesc(Long pharmacyId);

    // Check if order number exists
    boolean existsByOrderNumber(String orderNumber);
}
