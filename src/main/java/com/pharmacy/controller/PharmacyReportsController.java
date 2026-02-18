package com.pharmacy.controller;

import com.pharmacy.exception.BadRequestException;
import com.pharmacy.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pharmacy")
@Tag(name = "Pharmacy Reports", description = "Pharmacy statistics and reports")
public class PharmacyReportsController {

    private final JdbcTemplate jdbcTemplate;
    private final SecurityUtils securityUtils;

    public PharmacyReportsController(JdbcTemplate jdbcTemplate, SecurityUtils securityUtils) {
        this.jdbcTemplate = jdbcTemplate;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get pharmacy reports",
            description = "Get statistics and reports for the current pharmacy",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> getPharmacyReports(
            @RequestParam(defaultValue = "week") String range) {

        Long pharmacyId = securityUtils.getCurrentPharmacyId()
                .orElseThrow(() -> new BadRequestException("No pharmacy associated with current user"));

        Map<String, Object> reports = new HashMap<>();

        // Total orders
        Long totalOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE pharmacy_id = ?",
                Long.class, pharmacyId);
        reports.put("totalOrders", totalOrders != null ? totalOrders : 0);

        // Total revenue
        Double totalRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE pharmacy_id = ? AND status != 'CANCELLED'",
                Double.class, pharmacyId);
        reports.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);

        // Total customers
        Long totalCustomers = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT customer_id) FROM orders WHERE pharmacy_id = ?",
                Long.class, pharmacyId);
        reports.put("totalCustomers", totalCustomers != null ? totalCustomers : 0);

        // Total products
        Long totalProducts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE pharmacy_id = ?",
                Long.class, pharmacyId);
        reports.put("totalProducts", totalProducts != null ? totalProducts : 0);

        // Today's orders
        Long todayOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE pharmacy_id = ? AND DATE(created_at) = CURRENT_DATE",
                Long.class, pharmacyId);
        reports.put("todayOrders", todayOrders != null ? todayOrders : 0);

        // Today's revenue
        Double todayRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE pharmacy_id = ? AND DATE(created_at) = CURRENT_DATE AND status != 'CANCELLED'",
                Double.class, pharmacyId);
        reports.put("todayRevenue", todayRevenue != null ? todayRevenue : 0.0);

        // Pending orders
        Long pendingOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE pharmacy_id = ? AND status IN ('PENDING', 'CONFIRMED', 'PREPARING')",
                Long.class, pharmacyId);
        reports.put("pendingOrders", pendingOrders != null ? pendingOrders : 0);

        // Average order value
        Double avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0.0;
        reports.put("averageOrderValue", avgOrderValue);

        return ResponseEntity.ok(reports);
    }
}