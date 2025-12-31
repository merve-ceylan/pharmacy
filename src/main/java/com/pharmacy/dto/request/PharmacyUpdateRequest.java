package com.pharmacy.dto.request;

import jakarta.validation.constraints.*;

public class PharmacyUpdateRequest {

    @Size(min = 2, max = 200, message = "Pharmacy name must be between 2 and 200 characters")
    private String name;

    @Size(max = 100, message = "Custom domain cannot exceed 100 characters")
    private String customDomain;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "District cannot exceed 100 characters")
    private String district;

    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 20, message = "Tax number cannot exceed 20 characters")
    private String taxNumber;

    @Size(max = 200, message = "Tax office cannot exceed 200 characters")
    private String taxOffice;

    @Size(max = 50, message = "GLN number cannot exceed 50 characters")
    private String glnNumber;

    private String logoUrl;

    private String primaryColor;

    private String secondaryColor;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCustomDomain() { return customDomain; }
    public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }

    public String getTaxOffice() { return taxOffice; }
    public void setTaxOffice(String taxOffice) { this.taxOffice = taxOffice; }

    public String getGlnNumber() { return glnNumber; }
    public void setGlnNumber(String glnNumber) { this.glnNumber = glnNumber; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }
}