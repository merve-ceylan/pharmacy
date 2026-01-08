package com.pharmacy.service;

import com.pharmacy.entity.Cart;
import com.pharmacy.entity.CartItem;
import com.pharmacy.entity.Product;
import com.pharmacy.entity.User;
import com.pharmacy.entity.Pharmacy;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.repository.CartRepository;
import com.pharmacy.repository.CartItemRepository;
import com.pharmacy.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    public Cart getOrCreateCart(User customer, Pharmacy pharmacy) {
        return cartRepository.findByCustomerIdAndPharmacyId(customer.getId(), pharmacy.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomer(customer);
                    cart.setPharmacy(pharmacy);
                    return cartRepository.save(cart);
                });
    }

    public Optional<Cart> getCart(Long customerId, Long pharmacyId) {
        return cartRepository.findByCustomerIdAndPharmacyId(customerId, pharmacyId);
    }

    public Cart getById(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));
    }

    public CartItem addToCart(Cart cart, Product product, Integer quantity) {
        // Validate product is active
        if (!product.isActive()) {
            throw new BusinessException("Product is not available: " + product.getName(), "PRODUCT_UNAVAILABLE");
        }

        // Validate stock
        if (product.getStockQuantity() < quantity) {
            throw BusinessException.insufficientStock(product.getName(), product.getStockQuantity());
        }

        // Check if product already in cart
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;

            if (product.getStockQuantity() < newQuantity) {
                throw BusinessException.insufficientStock(product.getName(), product.getStockQuantity());
            }

            item.setQuantity(newQuantity);
            return cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
            cart.addItem(item);
            return cartItemRepository.save(item);
        }
    }

    public CartItem updateQuantity(Long cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));

        if (quantity <= 0) {
            // Miktar 0 veya negatifse sil
            Cart cart = item.getCart();
            cart.removeItem(item);
            cartRepository.save(cart);
            return null;
        }

        if (item.getProduct().getStockQuantity() < quantity) {
            throw BusinessException.insufficientStock(item.getProduct().getName(), item.getProduct().getStockQuantity());
        }

        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    public void removeFromCart(Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));

        // delete item from cart
        Cart cart = item.getCart();
        cart.removeItem(item);
        cartRepository.save(cart);
    }

    public void removeProductFromCart(Long cartId, Long productId) {
        Cart cart = getById(cartId);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", productId.toString()));
        cart.removeItem(item);
        cartRepository.save(cart);
    }

    public void clearCart(Long cartId) {
        Cart cart = getById(cartId);
        cart.clear();
        cartRepository.save(cart);
    }

    public BigDecimal getCartSubtotal(Long cartId) {
        Cart cart = getById(cartId);
        return cart.getItems().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getCartItemCount(Long cartId) {
        return (int) cartItemRepository.countByCartId(cartId);
    }

    public void validateCart(Long cartId) {
        Cart cart = getById(cartId);

        if (cart.isEmpty()) {
            throw BusinessException.emptyCart();
        }

        List<CartItem> unavailableItems = getUnavailableItems(cartId);
        if (!unavailableItems.isEmpty()) {
            throw BusinessException.cartItemsUnavailable();
        }
    }

    public List<CartItem> getUnavailableItems(Long cartId) {
        Cart cart = getById(cartId);
        return cart.getItems().stream()
                .filter(item -> !item.isAvailable())
                .toList();
    }
}