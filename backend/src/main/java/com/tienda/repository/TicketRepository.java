package com.tienda.repository;

import com.tienda.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStoreOwnerIdOrderByCreatedAtDesc(Long storeOwnerId);

    List<Ticket> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

    List<Ticket> findByStatus(Ticket.TicketStatus status);

    List<Ticket> findByStatusInOrderByCreatedAtDesc(List<Ticket.TicketStatus> statuses);

    List<Ticket> findByStatusAndVotingEndDateBefore(Ticket.TicketStatus status, LocalDateTime dateTime);

    List<Ticket> findAllByOrderByCreatedAtDesc();

    Optional<Ticket> findByOrderId(Long orderId);
}
