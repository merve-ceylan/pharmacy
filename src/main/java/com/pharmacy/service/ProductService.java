package com.pharmacy.service;

import com.pharmacy.entity.Product;
import com.pharmacy.entity.Pharmacy;
import com.pharmacy.entity.Category;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.DuplicateResourceException;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(Product product, Pharmacy pharmacy, Category category) {
        product.setPharmacy(pharmacy);
        product.setCategory(category);

        if (product.getSlug() == null || product.getSlug().isEmpty()) {
            product.setSlug(generateSlug(product.getName(), pharmacy.getId()));
        }

        if (product.getSku() != null && productRepository.existsByPharmacyIdAndSku(pharmacy.getId(), product.getSku())) {
            throw new DuplicateResourceException("Product", "SKU", product.getSku());
        }

        product.setActive(true);
        return productRepository.save(product);
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public Optional<Product> findBySlug(String slug) {
        return productRepository.findBySlug(slug);
    }

    public Optional<Product> findByPharmacyAndSlug(Long pharmacyId, String slug) {
        return productRepository.findByPharmacyIdAndSlug(pharmacyId, slug);
    }

    public Optional<Product> findBySku(Long pharmacyId, String sku) {
        return productRepository.findByPharmacyIdAndSku(pharmacyId, sku);
    }

    public Optional<Product> findByBarcode(Long pharmacyId, String barcode) {
        return productRepository.findByPharmacyIdAndBarcode(pharmacyId, barcode);
    }

    public Page<Product> findByPharmacy(Long pharmacyId, Pageable pageable) {
        return productRepository.findByPharmacyIdAndActiveTrue(pharmacyId, pageable);
    }

    public List<Product> findByCategory(Long pharmacyId, Long categoryId) {
        return productRepository.findByPharmacyIdAndCategoryIdAndActiveTrue(pharmacyId, categoryId);
    }

    public List<Product> findFeaturedProducts(Long pharmacyId) {
        return productRepository.findByPharmacyIdAndFeaturedTrueAndActiveTrue(pharmacyId);
    }

    public Page<Product> searchProducts(Long pharmacyId, String keyword, Pageable pageable) {
        return productRepository.searchByName(pharmacyId, keyword, pageable);
    }

    public List<Product> findLowStockProducts(Long pharmacyId) {
        return productRepository.findLowStockProducts(pharmacyId);
    }

    public List<Product> findOutOfStockProducts(Long pharmacyId) {
        return productRepository.findByPharmacyIdAndStockQuantityAndActiveTrue(pharmacyId, 0);
    }

    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateStock(Long productId, Integer quantity) {
        Product product = getById(productId);
        product.setStockQuantity(quantity);
        return productRepository.save(product);
    }

    public Product increaseStock(Long productId, Integer amount) {
        Product product = getById(productId);
        product.setStockQuantity(product.getStockQuantity() + amount);
        return productRepository.save(product);
    }

    public Product decreaseStock(Long productId, Integer amount) {
        Product product = getById(productId);

        int newStock = product.getStockQuantity() - amount;
        if (newStock < 0) {
            throw BusinessException.insufficientStock(product.getName(), product.getStockQuantity());
        }

        product.setStockQuantity(newStock);
        return productRepository.save(product);
    }

    public void validateStock(Long productId, Integer requiredQuantity) {
        Product product = getById(productId);
        if (product.getStockQuantity() < requiredQuantity) {
            throw BusinessException.insufficientStock(product.getName(), product.getStockQuantity());
        }
        if (!product.isActive()) {
            throw new BusinessException("Product is not available: " + product.getName(), "PRODUCT_UNAVAILABLE");
        }
    }

    public Product setFeatured(Long productId, boolean featured) {
        Product product = getById(productId);
        product.setFeatured(featured);
        return productRepository.save(product);
    }

    public Product deactivateProduct(Long productId) {
        Product product = getById(productId);
        product.setActive(false);
        return productRepository.save(product);
    }

    public Product activateProduct(Long productId) {
        Product product = getById(productId);
        product.setActive(true);
        return productRepository.save(product);
    }

    public long countByPharmacy(Long pharmacyId) {
        return productRepository.countByPharmacyIdAndActiveTrue(pharmacyId);
    }

    private String generateSlug(String name, Long pharmacyId) {
        if (name == null) return "";

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("");

        slug = slug.replace("ı", "i").replace("İ", "i")
                .replace("ğ", "g").replace("Ğ", "g")
                .replace("ü", "u").replace("Ü", "u")
                .replace("ş", "s").replace("Ş", "s")
                .replace("ö", "o").replace("Ö", "o")
                .replace("ç", "c").replace("Ç", "c");

        slug = slug.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        String finalSlug = slug + "-" + pharmacyId;

        if (productRepository.existsBySlug(finalSlug)) {
            finalSlug = slug + "-" + System.currentTimeMillis();
        }

        return finalSlug;
    }
}
