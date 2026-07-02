package com.secretariapay.api.dto.branding;

public class InstitutionBrandingResponse {

    private String institutionName;
    private String legalName;
    private String slug;
    private String slogan;
    private String address;
    private String country;
    private String currency;
    private String timezone;
    private String officialWhatsapp;
    private String supportEmail;
    private String academicPortalBaseUrl;
    private String platform;
    private String platformLogoUrl;
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;
    private Boolean active;

    public String getInstitutionName() { return institutionName; }
    public InstitutionBrandingResponse setInstitutionName(String institutionName) { this.institutionName = institutionName; return this; }

    public String getLegalName() { return legalName; }
    public InstitutionBrandingResponse setLegalName(String legalName) { this.legalName = legalName; return this; }

    public String getSlug() { return slug; }
    public InstitutionBrandingResponse setSlug(String slug) { this.slug = slug; return this; }

    public String getSlogan() { return slogan; }
    public InstitutionBrandingResponse setSlogan(String slogan) { this.slogan = slogan; return this; }

    public String getAddress() { return address; }
    public InstitutionBrandingResponse setAddress(String address) { this.address = address; return this; }

    public String getCountry() { return country; }
    public InstitutionBrandingResponse setCountry(String country) { this.country = country; return this; }

    public String getCurrency() { return currency; }
    public InstitutionBrandingResponse setCurrency(String currency) { this.currency = currency; return this; }

    public String getTimezone() { return timezone; }
    public InstitutionBrandingResponse setTimezone(String timezone) { this.timezone = timezone; return this; }

    public String getOfficialWhatsapp() { return officialWhatsapp; }
    public InstitutionBrandingResponse setOfficialWhatsapp(String officialWhatsapp) { this.officialWhatsapp = officialWhatsapp; return this; }

    public String getSupportEmail() { return supportEmail; }
    public InstitutionBrandingResponse setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; return this; }

    public String getAcademicPortalBaseUrl() { return academicPortalBaseUrl; }
    public InstitutionBrandingResponse setAcademicPortalBaseUrl(String academicPortalBaseUrl) { this.academicPortalBaseUrl = academicPortalBaseUrl; return this; }

    public String getPlatform() { return platform; }
    public InstitutionBrandingResponse setPlatform(String platform) { this.platform = platform; return this; }

    public String getPlatformLogoUrl() { return platformLogoUrl; }
    public InstitutionBrandingResponse setPlatformLogoUrl(String platformLogoUrl) { this.platformLogoUrl = platformLogoUrl; return this; }

    public String getPrimaryColor() { return primaryColor; }
    public InstitutionBrandingResponse setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; return this; }

    public String getSecondaryColor() { return secondaryColor; }
    public InstitutionBrandingResponse setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; return this; }

    public String getAccentColor() { return accentColor; }
    public InstitutionBrandingResponse setAccentColor(String accentColor) { this.accentColor = accentColor; return this; }

    public Boolean getActive() { return active; }
    public InstitutionBrandingResponse setActive(Boolean active) { this.active = active; return this; }
}
