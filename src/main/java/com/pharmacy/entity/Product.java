package com.pharmacy.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_sku", columnList = "pharmacy_id, sku")
})
public class Product extends BaseEntity {

    // Multi-tenant: which pharmacy owns this product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    // Product category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    // URL-friendly name
    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Stock Keeping Unit - unique per pharmacy
    private String sku;

    private String barcode;

    private String brand;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Sale price (if on discount)
    @Column(name = "discounted_price", precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    // Alert when stock falls below this
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 10;

    @Column(name = "image_url")
    private String imageUrl;

    // Product weight in grams (for shipping calculation)
    private Integer weight;

    @Column(name = "is_active")
    private boolean active = true;

    // Featured product flag (for homepage)
    @Column(name = "is_featured")
    private boolean featured = false;

    // Getters and Setters
    public Pharmacy getPharmacy() {
        return pharmacy;
    }

    public void setPharmacy(Pharmacy pharmacy) {
        this.pharmacy = pharmacy;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    // Helper methods
    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public boolean isLowStock() {
        return stockQuantity <= lowStockThreshold;
    }

    public boolean hasDiscount() {
        return discountedPrice != null && discountedPrice.compareTo(price) < 0;
    }

    public BigDecimal getEffectivePrice() {
        return hasDiscount() ? discountedPrice : price;
    }

    public BigDecimal getDiscountPercentage() {
        if (!hasDiscount()) {
            return BigDecimal.ZERO;
        }
        return price.subtract(discountedPrice)
                .divide(price, 2, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
