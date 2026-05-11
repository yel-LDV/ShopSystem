package com.tienda.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt;

    private LocalDateTime votingEndDate;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    private ResolutionType finalResolution;

    private Integer discountPercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_owner_id")
    private StoreOwner storeOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    private ResolutionType storeOwnerVote;

    @Enumerated(EnumType.STRING)
    private ResolutionType supplierVote;

    @Enumerated(EnumType.STRING)
    private ResolutionType adminVote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private AdminUser admin;

    private BigDecimal proposedPrice;

    private String priceProposedBy;

    @Enumerated(EnumType.STRING)
    private NegotiationStatus negotiationStatus;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.status = TicketStatus.OPEN;
        this.negotiationStatus = NegotiationStatus.NONE;
    }

    public enum TicketStatus {
        OPEN, VOTING, NEGOTIATING, RESOLVED
    }

    public enum ResolutionType {
        CANCEL, ACCEPT, DISCOUNT
    }

    public enum NegotiationStatus {
        NONE, PROPOSED, ACCEPTED, REJECTED
    }
}
