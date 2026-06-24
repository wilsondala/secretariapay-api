package com.vairapido.api.service.multicountry;

import com.vairapido.api.domain.enums.Country;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Map;
import java.util.Optional;

@Component
public class CityCatalog {

    private static final Map<String, Country> CITY_TO_COUNTRY = Map.ofEntries(
            // Angola
            Map.entry(normalizeStatic("Luanda"), Country.AO),
            Map.entry(normalizeStatic("Caxito"), Country.AO),
            Map.entry(normalizeStatic("Benguela"), Country.AO),
            Map.entry(normalizeStatic("Lobito"), Country.AO),
            Map.entry(normalizeStatic("Cuito"), Country.AO),
            Map.entry(normalizeStatic("Cabinda"), Country.AO),
            Map.entry(normalizeStatic("Menongue"), Country.AO),
            Map.entry(normalizeStatic("Ndalatando"), Country.AO),
            Map.entry(normalizeStatic("Sumbe"), Country.AO),
            Map.entry(normalizeStatic("Ondjiva"), Country.AO),
            Map.entry(normalizeStatic("Huambo"), Country.AO),
            Map.entry(normalizeStatic("Lubango"), Country.AO),
            Map.entry(normalizeStatic("Dundo"), Country.AO),
            Map.entry(normalizeStatic("Saurimo"), Country.AO),
            Map.entry(normalizeStatic("Malanje"), Country.AO),
            Map.entry(normalizeStatic("Luena"), Country.AO),
            Map.entry(normalizeStatic("Moçâmedes"), Country.AO),
            Map.entry(normalizeStatic("Namibe"), Country.AO),
            Map.entry(normalizeStatic("Uíge"), Country.AO),
            Map.entry(normalizeStatic("Mbanza Kongo"), Country.AO),
            Map.entry(normalizeStatic("Soyo"), Country.AO),

            // Brasil
            Map.entry(normalizeStatic("São Paulo"), Country.BR),
            Map.entry(normalizeStatic("Rio de Janeiro"), Country.BR),
            Map.entry(normalizeStatic("Campinas"), Country.BR),
            Map.entry(normalizeStatic("Belo Horizonte"), Country.BR),
            Map.entry(normalizeStatic("Curitiba"), Country.BR),
            Map.entry(normalizeStatic("Brasília"), Country.BR),
            Map.entry(normalizeStatic("Salvador"), Country.BR),
            Map.entry(normalizeStatic("Fortaleza"), Country.BR),
            Map.entry(normalizeStatic("Recife"), Country.BR),
            Map.entry(normalizeStatic("Porto Alegre"), Country.BR),
            Map.entry(normalizeStatic("Goiânia"), Country.BR),
            Map.entry(normalizeStatic("Manaus"), Country.BR),
            Map.entry(normalizeStatic("Florianópolis"), Country.BR)
    );

    public Optional<Country> resolveCountryByCity(String city) {
        if (city == null || city.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(CITY_TO_COUNTRY.get(normalize(city)));
    }

    public boolean isKnownCity(String city) {
        if (city == null || city.isBlank()) {
            return false;
        }

        return CITY_TO_COUNTRY.containsKey(normalize(city));
    }

    public String normalize(String value) {
        return normalizeStatic(value);
    }

    private static String normalizeStatic(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}