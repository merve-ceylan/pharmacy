package com.pharmacy.controller;

import com.pharmacy.dto.response.AdminStatsResponse;
import com.pharmacy.dto.response.PharmacyStatsResponse;
import com.pharmacy.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ==================== SUPER ADMIN ====================

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AdminStatsResponse> getSuperAdminStats() {
        return ResponseEntity.ok(adminService.getSuperAdminStats());
    }

    @GetMapping("/pharmacies/{pharmacyId}/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PharmacyStatsResponse> getPharmacyStats(
            @PathVariable Long pharmacyId,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(adminService.getPharmacyStats(pharmacyId, months));
    }

    // ==================== PHARMACY OWNER / STAFF ====================

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<AdminStatsResponse> getReports(
            @RequestParam(defaultValue = "week") String range) {
        return ResponseEntity.ok(adminService.getPharmacyReports(range));
    }
}
