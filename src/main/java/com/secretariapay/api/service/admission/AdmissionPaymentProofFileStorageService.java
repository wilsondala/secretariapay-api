package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AdmissionPaymentProofFileStorageService {

    private static final Map<String, FileType> ALLOWED_TYPES = Map.of(
            "application/pdf", new FileType("pdf", "application/pdf"),
            "image/png", new FileType("png", "image/png"),
            "image/jpeg", new FileType("jpg", "image/jpeg"),
            "image/jpg", new FileType("jpg", "image/jpeg")
    );

    private final AdmissionPublicPaymentService publicPaymentService;
    private final Path storageRoot;
    private final long maxSizeBytes;

    public AdmissionPaymentProofFileStorageService(
            AdmissionPublicPaymentService publicPaymentService,
            @Value("${secretariapay.admissions.payment-proof-storage-path:data/admission-payment-proofs}") String storagePath,
            @Value("${secretariapay.admissions.payment-proof-max-size-bytes:5242880}") long maxSizeBytes
    ) {
        this.publicPaymentService = publicPaymentService;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.maxSizeBytes = Math.max(1L, maxSizeBytes);
    }

    public AdmissionDto.PublicPaymentResponse storeAndSubmit(
            String applicationCode,
            String documentNumber,
            MultipartFile file
    ) {
        FileType fileType = validate(file);
        String storedName = UUID.randomUUID() + "." + fileType.extension();
        Path target = resolveStoredFile(storedName);

        try {
            Files.createDirectories(storageRoot);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return publicPaymentService.submitPaymentProof(
                    applicationCode,
                    new AdmissionDto.PublicPaymentProofRequest(
                            documentNumber,
                            "/api/v1/admissions/payment-proof-files/" + storedName,
                            sanitizeOriginalFilename(file.getOriginalFilename(), storedName),
                            fileType.mimeType()
                    )
            );
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new IllegalArgumentException(
                    "Não foi possível guardar o comprovativo. Tente novamente.",
                    exception
            );
        }
    }

    public StoredPaymentProofFile load(String storedName) {
        if (storedName == null || !storedName.matches("[0-9a-fA-F-]{36}\\.(pdf|png|jpg)")) {
            throw new NotFoundException("Comprovativo não encontrado.");
        }

        Path file = resolveStoredFile(storedName);
        if (!Files.isRegularFile(file)) {
            throw new NotFoundException("Comprovativo não encontrado.");
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("Comprovativo não encontrado.");
            }
            return new StoredPaymentProofFile(resource, mimeTypeFor(storedName), storedName);
        } catch (NotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NotFoundException("Comprovativo não encontrado.");
        }
    }

    private FileType validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new IllegalArgumentException("Selecione um comprovativo em PDF, JPG ou PNG.");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("O comprovativo não pode ultrapassar 5 MB.");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().trim().toLowerCase(Locale.ROOT);
        FileType fileType = ALLOWED_TYPES.get(contentType);
        if (fileType == null) {
            throw new IllegalArgumentException("Formato inválido. Envie um ficheiro PDF, JPG ou PNG.");
        }

        try (InputStream input = file.getInputStream()) {
            byte[] signature = input.readNBytes(8);
            if (!matchesSignature(signature, fileType.extension())) {
                throw new IllegalArgumentException(
                        "O conteúdo do ficheiro não corresponde ao formato informado."
                );
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Não foi possível ler o comprovativo enviado.", exception);
        }

        return fileType;
    }

    private boolean matchesSignature(byte[] signature, String extension) {
        if ("pdf".equals(extension)) {
            return signature.length >= 5
                    && signature[0] == '%'
                    && signature[1] == 'P'
                    && signature[2] == 'D'
                    && signature[3] == 'F'
                    && signature[4] == '-';
        }
        if ("png".equals(extension)) {
            int[] expected = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            if (signature.length < expected.length) return false;
            for (int index = 0; index < expected.length; index++) {
                if ((signature[index] & 0xFF) != expected[index]) return false;
            }
            return true;
        }
        return signature.length >= 3
                && (signature[0] & 0xFF) == 0xFF
                && (signature[1] & 0xFF) == 0xD8
                && (signature[2] & 0xFF) == 0xFF;
    }

    private Path resolveStoredFile(String storedName) {
        Path resolved = storageRoot.resolve(storedName).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Nome de comprovativo inválido.");
        }
        return resolved;
    }

    private String sanitizeOriginalFilename(String originalName, String fallback) {
        if (originalName == null || originalName.isBlank()) return fallback;
        String name = Path.of(originalName).getFileName().toString()
                .replaceAll("[\\r\\n]", "")
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isBlank() ? fallback : name.substring(0, Math.min(name.length(), 180));
    }

    private String mimeTypeFor(String storedName) {
        if (storedName.endsWith(".pdf")) return "application/pdf";
        if (storedName.endsWith(".png")) return "image/png";
        return "image/jpeg";
    }

    private void deleteQuietly(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // A falha original é mais relevante que a limpeza compensatória.
        }
    }

    private record FileType(String extension, String mimeType) {}

    public record StoredPaymentProofFile(
            Resource resource,
            String mimeType,
            String storedName
    ) {}
}
