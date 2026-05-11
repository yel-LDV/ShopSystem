package com.tienda.dto;

import com.tienda.entity.Ticket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDto {
    private Long id;
    private Long orderId;
    private String storeName;
    private String supplierName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime votingEndDate;
    private Ticket.ResolutionType finalResolution;
    private Integer discountPercentage;
    private Ticket.ResolutionType storeOwnerVote;
    private Ticket.ResolutionType supplierVote;
    private Ticket.ResolutionType adminVote;
    private BigDecimal proposedPrice;
    private String priceProposedBy;
    private Ticket.NegotiationStatus negotiationStatus;
    private List<MessageDto> messages;
}
