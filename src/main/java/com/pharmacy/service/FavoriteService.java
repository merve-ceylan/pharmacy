package com.pharmacy.service;

import com.pharmacy.entity.Favorite;
import com.pharmacy.entity.User;
import com.pharmacy.entity.Product;
import com.pharmacy.repository.FavoriteRepository;
import com.pharmacy.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    @Autowired
    public FavoriteService(FavoriteRepository favoriteRepository, ProductRepository productRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
    }

    public List<Favorite> getUserFavorites(User user) {
        return favoriteRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public Favorite addFavorite(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı"));

        if (favoriteRepository.existsByUserAndProduct(user, product)) {
            throw new RuntimeException("Ürün zaten favorilerde");
        }

        Favorite favorite = new Favorite(user, product);
        return favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(User user, Long favoriteId) {
        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new RuntimeException("Favori bulunamadı"));

        if (!favorite.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bu favoriyi silme yetkiniz yok");
        }

        favoriteRepository.delete(favorite);
    }

    public boolean isFavorite(User user, Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return false;
        return favoriteRepository.existsByUserAndProduct(user, product);
    }

    public Optional<Long> getFavoriteId(User user, Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return Optional.empty();
        return favoriteRepository.findByUserAndProduct(user, product)
                .map(Favorite::getId);
    }
}