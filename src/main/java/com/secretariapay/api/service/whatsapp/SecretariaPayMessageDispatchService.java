package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.whatsapp.SecretariaPayDispatchBatchResponse;
import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageDispatchResult;
import com.secretariapay.api.entity.enums.whatsapp.SecretariaPayMessageStatus;
import com.secretariapay.api.entity.whatsapp.SecretariaPayMessage;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.whatsapp.SecretariaPayMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SecretariaPayMessageDispatchService {

    private final SecretariaPayMessageRepository repository;
    private final boolean whatsappEnabled;
    private final String phoneNumberId;
    private final String accessToken;

    public SecretariaPayMessageDispatchService(
            SecretariaPayMessageRepository repository,
            @Value("${secretariapay.whatsapp.enabled:false}") boolean whatsappEnabled,
            @Value("${secretariapay.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${secretariapay.whatsapp.access-token:}") String accessToken
    ) {
        this.repository = repository;
        this.whatsappEnabled = whatsappEnabled;
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
    }

    @Transactional
    public SecretariaPayMessageDispatchResult queue(UUID messageId) {
        SecretariaPayMessage message = findMessage(messageId);
        message.setStatus(SecretariaPayMessageStatus.QUEUED)
                .setFailureReason(null);

        SecretariaPayMessage saved = repository.save(message);
        return toResult(saved, LocalDateTime.now());
    }

    @Transactional
    public SecretariaPayMessageDispatchResult dispatch(UUID messageId) {
        SecretariaPayMessage message = findMessage(messageId);
        return dispatchMessage(message);
    }

    @Transactional
    public SecretariaPayDispatchBatchResponse processQueue(Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 10 : Math.min(limit, 20);
        List<SecretariaPayMessage> queuedMessages = repository.findTop20ByStatusOrderByCreatedAtAsc(SecretariaPayMessageStatus.QUEUED)
                .stream()
                .limit(safeLimit)
                .toList();

        List<SecretariaPayMessageDispatchResult> results = new ArrayList<>();
        int sent = 0;
        int failed = 0;

        for (SecretariaPayMessage message : queuedMessages) {
            SecretariaPayMessageDispatchResult result = dispatchMessage(message);
            results.add(result);

            if (SecretariaPayMessageStatus.SENT.name().equals(result.getStatus())) {
                sent++;
            }

            if (SecretariaPayMessageStatus.FAILED.name().equals(result.getStatus())) {
                failed++;
            }
        }

        return new SecretariaPayDispatchBatchResponse()
                .setProcessed(results.size())
                .setSent(sent)
                .setFailed(failed)
                .setResults(results);
    }

    private SecretariaPayMessageDispatchResult dispatchMessage(SecretariaPayMessage message) {
        LocalDateTime now = LocalDateTime.now();

        if (message.getRecipientPhone() == null || message.getRecipientPhone().isBlank()) {
            message.setStatus(SecretariaPayMessageStatus.FAILED)
                    .setFailureReason("Mensagem sem número de WhatsApp do destinatário.");
            return toResult(repository.save(message), now);
        }

        if (!whatsappEnabled) {
            String mockProviderMessageId = "mock-whatsapp-" + message.getId();
            message.setStatus(SecretariaPayMessageStatus.SENT)
                    .setProviderMessageId(mockProviderMessageId)
                    .setFailureReason(null)
                    .setSentAt(now);
            return toResult(repository.save(message), now);
        }

        if (phoneNumberId == null || phoneNumberId.isBlank() || accessToken == null || accessToken.isBlank()) {
            message.setStatus(SecretariaPayMessageStatus.FAILED)
                    .setFailureReason("WhatsApp Cloud API habilitado, mas phone-number-id ou access-token não configurado.");
            return toResult(repository.save(message), now);
        }

        // Ponto preparado para envio real via WhatsApp Cloud API.
        // Nesta fase, mantemos seguro: sem enviar para a Meta até validarmos credenciais oficiais.
        message.setStatus(SecretariaPayMessageStatus.FAILED)
                .setFailureReason("Envio real ainda não ativado neste build. Integração Cloud API preparada para próxima etapa.");
        return toResult(repository.save(message), now);
    }

    private SecretariaPayMessage findMessage(UUID messageId) {
        return repository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Mensagem do SecretáriaPay não encontrada."));
    }

    private SecretariaPayMessageDispatchResult toResult(SecretariaPayMessage message, LocalDateTime processedAt) {
        return new SecretariaPayMessageDispatchResult()
                .setMessageId(message.getId())
                .setStatus(message.getStatus() != null ? message.getStatus().name() : null)
                .setProviderMessageId(message.getProviderMessageId())
                .setFailureReason(message.getFailureReason())
                .setRecipientPhone(message.getRecipientPhone())
                .setProcessedAt(processedAt);
    }
}
