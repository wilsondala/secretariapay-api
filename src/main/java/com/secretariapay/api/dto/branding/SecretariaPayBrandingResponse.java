package com.secretariapay.api.dto.branding;

public class SecretariaPayBrandingResponse {

    private String name;
    private String product;
    private String description;
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;
    private String company;
    private String platform;
    private String countryFocus;

    public String getName() {
        return name;
    }

    public SecretariaPayBrandingResponse setName(String name) {
        this.name = name;
        return this;
    }

    public String getProduct() {
        return product;
    }

    public SecretariaPayBrandingResponse setProduct(String product) {
        this.product = product;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public SecretariaPayBrandingResponse setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public SecretariaPayBrandingResponse setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        return this;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public SecretariaPayBrandingResponse setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
        return this;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public SecretariaPayBrandingResponse setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
        return this;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public SecretariaPayBrandingResponse setAccentColor(String accentColor) {
        this.accentColor = accentColor;
        return this;
    }

    public String getCompany() {
        return company;
    }

    public SecretariaPayBrandingResponse setCompany(String company) {
        this.company = company;
        return this;
    }

    public String getPlatform() {
        return platform;
    }

    public SecretariaPayBrandingResponse setPlatform(String platform) {
        this.platform = platform;
        return this;
    }

    public String getCountryFocus() {
        return countryFocus;
    }

    public SecretariaPayBrandingResponse setCountryFocus(String countryFocus) {
        this.countryFocus = countryFocus;
        return this;
    }
}
