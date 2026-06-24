package com.vairapido.api.controller;

import com.vairapido.api.dto.multicountry.CountryResolutionRequest;
import com.vairapido.api.dto.multicountry.CountryResolutionResponse;
import com.vairapido.api.service.multicountry.CountryResolverService;
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
