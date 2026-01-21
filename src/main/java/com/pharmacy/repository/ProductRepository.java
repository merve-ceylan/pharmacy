package com.pharmacy.repository;

import com.pharmacy.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find by pharmacy (multi-tenant)
    List<Product> findByPharmacyId(Long pharmacyId);

    // Find by pharmacy with pagination
    Page<Product> findByPharmacyIdAndActiveTrue(Long pharmacyId, Pageable pageable);

    // Find by pharmacy and category
    List<Product> findByPharmacyIdAndCategoryIdAndActiveTrue(Long pharmacyId, Long categoryId);

    // Find by slug
    Optional<Product> findBySlug(String slug);

    // Find by pharmacy and slug
    Optional<Product> findByPharmacyIdAndSlug(Long pharmacyId, String slug);

    // Find by SKU within pharmacy
    Optional<Product> findByPharmacyIdAndSku(Long pharmacyId, String sku);

    // Find by barcode within pharmacy
    Optional<Product> findByPharmacyIdAndBarcode(Long pharmacyId, String barcode);

    // Find low stock products
    @Query("SELECT p FROM Product p WHERE p.pharmacy.id = :pharmacyId AND p.stockQuantity <= p.lowStockThreshold AND p.active = true")
    List<Product> findLowStockProducts(@Param("pharmacyId") Long pharmacyId);

    // Find out of stock products
    List<Product> findByPharmacyIdAndStockQuantityAndActiveTrue(Long pharmacyId, Integer stockQuantity);

    // Find featured products
    List<Product> findByPharmacyIdAndFeaturedTrueAndActiveTrue(Long pharmacyId);

    // Search products by name
    @Query("SELECT p FROM Product p WHERE p.pharmacy.id = :pharmacyId AND p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByName(@Param("pharmacyId") Long pharmacyId, @Param("keyword") String keyword, Pageable pageable);

    // Check if slug exists
    boolean existsBySlug(String slug);

    // Check if SKU exists in pharmacy
    boolean existsByPharmacyIdAndSku(Long pharmacyId, String sku);

    // Count products by pharmacy
    long countByPharmacyId(Long pharmacyId);

    // Count active products by pharmacy
    long countByPharmacyIdAndActiveTrue(Long pharmacyId);

}