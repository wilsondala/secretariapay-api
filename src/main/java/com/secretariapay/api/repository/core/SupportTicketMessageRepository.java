package com.secretariapay.api.repository.core;
import com.secretariapay.api.entity.core.SupportTicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage, UUID> { List<SupportTicketMessage> findByTicketIdOrderByCreatedAtAsc(UUID ticketId); }
