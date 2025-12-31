package com.pharmacy.mapper;

import com.pharmacy.dto.request.PharmacyCreateRequest;
import com.pharmacy.dto.request.PharmacyUpdateRequest;
import com.pharmacy.dto.response.PharmacyPublicResponse;
import com.pharmacy.dto.response.PharmacyResponse;
import com.pharmacy.entity.Pharmacy;
import org.springframework.stereotype.Component;

@Component
public class PharmacyMapper {

    private static final String BASE_DOMAIN = "eczanem.com";

    public Pharmacy toEntity(PharmacyCreateRequest request) {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setName(request.getName());
        pharmacy.setSubdomain(request.getSubdomain().toLowerCase());
        pharmacy.setCustomDomain(request.getCustomDomain());
        pharmacy.setAddress(request.getAddress());
        pharmacy.setCity(request.getCity());
        pharmacy.setDistrict(request.getDistrict());
        pharmacy.setPostalCode(request.getPostalCode());
        pharmacy.setPhone(request.getPhone());
        pharmacy.setEmail(request.getEmail());
        pharmacy.setTaxNumber(request.getTaxNumber());
        pharmacy.setTaxOffice(request.getTaxOffice());
        pharmacy.setGlnNumber(request.getGlnNumber());
        pharmacy.setSubscriptionPlan(request.getSubscriptionPlan());
        pharmacy.setPaymentPeriod(request.getPaymentPeriod());
        return pharmacy;
    }

    public void updateEntity(Pharmacy pharmacy, PharmacyUpdateRequest request) {
        if (request.getName() != null) {
            pharmacy.setName(request.getName());
        }
        if (request.getCustomDomain() != null) {
            pharmacy.setCustomDomain(request.getCustomDomain());
        }
        if (request.getAddress() != null) {
            pharmacy.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            pharmacy.setCity(request.getCity());
        }
        if (request.getDistrict() != null) {
            pharmacy.setDistrict(request.getDistrict());
        }
        if (request.getPostalCode() != null) {
            pharmacy.setPostalCode(request.getPostalCode());
        }
        if (request.getPhone() != null) {
            pharmacy.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            pharmacy.setEmail(request.getEmail());
        }
        if (request.getTaxNumber() != null) {
            pharmacy.setTaxNumber(request.getTaxNumber());
        }
        if (request.getTaxOffice() != null) {
            pharmacy.setTaxOffice(request.getTaxOffice());
        }
        if (request.getGlnNumber() != null) {
            pharmacy.setGlnNumber(request.getGlnNumber());
        }
        if (request.getLogoUrl() != null) {
            pharmacy.setLogoUrl(request.getLogoUrl());
        }
        if (request.getPrimaryColor() != null) {
            pharmacy.setPrimaryColor(request.getPrimaryColor());
        }
        if (request.getSecondaryColor() != null) {
            pharmacy.setSecondaryColor(request.getSecondaryColor());
        }
    }

    public PharmacyResponse toResponse(Pharmacy pharmacy) {
        PharmacyResponse response = new PharmacyResponse();
        response.setId(pharmacy.getId());
        response.setName(pharmacy.getName());
        response.setSubdomain(pharmacy.getSubdomain());
        response.setCustomDomain(pharmacy.getCustomDomain());
        response.setFullUrl(buildFullUrl(pharmacy));

        // Contact info
        response.setAddress(pharmacy.getAddress());
        response.setCity(pharmacy.getCity());
        response.setDistrict(pharmacy.getDistrict());
        response.setPostalCode(pharmacy.getPostalCode());
        response.setPhone(pharmacy.getPhone());
        response.setEmail(pharmacy.getEmail());

        // Business info
        response.setTaxNumber(pharmacy.getTaxNumber());
        response.setTaxOffice(pharmacy.getTaxOffice());
        response.setGlnNumber(pharmacy.getGlnNumber());

        // Branding
        response.setLogoUrl(pharmacy.getLogoUrl());
        response.setPrimaryColor(pharmacy.getPrimaryColor());
        response.setSecondaryColor(pharmacy.getSecondaryColor());

        // Subscription info
        response.setSubscriptionPlan(pharmacy.getSubscriptionPlan());
        response.setPaymentPeriod(pharmacy.getPaymentPeriod());
        response.setSetupFee(pharmacy.getSetupFee());
        response.setMonthlyFee(pharmacy.getMonthlyFee());
        response.setSubscriptionStartDate(pharmacy.getSubscriptionStartDate());
        response.setNextPaymentDate(pharmacy.getNextPaymentDate());
        response.setGracePeriodEnd(pharmacy.getGracePeriodEnd());

        // Status
        response.setStatus(pharmacy.getStatus());
        response.setSuspendedAt(pharmacy.getSuspendedAt());

        // Timestamps
        response.setCreatedAt(pharmacy.getCreatedAt());
        response.setUpdatedAt(pharmacy.getUpdatedAt());

        return response;
    }

    public PharmacyPublicResponse toPublicResponse(Pharmacy pharmacy) {
        PharmacyPublicResponse response = new PharmacyPublicResponse();
        response.setId(pharmacy.getId());
        response.setName(pharmacy.getName());
        response.setSubdomain(pharmacy.getSubdomain());
        response.setAddress(pharmacy.getAddress());
        response.setCity(pharmacy.getCity());
        response.setDistrict(pharmacy.getDistrict());
        response.setPhone(pharmacy.getPhone());
        response.setEmail(pharmacy.getEmail());
        response.setLogoUrl(pharmacy.getLogoUrl());
        response.setPrimaryColor(pharmacy.getPrimaryColor());
        response.setSecondaryColor(pharmacy.getSecondaryColor());
        return response;
    }

    private String buildFullUrl(Pharmacy pharmacy) {
        if (pharmacy.getCustomDomain() != null && !pharmacy.getCustomDomain().isEmpty()) {
            return "https://" + pharmacy.getCustomDomain();
        }
        return "https://" + pharmacy.getSubdomain() + "." + BASE_DOMAIN;
    }
}