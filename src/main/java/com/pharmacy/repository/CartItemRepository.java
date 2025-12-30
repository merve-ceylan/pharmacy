package com.pharmacy.repository;

import com.pharmacy.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Find all items in cart
    List<CartItem> findByCartId(Long cartId);

    // Find specific product in cart
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    // Check if product exists in cart
    boolean existsByCartIdAndProductId(Long cartId, Long productId);

    // Delete all items in cart
    void deleteByCartId(Long cartId);

    // Delete specific product from cart
    void deleteByCartIdAndProductId(Long cartId, Long productId);

    // Count items in cart
    long countByCartId(Long cartId);
}
