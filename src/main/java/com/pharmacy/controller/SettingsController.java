package com.pharmacy.controller;

import com.pharmacy.dto.response.ApiResponse;
import com.pharmacy.entity.Settings;
import com.pharmacy.security.SecurityUtils;
import com.pharmacy.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
@Tag(name = "Settings", description = "Platform settings management (Super Admin)")
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    private final SettingsService settingsService;
    private final SecurityUtils securityUtils;

    public SettingsController(SettingsService settingsService,
                              SecurityUtils securityUtils) {
        this.settingsService = settingsService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Get platform settings",
            description = "Get current platform settings",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Settings> getSettings() {
        Settings settings = settingsService.getSettings();
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Update platform settings",
            description = "Update platform settings (Super Admin only)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Settings>> updateSettings(@Valid @RequestBody Settings settings) {
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Settings updated = settingsService.updateSettings(settings, adminEmail);

        log.info("Platform settings updated by admin: {}", adminEmail);

        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", updated));
    }
}