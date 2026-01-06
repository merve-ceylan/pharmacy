package com.pharmacy.controller;

import com.pharmacy.entity.Favorite;
import com.pharmacy.entity.User;
import com.pharmacy.service.FavoriteService;
import com.pharmacy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService;

    @Autowired
    public FavoriteController(FavoriteService favoriteService, UserService userService) {
        this.favoriteService = favoriteService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getFavorites(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        List<Favorite> favorites = favoriteService.getUserFavorites(user);

        List<Map<String, Object>> response = favorites.stream().map(fav -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", fav.getId());
            map.put("productId", fav.getProduct().getId());
            map.put("productName", fav.getProduct().getName());
            map.put("productSlug", fav.getProduct().getSlug());
            map.put("productPrice", fav.getProduct().getPrice());
            map.put("productDiscountedPrice", fav.getProduct().getDiscountedPrice());
            map.put("productImageUrl", fav.getProduct().getImageUrl());
            map.put("createdAt", fav.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> addFavorite(Authentication authentication, @RequestBody Map<String, Long> request) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        Long productId = request.get("productId");
        if (productId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "productId gerekli"));
        }

        try {
            Favorite favorite = favoriteService.addFavorite(user, productId);
            Map<String, Object> response = new HashMap<>();
            response.put("id", favorite.getId());
            response.put("message", "Favorilere eklendi");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<?> removeFavorite(Authentication authentication, @PathVariable Long favoriteId) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        try {
            favoriteService.removeFavorite(user, favoriteId);
            return ResponseEntity.ok(Map.of("message", "Favorilerden kaldırıldı"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<?> checkFavorite(Authentication authentication, @PathVariable Long productId) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        boolean isFavorite = favoriteService.isFavorite(user, productId);
        Long favoriteId = favoriteService.getFavoriteId(user, productId).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("isFavorite", isFavorite);
        response.put("favoriteId", favoriteId);

        return ResponseEntity.ok(response);
    }
}