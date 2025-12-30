package com.pharmacy.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cart_id", "product_id"})
})
public class CartItem extends BaseEntity {

    // Parent cart
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // Product in cart
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    // Getters and Setters
    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    // Helper methods
    public BigDecimal getUnitPrice() {
        return product.getEffectivePrice();
    }

    public BigDecimal getTotalPrice() {
        return getUnitPrice().multiply(new BigDecimal(quantity));
    }

    public void incrementQuantity() {
        this.quantity++;
    }

    public void decrementQuantity() {
        if (this.quantity > 1) {
            this.quantity--;
        }
    }

    public boolean isAvailable() {
        return product.isActive() && product.getStockQuantity() >= quantity;
    }
}