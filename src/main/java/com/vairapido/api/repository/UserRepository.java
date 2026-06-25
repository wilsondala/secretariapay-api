package com.vairapido.api.repository;

import com.vairapido.api.entity.User;
import com.vairapido.api.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByWhatsapp(String whatsapp);

    Optional<User> findByWhatsappAndStatus(String whatsapp, UserStatus status);

    Optional<User> findFirstByWhatsappAndStatusOrderByUpdatedAtDesc(String whatsapp, UserStatus status);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByWhatsapp(String whatsapp);
}
