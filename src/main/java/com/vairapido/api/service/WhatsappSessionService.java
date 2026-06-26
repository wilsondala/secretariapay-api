package com.vairapido.api.service;

import com.vairapido.api.dto.whatsappsession.WhatsappSessionResponse;
import com.vairapido.api.dto.whatsappsession.WhatsappSessionStartRequest;
import com.vairapido.api.entity.Passenger;
import com.vairapido.api.entity.User;
import com.vairapido.api.entity.WhatsappSession;
import com.vairapido.api.entity.enums.UserStatus;
import com.vairapido.api.entity.enums.WhatsappConversationStep;
import com.vairapido.api.entity.enums.WhatsappSessionStatus;
import com.vairapido.api.entity.enums.WhatsappSessionType;
import com.vairapido.api.repository.PassengerRepository;
import com.vairapido.api.repository.UserRepository;
import com.vairapido.api.repository.WhatsappSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WhatsappSessionService {

    private final WhatsappSessionRepository whatsappSessionRepository;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;

    public WhatsappSessionService(
            WhatsappSessionRepository whatsappSessionRepository,
            UserRepository userRepository,
            PassengerRepository passengerRepository
    ) {
        this.whatsappSessionRepository = whatsappSessionRepository;
        this.userRepository = userRepository;
        this.passengerRepository = passengerRepository;
    }

    @Transactional
    public WhatsappSessionResponse startSession(WhatsappSessionStartRequest request) {
        expireOverdueSessions();

        String phoneNumber = normalizePhoneNumber(request.getPhoneNumber());
        LocalDateTime now = LocalDateTime.now();

        WhatsappSession session = whatsappSessionRepository
                .findFirstByPhoneNumberAndSessionTypeAndStatusOrderByUpdatedAtDesc(
                        phoneNumber,
                        request.getSessionType(),
                        WhatsappSessionStatus.ACTIVE
                )
                .filter(existing -> existing.getExpiresAt().isAfter(now))
                .orElseGet(() -> createNewSession(phoneNumber, request.getSessionType()));

        resetPassengerFlowAfterInactivityIfNeeded(session, request.getSessionType(), now);

        session
                .setLastMessageText(request.getMessageText())
                .setExpiresAt(now.plusHours(24));

        if (WhatsappSessionType.USER.equals(request.getSessionType())) {
            linkUser(session, phoneNumber);
        }

        if (WhatsappSessionType.PASSENGER.equals(request.getSessionType())) {
            linkPassenger(session, phoneNumber);
        }

        return toResponse(whatsappSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public WhatsappSessionResponse findActiveSession(String phoneNumber) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);

        WhatsappSession session = whatsappSessionRepository
                .findFirstByPhoneNumberAndStatusOrderByUpdatedAtDesc(
                        normalizedPhone,
                        WhatsappSessionStatus.ACTIVE
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sessão WhatsApp ativa não encontrada."
                ));

        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<WhatsappSessionResponse> findByPhoneNumber(String phoneNumber) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);

        return whatsappSessionRepository.findByPhoneNumberOrderByUpdatedAtDesc(normalizedPhone)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WhatsappSessionResponse closeSession(UUID id) {
        WhatsappSession session = whatsappSessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sessão WhatsApp não encontrada."
                ));

        session
                .setStatus(WhatsappSessionStatus.CLOSED)
                .setCurrentStep(WhatsappConversationStep.FINISHED);

        return toResponse(whatsappSessionRepository.save(session));
    }

    @Transactional
    public int expireOverdueSessions() {
        LocalDateTime now = LocalDateTime.now();

        List<WhatsappSession> overdueSessions =
                whatsappSessionRepository.findByStatusAndExpiresAtBefore(
                        WhatsappSessionStatus.ACTIVE,
                        now
                );

        for (WhatsappSession session : overdueSessions) {
            session
                    .setStatus(WhatsappSessionStatus.EXPIRED)
                    .setCurrentStep(WhatsappConversationStep.FINISHED);
        }

        whatsappSessionRepository.saveAll(overdueSessions);

        return overdueSessions.size();
    }

    private void resetPassengerFlowAfterInactivityIfNeeded(
            WhatsappSession session,
            WhatsappSessionType sessionType,
            LocalDateTime now) {
        if (session == null || !WhatsappSessionType.PASSENGER.equals(sessionType)) {
            return;
        }

        if (session.getUpdatedAt() == null) {
            return;
        }

        if (!session.getUpdatedAt().isBefore(now.minusMinutes(5))) {
            return;
        }

        if (WhatsappConversationStep.PASSENGER_IDENTIFICATION.equals(session.getCurrentStep())
                || WhatsappConversationStep.START.equals(session.getCurrentStep())
                || WhatsappConversationStep.TICKET_ISSUED.equals(session.getCurrentStep())
                || WhatsappConversationStep.FINISHED.equals(session.getCurrentStep())) {
            return;
        }

        session
                .setCurrentStep(WhatsappConversationStep.PASSENGER_IDENTIFICATION)
                .setMetadata(
                        "auto_reset_due_to_inactivity=true\n"
                                + "auto_reset_minutes=5\n"
                                + "auto_reset_at=" + now);
    }

    private WhatsappSession createNewSession(
            String phoneNumber,
            WhatsappSessionType sessionType
    ) {
        WhatsappConversationStep initialStep = WhatsappSessionType.USER.equals(sessionType)
                ? WhatsappConversationStep.START
                : WhatsappConversationStep.PASSENGER_IDENTIFICATION;

        return new WhatsappSession()
                .setPhoneNumber(phoneNumber)
                .setSessionType(sessionType)
                .setStatus(WhatsappSessionStatus.ACTIVE)
                .setCurrentStep(initialStep)
                .setExpiresAt(LocalDateTime.now().plusHours(24));
    }

    private void linkUser(WhatsappSession session, String phoneNumber) {
        User user = userRepository.findFirstByWhatsappAndStatusOrderByUpdatedAtDesc(
                        phoneNumber,
                        UserStatus.ACTIVE
                )
                .orElse(null);

        session.setUser(user);

        if (user != null) {
            user.setLastWhatsappLoginAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    private void linkPassenger(WhatsappSession session, String phoneNumber) {
        Passenger passenger = passengerRepository.findFirstByWhatsappOrderByUpdatedAtDesc(phoneNumber)
                .orElse(null);

        session.setPassenger(passenger);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Número do WhatsApp é obrigatório."
            );
        }

        String cleaned = phoneNumber.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        }

        return cleaned;
    }

    private WhatsappSessionResponse toResponse(WhatsappSession session) {
        WhatsappSessionResponse response = new WhatsappSessionResponse()
                .setId(session.getId())
                .setPhoneNumber(session.getPhoneNumber())
                .setSessionType(session.getSessionType())
                .setStatus(session.getStatus())
                .setCurrentStep(session.getCurrentStep())
                .setLastMessageText(session.getLastMessageText())
                .setMetadata(session.getMetadata())
                .setExpiresAt(session.getExpiresAt())
                .setCreatedAt(session.getCreatedAt())
                .setUpdatedAt(session.getUpdatedAt());

        if (session.getUser() != null) {
            response
                    .setUserId(session.getUser().getId())
                    .setUserFullName(session.getUser().getFullName())
                    .setUserEmail(session.getUser().getEmail())
                    .setUserRole(session.getUser().getRole().name());
        }

        if (session.getPassenger() != null) {
            response
                    .setPassengerId(session.getPassenger().getId())
                    .setPassengerFullName(session.getPassenger().getFullName())
                    .setPassengerDocumentNumber(session.getPassenger().getDocumentNumber());
        }

        return response;
    }
}
