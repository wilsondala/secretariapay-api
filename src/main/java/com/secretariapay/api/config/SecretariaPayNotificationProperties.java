package com.secretariapay.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "secretariapay.notifications")
public class SecretariaPayNotificationProperties {

    private String publicGuideBaseUrl = "https://painel-secretariapay.paixaoangola.com/guias";
    private boolean emailEnabled = false;
    private String emailFrom = "dcr_pay@imetroangola.com";
    private String emailCc = "df.oi_pay@imetroangola.com";
    private String emailSenderName = "SecretáriaPay Académico";
    private boolean smsEnabled = false;
    private String smsProvider = "MOCK";
    private String smsApiUrl = "";
    private String smsSenderId = "SecretariaPay";

    public String getPublicGuideBaseUrl() {
        return publicGuideBaseUrl;
    }

    public void setPublicGuideBaseUrl(String publicGuideBaseUrl) {
        this.publicGuideBaseUrl = publicGuideBaseUrl;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public String getEmailCc() {
        return emailCc;
    }

    public void setEmailCc(String emailCc) {
        this.emailCc = emailCc;
    }

    public String getEmailSenderName() {
        return emailSenderName;
    }

    public void setEmailSenderName(String emailSenderName) {
        this.emailSenderName = emailSenderName;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public String getSmsProvider() {
        return smsProvider;
    }

    public void setSmsProvider(String smsProvider) {
        this.smsProvider = smsProvider;
    }

    public String getSmsApiUrl() {
        return smsApiUrl;
    }

    public void setSmsApiUrl(String smsApiUrl) {
        this.smsApiUrl = smsApiUrl;
    }

    public String getSmsSenderId() {
        return smsSenderId;
    }

    public void setSmsSenderId(String smsSenderId) {
        this.smsSenderId = smsSenderId;
    }
}
