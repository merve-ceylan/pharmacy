package com.pharmacy.mapper;

import com.pharmacy.dto.response.CartItemResponse;
import com.pharmacy.dto.response.CartResponse;
import com.pharmacy.entity.Cart;
import com.pharmacy.entity.CartItem;
import com.pharmacy.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CartMapper {

    private static final BigDecimal DEFAULT_SHIPPING = new BigDecimal("20.00");

    public CartResponse toResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setUpdatedAt(cart.getUpdatedAt());

        // Pharmacy info
        if (cart.getPharmacy() != null) {
            response.setPharmacyId(cart.getPharmacy().getId());
            response.setPharmacyName(cart.getPharmacy().getName());
        }

        // Items
        if (cart.getItems() != null && !cart.getItems().isEmpty()) {
            List<CartItemResponse> items = cart.getItems().stream()
                    .map(this::toCartItemResponse)
                    .toList();
            response.setItems(items);
            response.setItemCount(items.size());

            // Calculate totals
            int totalQuantity = cart.getItems().stream()
                    .mapToInt(CartItem::getQuantity)
                    .sum();
            response.setTotalQuantity(totalQuantity);

            BigDecimal subtotal = cart.getItems().stream()
                    .map(CartItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            response.setSubtotal(subtotal);

            // Estimated shipping
            response.setEstimatedShipping(DEFAULT_SHIPPING);
            response.setEstimatedTotal(subtotal.add(DEFAULT_SHIPPING));

            // Check unavailable items
            boolean hasUnavailable = cart.getItems().stream()
                    .anyMatch(item -> !item.isAvailable());
            response.setHasUnavailableItems(hasUnavailable);
        } else {
            response.setItems(List.of());
            response.setItemCount(0);
            response.setTotalQuantity(0);
            response.setSubtotal(BigDecimal.ZERO);
            response.setEstimatedShipping(BigDecimal.ZERO);
            response.setEstimatedTotal(BigDecimal.ZERO);
            response.setHasUnavailableItems(false);
        }

        return response;
    }

    public CartItemResponse toCartItemResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setQuantity(item.getQuantity());
        response.setTotalPrice(item.getTotalPrice());
        response.setAvailable(item.isAvailable());

        Product product = item.getProduct();
        if (product != null) {
            response.setProductId(product.getId());
            response.setProductName(product.getName());
            response.setProductSlug(product.getSlug());
            response.setProductSku(product.getSku());
            response.setProductImageUrl(product.getImageUrl());
            response.setUnitPrice(product.getPrice());
            response.setDiscountedPrice(product.getDiscountedPrice());
            response.setEffectivePrice(product.getEffectivePrice());
            response.setAvailableStock(product.getStockQuantity());
            response.setInStock(product.isInStock());
        }

        return response;
    }
}