package com.pharmacy.mapper;

import com.pharmacy.dto.request.ProductCreateRequest;
import com.pharmacy.dto.request.ProductUpdateRequest;
import com.pharmacy.dto.response.ProductResponse;
import com.pharmacy.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductCreateRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setBarcode(request.getBarcode());
        product.setPrice(request.getPrice());
        product.setDiscountedPrice(request.getDiscountPrice()); // discountPrice -> discountedPrice
        product.setStockQuantity(request.getStockQuantity());
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setImageUrl(request.getImageUrl());
        product.setFeatured(request.getFeatured() != null ? request.getFeatured() : false);
        product.setActive(request.getActive() != null ? request.getActive() : true);
        return product;
    }

    public void updateEntity(Product product, ProductUpdateRequest request) {
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getSku() != null) {
            product.setSku(request.getSku());
        }
        if (request.getBarcode() != null) {
            product.setBarcode(request.getBarcode());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getDiscountPrice() != null) {
            product.setDiscountedPrice(request.getDiscountPrice()); // discountPrice -> discountedPrice
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getLowStockThreshold() != null) {
            product.setLowStockThreshold(request.getLowStockThreshold());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (request.getFeatured() != null) {
            product.setFeatured(request.getFeatured());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
    }

    public ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setSlug(product.getSlug());
        response.setDescription(product.getDescription());
        response.setSku(product.getSku());
        response.setBarcode(product.getBarcode());
        response.setPrice(product.getPrice());
        response.setDiscountPrice(product.getDiscountedPrice()); // discountedPrice -> discountPrice
        response.setEffectivePrice(product.getEffectivePrice());
        response.setDiscountPercentage(product.getDiscountPercentage().intValue());
        response.setStockQuantity(product.getStockQuantity());
        response.setLowStockThreshold(product.getLowStockThreshold());
        response.setInStock(product.isInStock());
        response.setLowStock(product.isLowStock());
        response.setImageUrl(product.getImageUrl());
        response.setFeatured(product.isFeatured());
        response.setActive(product.isActive());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());

        if (product.getCategory() != null) {
            response.setCategoryId(product.getCategory().getId());
            response.setCategoryName(product.getCategory().getName());
        }

        return response;
    }
}