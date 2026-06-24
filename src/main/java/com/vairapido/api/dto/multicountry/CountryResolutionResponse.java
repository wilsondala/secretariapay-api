package com.vairapido.api.dto.multicountry;

import com.vairapido.api.domain.enums.Country;
import com.vairapido.api.domain.enums.Currency;
import com.vairapido.api.domain.enums.DocumentType;
import com.vairapido.api.domain.enums.PaymentMethod;

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
