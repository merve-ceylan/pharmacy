package com.pharmacy.repository;

import com.pharmacy.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Find by pharmacy
    Page<AuditLog> findByPharmacyId(Long pharmacyId, Pageable pageable);

    // Find by user
    List<AuditLog> findByUserId(Long userId);

    // Find by action type
    List<AuditLog> findByActionType(String actionType);

    // Find by entity type
    List<AuditLog> findByEntityType(String entityType);

    // Find by pharmacy and action type
    Page<AuditLog> findByPharmacyIdAndActionType(Long pharmacyId, String actionType, Pageable pageable);

    // Find by pharmacy and entity type
    Page<AuditLog> findByPharmacyIdAndEntityType(Long pharmacyId, String entityType, Pageable pageable);

    // Find by pharmacy and date range
    Page<AuditLog> findByPharmacyIdAndCreatedAtBetween(
            Long pharmacyId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    // Find by entity
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    // Find recent logs by pharmacy
    List<AuditLog> findTop50ByPharmacyIdOrderByCreatedAtDesc(Long pharmacyId);

    // Find all system logs (no pharmacy)
    Page<AuditLog> findByPharmacyIdIsNull(Pageable pageable);
}
