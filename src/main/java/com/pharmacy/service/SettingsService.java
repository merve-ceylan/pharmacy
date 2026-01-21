package com.pharmacy.service;

import com.pharmacy.entity.Settings;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.SettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SettingsService {

    private final SettingsRepository settingsRepository;

    public SettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    /**
     * Get platform settings (singleton - only one record)
     */
    public Settings getSettings() {
        return settingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaultSettings);
    }

    /**
     * Update platform settings
     */
    public Settings updateSettings(Settings settings, String updatedBy) {
        Settings existing = getSettings();

        // Update fields
        existing.setPlatformName(settings.getPlatformName());
        existing.setPlatformEmail(settings.getPlatformEmail());
        existing.setSupportEmail(settings.getSupportEmail());
        existing.setSupportPhone(settings.getSupportPhone());
        existing.setGracePeriodDays(settings.getGracePeriodDays());
        existing.setDataRetentionDays(settings.getDataRetentionDays());
        existing.setMaxFailedLoginAttempts(settings.getMaxFailedLoginAttempts());
        existing.setAccountLockoutMinutes(settings.getAccountLockoutMinutes());
        existing.setEmailEnabled(settings.getEmailEnabled());
        existing.setEmailFromName(settings.getEmailFromName());
        existing.setEmailFromAddress(settings.getEmailFromAddress());
        existing.setTrialPeriodDays(settings.getTrialPeriodDays());
        existing.setAllowCustomDomains(settings.getAllowCustomDomains());
        existing.setMaintenanceMode(settings.getMaintenanceMode());
        existing.setRegistrationsEnabled(settings.getRegistrationsEnabled());
        existing.setEmailVerificationRequired(settings.getEmailVerificationRequired());
        existing.setUpdatedBy(updatedBy);

        return settingsRepository.save(existing);
    }

    /**
     * Create default settings on first run
     */
    private Settings createDefaultSettings() {
        Settings settings = new Settings();
        settings.setPlatformName("Pharmacy Platform");
        settings.setPlatformEmail("admin@pharmacyplatform.com");
        settings.setSupportEmail("support@pharmacyplatform.com");
        settings.setSupportPhone("+90 555 123 4567");
        settings.setGracePeriodDays(7);
        settings.setDataRetentionDays(30);
        settings.setMaxFailedLoginAttempts(5);
        settings.setAccountLockoutMinutes(30);
        settings.setEmailEnabled(true);
        settings.setEmailFromName("Pharmacy Platform");
        settings.setEmailFromAddress("noreply@pharmacyplatform.com");
        settings.setTrialPeriodDays(14);
        settings.setAllowCustomDomains(true);
        settings.setMaintenanceMode(false);
        settings.setRegistrationsEnabled(true);
        settings.setEmailVerificationRequired(false);
        settings.setUpdatedBy("system");

        return settingsRepository.save(settings);
    }

    /**
     * Check if maintenance mode is enabled
     */
    public boolean isMaintenanceMode() {
        return getSettings().getMaintenanceMode();
    }

    /**
     * Check if registrations are enabled
     */
    public boolean areRegistrationsEnabled() {
        return getSettings().getRegistrationsEnabled();
    }
}