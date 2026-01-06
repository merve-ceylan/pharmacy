package com.pharmacy.controller;

import com.pharmacy.entity.Address;
import com.pharmacy.entity.User;
import com.pharmacy.service.AddressService;
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
@RequestMapping("/api/customer/addresses")
public class AddressController {

    private final AddressService addressService;
    private final UserService userService;

    @Autowired
    public AddressController(AddressService addressService, UserService userService) {
        this.addressService = addressService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getAddresses(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        List<Address> addresses = addressService.getUserAddresses(user);

        List<Map<String, Object>> response = addresses.stream().map(this::mapAddress).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAddress(Authentication authentication, @PathVariable Long id) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        Address address = addressService.getAddressById(id, user)
                .orElseThrow(() -> new RuntimeException("Adres bulunamadı"));

        return ResponseEntity.ok(mapAddress(address));
    }

    @PostMapping
    public ResponseEntity<?> createAddress(Authentication authentication, @RequestBody Map<String, Object> request) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        try {
            String title = (String) request.get("title");
            String fullName = (String) request.get("fullName");
            String phone = (String) request.get("phone");
            String city = (String) request.get("city");
            String district = (String) request.get("district");
            String postalCode = (String) request.get("postalCode");
            String addressLine = (String) request.get("addressLine");
            boolean isDefault = request.get("isDefault") != null && (Boolean) request.get("isDefault");

            if (title == null || fullName == null || phone == null || city == null || district == null || addressLine == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Zorunlu alanlar eksik"));
            }

            Address address = addressService.createAddress(user, title, fullName, phone, city, district, postalCode, addressLine, isDefault);

            Map<String, Object> response = mapAddress(address);
            response.put("message", "Adres eklendi");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(Authentication authentication, @PathVariable Long id, @RequestBody Map<String, Object> request) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        try {
            Address address = addressService.getAddressById(id, user)
                    .orElseThrow(() -> new RuntimeException("Adres bulunamadı"));

            String title = (String) request.get("title");
            String fullName = (String) request.get("fullName");
            String phone = (String) request.get("phone");
            String city = (String) request.get("city");
            String district = (String) request.get("district");
            String postalCode = (String) request.get("postalCode");
            String addressLine = (String) request.get("addressLine");

            Address updated = addressService.updateAddress(address, title, fullName, phone, city, district, postalCode, addressLine);

            Map<String, Object> response = mapAddress(updated);
            response.put("message", "Adres güncellendi");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<?> setDefaultAddress(Authentication authentication, @PathVariable Long id) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        try {
            addressService.setDefaultAddress(user, id);
            return ResponseEntity.ok(Map.of("message", "Varsayılan adres güncellendi"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(Authentication authentication, @PathVariable Long id) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        try {
            addressService.deleteAddress(user, id);
            return ResponseEntity.ok(Map.of("message", "Adres silindi"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Map<String, Object> mapAddress(Address address) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", address.getId());
        map.put("title", address.getTitle());
        map.put("fullName", address.getFullName());
        map.put("phone", address.getPhone());
        map.put("city", address.getCity());
        map.put("district", address.getDistrict());
        map.put("postalCode", address.getPostalCode());
        map.put("addressLine", address.getAddressLine());
        map.put("isDefault", address.isDefault());
        map.put("createdAt", address.getCreatedAt());
        return map;
    }
}