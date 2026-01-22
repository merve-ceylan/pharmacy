package com.pharmacy.repository;

import com.pharmacy.entity.User;
import com.pharmacy.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find by email (for login)
    Optional<User> findByEmail(String email);

    // Find by email and pharmacy (multi-tenant)
    Optional<User> findByEmailAndPharmacyId(String email, Long pharmacyId);

    // Check if email exists
    boolean existsByEmail(String email);

    // Check if email exists in pharmacy
    boolean existsByEmailAndPharmacyId(String email, Long pharmacyId);

    // Find all users by pharmacy
    List<User> findByPharmacyId(Long pharmacyId);

    // Find users by pharmacy and role
    List<User> findByPharmacyIdAndRole(Long pharmacyId, UserRole role);

    // Find all users by role
    List<User> findByRole(UserRole role);

    // Find active users by pharmacy
    List<User> findByPharmacyIdAndActiveTrue(Long pharmacyId);

    // Find by phone
    Optional<User> findByPhone(String phone);

    // Fetch users with pharmacy data (avoid lazy loading)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.pharmacy")
    List<User> findAllWithPharmacy();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.pharmacy WHERE u.role = :role")
    List<User> findByRoleWithPharmacy(@Param("role") UserRole role);
}