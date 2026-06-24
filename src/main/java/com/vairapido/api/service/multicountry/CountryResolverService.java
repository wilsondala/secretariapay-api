package com.vairapido.api.service.multicountry;

import com.vairapido.api.domain.enums.Country;
import com.vairapido.api.domain.enums.Currency;
import com.vairapido.api.domain.enums.DocumentType;
import com.vairapido.api.domain.enums.PaymentMethod;
import com.vairapido.api.dto.multicountry.CountryResolutionResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CountryResolverService {

    private final CityCatalog cityCatalog;

    public CountryResolverService(CityCatalog cityCatalog) {
        this.cityCatalog = cityCatalog;
    }

    public CountryResolutionResponse resolveByCities(String originCity, String destinationCity) {
        Optional<Country> originCountry = cityCatalog.resolveCountryByCity(originCity);
        Optional<Country> destinationCountry = cityCatalog.resolveCountryByCity(destinationCity);

        if (originCountry.isPresent() && destinationCountry.isPresent()) {
            Country origin = originCountry.get();
            Country destination = destinationCountry.get();

            if (origin == destination) {
                return resolved(origin);
            }

            return needsConfirmation(
                    "Identifiquei cidades de países diferentes. Origem parece ser "
                            + origin
                            + " e destino parece ser "
                            + destination
                            + ". Por favor, confirme o país da viagem."
            );
        }

        if (originCountry.isPresent()) {
            return resolved(originCountry.get());
        }

        if (destinationCountry.isPresent()) {
            return resolved(destinationCountry.get());
        }

        return needsConfirmation(
                "Não consegui identificar o país pela cidade informada. Por favor, confirme se a viagem é no Brasil ou em Angola."
        );
    }

    public Currency currencyFor(Country country) {
        return switch (country) {
            case AO -> Currency.AOA;
            case BR -> Currency.BRL;
        };
    }

    public List<DocumentType> documentTypesFor(Country country) {
        return switch (country) {
            case AO -> List.of(DocumentType.BI, DocumentType.PASSPORT);
            case BR -> List.of(DocumentType.CPF, DocumentType.PASSPORT);
        };
    }

    public List<PaymentMethod> paymentMethodsFor(Country country) {
        return switch (country) {
            case AO -> List.of(
                    PaymentMethod.MULTICAIXA,
                    PaymentMethod.UNITEL_MONEY,
                    PaymentMethod.AFRIMONEY,
                    PaymentMethod.CASH
            );
            case BR -> List.of(
                    PaymentMethod.PIX,
                    PaymentMethod.CASH
            );
        };
    }

    private CountryResolutionResponse resolved(Country country) {
        return new CountryResolutionResponse(
                true,
                false,
                country,
                currencyFor(country),
                documentTypesFor(country),
                paymentMethodsFor(country),
                messageFor(country)
        );
    }

    private CountryResolutionResponse needsConfirmation(String message) {
        return new CountryResolutionResponse(
                false,
                true,
                null,
                null,
                List.of(),
                List.of(),
                message
        );
    }

    private String messageFor(Country country) {
        return switch (country) {
            case AO -> "País identificado: Angola. Documento aceito: BI ou Passaporte. Pagamentos: Multicaixa, Unitel Money, Afrimoney ou dinheiro.";
            case BR -> "País identificado: Brasil. Documento aceito: CPF ou Passaporte. Pagamentos: Pix ou dinheiro.";
        };
    }
}
