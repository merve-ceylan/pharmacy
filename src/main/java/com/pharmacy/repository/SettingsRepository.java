package com.pharmacy.repository;

import com.pharmacy.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, Long> {

    // Settings is a singleton - only one row
    Optional<Settings> findFirstByOrderByIdAsc();
}