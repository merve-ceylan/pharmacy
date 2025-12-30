package com.pharmacy.repository;

import com.pharmacy.entity.Pharmacy;
import com.pharmacy.enums.PharmacyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

    // Find by subdomain: ozan.pharmacyplatform.com
    Optional<Pharmacy> findBySubdomain(String subdomain);

    // Find by custom domain: ozaneczanesi.com
    Optional<Pharmacy> findByCustomDomain(String customDomain);

    // Find by either subdomain or custom domain
    Optional<Pharmacy> findBySubdomainOrCustomDomain(String subdomain, String customDomain);

    // Find all active pharmacies
    List<Pharmacy> findByStatus(PharmacyStatus status);

    // Check if subdomain exists
    boolean existsBySubdomain(String subdomain);

    // Check if custom domain exists
    boolean existsByCustomDomain(String customDomain);

    // Find by email
    Optional<Pharmacy> findByEmail(String email);
}
