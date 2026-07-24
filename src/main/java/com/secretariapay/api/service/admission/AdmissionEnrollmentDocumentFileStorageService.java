package com.secretariapay.api.service.admission;

import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionEnrollmentDocumentFile;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionEnrollmentDocumentType;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionEnrollmentDocumentFileRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AdmissionEnrollmentDocumentFileStorageService {

    private static final ZoneId LUANDA_ZONE = ZoneId.of("Africa/Luanda");
    private static final Map<String, FileType> ALLOWED_TYPES = Map.of(
            "application/pdf", new FileType("pdf", "application/pdf"),
            "image/png", new FileType("png", "image/png"),
            "image/jpeg", new FileType("jpg", "image/jpeg"),
            "image/jpg", new FileType("jpg", "image/jpeg")
    );

    private final AdmissionApplicationRepository applicationRepository;
    private final AdmissionInvoiceRepository invoiceRepository;
    private final AdmissionEnrollmentDocumentFileRepository fileRepository;
    private final Path storageRoot;
    private final long maxSizeBytes;

    public AdmissionEnrollmentDocumentFileStorageService(
            AdmissionApplicationRepository applicationRepository,
            AdmissionInvoiceRepository invoiceRepository,
            AdmissionEnrollmentDocumentFileRepository fileRepository,
            @Value("${secretariapay.admissions.enrollment-document-storage-path:data/admission-enrollment-documents}") String storagePath,
            @Value("${secretariapay.admissions.enrollment-document-max-size-bytes:5242880}") long maxSizeBytes
    ) {
        this.applicationRepository = applicationRepository;
        this.invoiceRepository = invoiceRepository;
        this.fileRepository = fileRepository;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.maxSizeBytes = Math.max(1L, maxSizeBytes);
    }

    @Transactional(readOnly = true)
    public List<DocumentFileResponse> list(UUID applicationId) {
        ensureApplicationExists(applicationId);
        return fileRepository.findByApplicationIdOrderByUploadedAtAsc(applicationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DocumentFileResponse store(
            UUID applicationId,
            AdmissionEnrollmentDocumentType documentType,
            MultipartFile file,
            String uploadedBy
    ) {
        AdmissionApplication application = eligibleApplication(applicationId);
        FileType fileType = validate(file, documentType);
        List<AdmissionEnrollmentDocumentFile> previousFiles = List.of();

        if (documentType == AdmissionEnrollmentDocumentType.PASSPORT_PHOTO) {
            long currentPhotos = fileRepository.countByApplicationIdAndDocumentType(
                    applicationId,
                    AdmissionEnrollmentDocumentType.PASSPORT_PHOTO
            );
            if (currentPhotos >= 2) {
                throw new IllegalArgumentException(
                        "As duas fotografias já foram anexadas. Remova uma fotografia antes de substituir."
                );
            }
        } else {
            previousFiles = fileRepository.findByApplicationIdAndDocumentTypeOrderByUploadedAtAsc(
                    applicationId,
                    documentType
            );
        }

        String storedName = UUID.randomUUID() + "." + fileType.extension();
        Path applicationDirectory = storageRoot.resolve(applicationId.toString()).normalize();
        Path target = applicationDirectory.resolve(storedName).normalize();
        if (!target.startsWith(applicationDirectory)) {
            throw new IllegalArgumentException("Nome de documento inválido.");
        }

        try {
            Files.createDirectories(applicationDirectory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            registerNewFileRollbackCleanup(target);

            AdmissionEnrollmentDocumentFile entity = fileRepository.save(
                    new AdmissionEnrollmentDocumentFile()
                            .setApplication(application)
                            .setDocumentType(documentType)
                            .setStoredName(storedName)
                            .setOriginalFileName(sanitizeOriginalFilename(file.getOriginalFilename(), storedName))
                            .setMimeType(fileType.mimeType())
                            .setFileSize(file.getSize())
                            .setUploadedBy(clean(uploadedBy, "Secretaria / Admissões"))
                            .setUploadedAt(LocalDateTime.now(LUANDA_ZONE))
            );

            previousFiles.forEach(this::deleteEntityAndSchedulePhysicalFile);
            return toResponse(entity);
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new IllegalArgumentException(
                    "Não foi possível guardar o documento. Tente novamente.",
                    exception
            );
        }
    }

    @Transactional
    public void delete(UUID fileId) {
        AdmissionEnrollmentDocumentFile entity = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Documento não encontrado."));
        eligibleApplication(entity.getApplication().getId());
        deleteEntityAndSchedulePhysicalFile(entity);
    }

    @Transactional(readOnly = true)
    public StoredDocumentFile load(UUID fileId) {
        AdmissionEnrollmentDocumentFile entity = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Documento não encontrado."));
        Path file = resolvePhysicalFile(entity);
        if (!Files.isRegularFile(file)) {
            throw new NotFoundException("Documento não encontrado.");
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("Documento não encontrado.");
            }
            return new StoredDocumentFile(
                    resource,
                    entity.getMimeType(),
                    entity.getOriginalFileName()
            );
        } catch (NotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NotFoundException("Documento não encontrado.");
        }
    }

    @Transactional(readOnly = true)
    public boolean hasRequiredFiles(UUID applicationId, boolean studiedAbroad) {
        boolean photos = fileRepository.countByApplicationIdAndDocumentType(
                applicationId,
                AdmissionEnrollmentDocumentType.PASSPORT_PHOTO
        ) >= 2;
        boolean certificate = fileRepository.countByApplicationIdAndDocumentType(
                applicationId,
                AdmissionEnrollmentDocumentType.AUTHENTICATED_CERTIFICATE
        ) >= 1;
        boolean identity = fileRepository.countByApplicationIdAndDocumentType(
                applicationId,
                AdmissionEnrollmentDocumentType.IDENTITY_DOCUMENT
        ) >= 1;
        boolean equivalence = !studiedAbroad || fileRepository.countByApplicationIdAndDocumentType(
                applicationId,
                AdmissionEnrollmentDocumentType.EDUCATION_EQUIVALENCE
        ) >= 1;
        return photos && certificate && identity && equivalence;
    }

    private AdmissionApplication eligibleApplication(UUID applicationId) {
        AdmissionApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada."));
        if (application.getStatus() != AdmissionApplicationStatus.DOCUMENTATION_PENDING) {
            throw new IllegalArgumentException(
                    "Os anexos somente podem ser alterados enquanto a documentação estiver pendente."
            );
        }
        AdmissionInvoice invoice = invoiceRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("A cobrança da inscrição não foi encontrada."));
        if (invoice.getStatus() != AdmissionInvoiceStatus.PAID) {
            throw new IllegalArgumentException(
                    "Os documentos somente podem ser anexados depois da confirmação do pagamento da inscrição."
            );
        }
        return application;
    }

    private void ensureApplicationExists(UUID applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new NotFoundException("Candidatura não encontrada.");
        }
    }

    private FileType validate(MultipartFile file, AdmissionEnrollmentDocumentType documentType) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new IllegalArgumentException("Selecione um documento em PDF, JPG ou PNG.");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("O documento não pode ultrapassar 5 MB.");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().trim().toLowerCase(Locale.ROOT);
        FileType fileType = ALLOWED_TYPES.get(contentType);
        if (fileType == null) {
            throw new IllegalArgumentException("Formato inválido. Envie um ficheiro PDF, JPG ou PNG.");
        }
        if (documentType == AdmissionEnrollmentDocumentType.PASSPORT_PHOTO
                && "application/pdf".equals(fileType.mimeType())) {
            throw new IllegalArgumentException("As fotografias tipo passe devem ser enviadas em JPG ou PNG.");
        }

        try (InputStream input = file.getInputStream()) {
            byte[] signature = input.readNBytes(8);
            if (!matchesSignature(signature, fileType.extension())) {
                throw new IllegalArgumentException(
                        "O conteúdo do ficheiro não corresponde ao formato informado."
                );
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Não foi possível ler o documento enviado.", exception);
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

    private void deleteEntityAndSchedulePhysicalFile(AdmissionEnrollmentDocumentFile entity) {
        Path physicalFile = resolvePhysicalFile(entity);
        fileRepository.delete(entity);
        deleteAfterCommitOrNow(physicalFile);
    }

    private void deleteAfterCommitOrNow(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(target);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(target);
            }
        });
    }

    private void registerNewFileRollbackCleanup(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deleteQuietly(target);
                }
            }
        });
    }

    private Path resolvePhysicalFile(AdmissionEnrollmentDocumentFile entity) {
        Path applicationDirectory = storageRoot.resolve(entity.getApplication().getId().toString()).normalize();
        Path resolved = applicationDirectory.resolve(entity.getStoredName()).normalize();
        if (!resolved.startsWith(applicationDirectory)) {
            throw new IllegalArgumentException("Nome de documento inválido.");
        }
        return resolved;
    }

    private DocumentFileResponse toResponse(AdmissionEnrollmentDocumentFile entity) {
        return new DocumentFileResponse(
                entity.getId(),
                entity.getApplication().getId(),
                entity.getDocumentType(),
                entity.getOriginalFileName(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.getUploadedBy(),
                entity.getUploadedAt(),
                "/api/v1/admissions/enrollment-document-files/" + entity.getId() + "/content"
        );
    }

    private String sanitizeOriginalFilename(String originalName, String fallback) {
        if (originalName == null || originalName.isBlank()) return fallback;
        String name = Path.of(originalName).getFileName().toString()
                .replaceAll("[\\r\\n]", "")
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isBlank() ? fallback : name.substring(0, Math.min(name.length(), 180));
    }

    private void deleteQuietly(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // A operação principal continua auditável no banco.
        }
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }

    private record FileType(String extension, String mimeType) {}

    public record DocumentFileResponse(
            UUID id,
            UUID applicationId,
            AdmissionEnrollmentDocumentType documentType,
            String originalFileName,
            String mimeType,
            long fileSize,
            String uploadedBy,
            LocalDateTime uploadedAt,
            String contentUrl
    ) {}

    public record StoredDocumentFile(
            Resource resource,
            String mimeType,
            String originalFileName
    ) {}
}
