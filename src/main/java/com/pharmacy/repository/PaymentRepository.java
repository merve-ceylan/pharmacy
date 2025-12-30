package com.pharmacy.repository;

import com.pharmacy.entity.Payment;
import com.pharmacy.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find by order
    Optional<Payment> findByOrderId(Long orderId);

    // Find by iyzico transaction ID
    Optional<Payment> findByTransactionId(String transactionId);

    // Find by iyzico payment ID
    Optional<Payment> findByPaymentId(String paymentId);

    // Find by conversation ID
    Optional<Payment> findByConversationId(String conversationId);

    // Find by status
    List<Payment> findByStatus(PaymentStatus status);

    // Check if order has payment
    boolean existsByOrderId(Long orderId);
}
