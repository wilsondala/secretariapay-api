package com.secretariapay.api.service;

import com.secretariapay.api.dto.passenger.PassengerRequest;
import com.secretariapay.api.dto.passenger.PassengerResponse;
import com.secretariapay.api.entity.Passenger;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.PassengerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PassengerService {

    private final PassengerRepository repository;

    public PassengerService(PassengerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PassengerResponse create(PassengerRequest request) {
        if (repository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new IllegalArgumentException("Já existe um passageiro cadastrado com este documento.");
        }

        Passenger passenger = new Passenger()
                .setFullName(request.getFullName())
                .setDocumentNumber(request.getDocumentNumber())
                .setEmail(request.getEmail())
                .setPhone(request.getPhone())
                .setWhatsapp(request.getWhatsapp())
                .setBirthDate(request.getBirthDate());

        return toResponse(repository.save(passenger));
    }

    @Transactional(readOnly = true)
    public List<PassengerResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PassengerResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public PassengerResponse update(UUID id, PassengerRequest request) {
        Passenger passenger = findEntityById(id);

        repository.findByDocumentNumber(request.getDocumentNumber())
                .filter(existingPassenger -> !existingPassenger.getId().equals(id))
                .ifPresent(existingPassenger -> {
                    throw new IllegalArgumentException("Já existe outro passageiro cadastrado com este documento.");
                });

        passenger
                .setFullName(request.getFullName())
                .setDocumentNumber(request.getDocumentNumber())
                .setEmail(request.getEmail())
                .setPhone(request.getPhone())
                .setWhatsapp(request.getWhatsapp())
                .setBirthDate(request.getBirthDate());

        return toResponse(repository.save(passenger));
    }

    @Transactional
    public void delete(UUID id) {
        Passenger passenger = findEntityById(id);
        repository.delete(passenger);
    }

    private Passenger findEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Passageiro não encontrado."));
    }

    private PassengerResponse toResponse(Passenger passenger) {
        return new PassengerResponse()
                .setId(passenger.getId())
                .setFullName(passenger.getFullName())
                .setDocumentNumber(passenger.getDocumentNumber())
                .setEmail(passenger.getEmail())
                .setPhone(passenger.getPhone())
                .setWhatsapp(passenger.getWhatsapp())
                .setBirthDate(passenger.getBirthDate())
                .setCreatedAt(passenger.getCreatedAt())
                .setUpdatedAt(passenger.getUpdatedAt());
    }
}
