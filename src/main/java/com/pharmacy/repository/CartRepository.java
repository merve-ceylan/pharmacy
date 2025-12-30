package com.pharmacy.repository;

import com.pharmacy.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Find cart by customer and pharmacy (unique combination)
    Optional<Cart> findByCustomerIdAndPharmacyId(Long customerId, Long pharmacyId);

    // Check if cart exists
    boolean existsByCustomerIdAndPharmacyId(Long customerId, Long pharmacyId);

    // Delete cart by customer and pharmacy
    void deleteByCustomerIdAndPharmacyId(Long customerId, Long pharmacyId);
}
