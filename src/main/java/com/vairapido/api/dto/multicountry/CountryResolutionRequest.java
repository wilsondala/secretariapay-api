package com.vairapido.api.dto.multicountry;

public record CountryResolutionRequest(
        String originCity,
        String destinationCity
) {
}
