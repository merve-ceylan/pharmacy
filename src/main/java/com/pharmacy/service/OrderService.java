package com.pharmacy.service;

import com.pharmacy.entity.*;
import com.pharmacy.enums.DeliveryType;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.repository.OrderRepository;
import com.pharmacy.repository.OrderItemRepository;
import com.pharmacy.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    private static final AtomicLong orderCounter = new AtomicLong(0);

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductRepository productRepository,
                        CartService cartService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
    }

    public Order createOrderFromCart(Cart cart, DeliveryType deliveryType,
                                     String shippingAddress, String shippingCity,
                                     String shippingDistrict, String shippingPostalCode,
                                     String shippingPhone, String notes) {

        // Validate cart
        cartService.validateCart(cart.getId());

        Order order = new Order();
        order.setPharmacy(cart.getPharmacy());
        order.setCustomer(cart.getCustomer());
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryType(deliveryType);
        order.setShippingAddress(shippingAddress);
        order.setShippingCity(shippingCity);
        order.setShippingDistrict(shippingDistrict);
        order.setShippingPostalCode(shippingPostalCode);
        order.setShippingPhone(shippingPhone);
        order.setNotes(notes);

        // Calculate subtotal first
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            BigDecimal itemTotal = cartItem.getProduct().getEffectivePrice()
                    .multiply(new BigDecimal(cartItem.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }

        // Set amounts before saving
        order.setSubtotal(subtotal);
        order.setShippingCost(calculateShippingCost(deliveryType));
        order.setTotalAmount(subtotal.add(order.getShippingCost()));

        order = orderRepository.save(order);

        // Create order items and update stock
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setProductSku(product.getSku());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(product.getEffectivePrice());
            orderItem.calculateTotal();

            orderItemRepository.save(orderItem);
            order.addItem(orderItem);

            subtotal = subtotal.add(orderItem.getTotalPrice());

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        order = orderRepository.save(order);

        cartService.clearCart(cart.getId());

        return order;
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public Order getByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
    }

    public Page<Order> findByPharmacy(Long pharmacyId, Pageable pageable) {
        return orderRepository.findByPharmacyId(pharmacyId, pageable);
    }

    public Page<Order> findByCustomer(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable);
    }

    public List<Order> findByPharmacyAndStatus(Long pharmacyId, OrderStatus status) {
        return orderRepository.findByPharmacyIdAndStatus(pharmacyId, status);
    }

    public List<Order> findPendingOrders(Long pharmacyId) {
        return orderRepository.findByPharmacyIdAndStatusOrderByCreatedAtAsc(pharmacyId, OrderStatus.PENDING);
    }

    public List<Order> findRecentOrders(Long pharmacyId) {
        return orderRepository.findTop10ByPharmacyIdOrderByCreatedAtDesc(pharmacyId);
    }

    public Order updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = getById(orderId);

        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);

        switch (newStatus) {
            case CONFIRMED -> order.setConfirmedAt(LocalDateTime.now());
            case PREPARING -> order.setPreparingAt(LocalDateTime.now());
            case SHIPPED -> order.setShippedAt(LocalDateTime.now());
            case DELIVERED -> order.setDeliveredAt(LocalDateTime.now());
            default -> {}
        }

        return orderRepository.save(order);
    }

    public Order setTrackingNumber(Long orderId, String trackingNumber, String cargoCompany) {
        Order order = getById(orderId);
        order.setTrackingNumber(trackingNumber);
        order.setCargoCompany(cargoCompany);
        return orderRepository.save(order);
    }

    public Order cancelOrder(Long orderId, String reason, Long cancelledBy) {
        Order order = getById(orderId);

        if (!order.isCancellable()) {
            throw BusinessException.orderNotCancellable();
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(cancelledBy);

        // Restore stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        return orderRepository.save(order);
    }

    public long countByStatus(Long pharmacyId, OrderStatus status) {
        return orderRepository.countByPharmacyIdAndStatus(pharmacyId, status);
    }

    public long countTodayOrders(Long pharmacyId) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return orderRepository.countTodayOrders(pharmacyId, startOfDay);
    }

    private String generateOrderNumber() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        long count = orderCounter.incrementAndGet();
        String number = String.format("%05d", count);

        String orderNumber = "ORD-" + year + "-" + number;

        while (orderRepository.existsByOrderNumber(orderNumber)) {
            count = orderCounter.incrementAndGet();
            number = String.format("%05d", count);
            orderNumber = "ORD-" + year + "-" + number;
        }

        return orderNumber;
    }

    private BigDecimal calculateShippingCost(DeliveryType deliveryType) {
        if (deliveryType == DeliveryType.COURIER) {
            return new BigDecimal("20.00");
        } else {
            return new BigDecimal("35.00");
        }
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED;
            case PREPARING -> next == OrderStatus.SHIPPED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED, PAYMENT_FAILED -> false;
        };

        if (!valid) {
            throw BusinessException.invalidStatusTransition(current.name(), next.name());
        }
    }
}
