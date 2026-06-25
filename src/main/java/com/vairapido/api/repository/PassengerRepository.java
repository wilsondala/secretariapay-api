package com.vairapido.api.repository;

import com.vairapido.api.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PassengerRepository extends JpaRepository<Passenger, UUID> {

    Optional<Passenger> findByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumber(String documentNumber);

    Optional<Passenger> findByWhatsapp(String whatsapp);

    Optional<Passenger> findFirstByWhatsappOrderByUpdatedAtDesc(String whatsapp);
}
