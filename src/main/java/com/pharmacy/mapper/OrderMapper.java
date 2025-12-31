package com.pharmacy.mapper;

import com.pharmacy.dto.response.OrderItemResponse;
import com.pharmacy.dto.response.OrderResponse;
import com.pharmacy.entity.Order;
import com.pharmacy.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus());
        response.setDeliveryType(order.getDeliveryType());

        // Customer info
        if (order.getCustomer() != null) {
            response.setCustomerId(order.getCustomer().getId());
            response.setCustomerName(order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName());
            response.setCustomerEmail(order.getCustomer().getEmail());
            response.setCustomerPhone(order.getCustomer().getPhone());
        }

        // Pharmacy info
        if (order.getPharmacy() != null) {
            response.setPharmacyId(order.getPharmacy().getId());
            response.setPharmacyName(order.getPharmacy().getName());
        }

        // Shipping info
        response.setShippingAddress(order.getShippingAddress());
        response.setShippingCity(order.getShippingCity());
        response.setShippingDistrict(order.getShippingDistrict());
        response.setShippingPostalCode(order.getShippingPostalCode());
        response.setShippingPhone(order.getShippingPhone());

        // Tracking info
        response.setTrackingNumber(order.getTrackingNumber());
        response.setCargoCompany(order.getCargoCompany());

        // Price info
        response.setSubtotal(order.getSubtotal());
        response.setShippingCost(order.getShippingCost());
        response.setTotalAmount(order.getTotalAmount());

        // Notes
        response.setNotes(order.getNotes());
        response.setCancellationReason(order.getCancellationReason());

        // Timestamps
        response.setCreatedAt(order.getCreatedAt());
        response.setConfirmedAt(order.getConfirmedAt());
        response.setPreparingAt(order.getPreparingAt());
        response.setShippedAt(order.getShippedAt());
        response.setDeliveredAt(order.getDeliveredAt());
        response.setCancelledAt(order.getCancelledAt());

        // Flags
        response.setCancellable(order.isCancellable());

        // Item count
        if (order.getItems() != null) {
            response.setItemCount(order.getItems().size());
        }

        return response;
    }

    public OrderResponse toResponseWithItems(Order order) {
        OrderResponse response = toResponse(order);

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            List<OrderItemResponse> items = order.getItems().stream()
                    .map(this::toOrderItemResponse)
                    .toList();
            response.setItems(items);
        }

        return response;
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductName(item.getProductName());
        response.setProductSku(item.getProductSku());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setTotalPrice(item.getTotalPrice());

        if (item.getProduct() != null) {
            response.setProductId(item.getProduct().getId());
            response.setProductImageUrl(item.getProduct().getImageUrl());
        }

        return response;
    }
}