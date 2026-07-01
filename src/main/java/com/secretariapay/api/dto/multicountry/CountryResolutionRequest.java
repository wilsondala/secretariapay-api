package com.secretariapay.api.dto.multicountry;

public record CountryResolutionRequest(
        String originCity,
        String destinationCity
) {
}

