package com.secretariapay.api.repository.core;
import com.secretariapay.api.entity.core.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> { List<SupportTicket> findByStatusOrderByCreatedAtDesc(String status); List<SupportTicket> findByStudentIdOrderByCreatedAtDesc(UUID studentId); Optional<SupportTicket> findByProtocolCode(String protocolCode); }
