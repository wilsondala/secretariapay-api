package com.secretariapay.api.dto.multicountry;

import com.secretariapay.api.domain.enums.Country;
import com.secretariapay.api.domain.enums.Currency;
import com.secretariapay.api.domain.enums.DocumentType;
import com.secretariapay.api.domain.enums.PaymentMethod;

import java.util.List;

public record CountryResolutionResponse(
        boolean resolved,
        boolean needsCountryConfirmation,
        Country country,
        Currency currency,
        List<DocumentType> documentTypes,
        List<PaymentMethod> paymentMethods,
        String message
) {
}

