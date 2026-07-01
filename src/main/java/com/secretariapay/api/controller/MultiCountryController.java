package com.secretariapay.api.controller;

import com.secretariapay.api.dto.multicountry.CountryResolutionRequest;
import com.secretariapay.api.dto.multicountry.CountryResolutionResponse;
import com.secretariapay.api.service.multicountry.CountryResolverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/multi-country")
public class MultiCountryController {

    private final CountryResolverService countryResolverService;

    public MultiCountryController(CountryResolverService countryResolverService) {
        this.countryResolverService = countryResolverService;
    }

    @PostMapping("/resolve")
    public ResponseEntity<CountryResolutionResponse> resolveCountry(
            @RequestBody CountryResolutionRequest request
    ) {
        CountryResolutionResponse response = countryResolverService.resolveByCities(
                request.originCity(),
                request.destinationCity()
        );

        return ResponseEntity.ok(response);
    }
}

