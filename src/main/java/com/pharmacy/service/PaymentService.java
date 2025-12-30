package com.pharmacy.service;

import com.pharmacy.entity.Order;
import com.pharmacy.entity.Payment;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.enums.PaymentStatus;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.DuplicateResourceException;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.repository.PaymentRepository;
import com.pharmacy.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    public Payment createPayment(Order order) {
        if (paymentRepository.existsByOrderId(order.getId())) {
            throw new DuplicateResourceException("Payment already exists for order: " + order.getOrderNumber());
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setConversationId(generateConversationId());

        return paymentRepository.save(payment);
    }

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    public Payment getById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    public Optional<Payment> findByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public Optional<Payment> findByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }

    public Payment getByConversationId(String conversationId) {
        return paymentRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "conversationId", conversationId));
    }

    public Payment processSuccessfulPayment(String conversationId, String transactionId,
                                            String paymentId, String cardLastFour, String cardBrand) {
        Payment payment = getByConversationId(conversationId);

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(transactionId);
        payment.setPaymentId(paymentId);
        payment.setCardLastFour(cardLastFour);
        payment.setCardBrand(cardBrand);
        payment.setPaidAt(LocalDateTime.now());

        Order order = payment.getOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        orderRepository.save(order);

        return paymentRepository.save(payment);
    }

    public Payment processFailedPayment(String conversationId, String errorCode, String errorMessage) {
        Payment payment = getByConversationId(conversationId);

        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorCode(errorCode);
        payment.setErrorMessage(errorMessage);

        Order order = payment.getOrder();
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);

        return paymentRepository.save(payment);
    }

    public Payment processFullRefund(Long paymentId) {
        Payment payment = getById(paymentId);

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw BusinessException.refundNotAllowed();
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAmount(payment.getAmount());
        payment.setRefundedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment processPartialRefund(Long paymentId, BigDecimal refundAmount) {
        Payment payment = getById(paymentId);

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw BusinessException.refundNotAllowed();
        }

        BigDecimal totalRefunded = payment.getRefundedAmount().add(refundAmount);
        if (totalRefunded.compareTo(payment.getAmount()) > 0) {
            throw new BusinessException("Refund amount exceeds payment amount", "REFUND_EXCEEDS_PAYMENT");
        }

        payment.setRefundedAmount(totalRefunded);
        payment.setRefundedAt(LocalDateTime.now());

        if (totalRefunded.compareTo(payment.getAmount()) == 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        }

        return paymentRepository.save(payment);
    }

    private String generateConversationId() {
        return "CONV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
