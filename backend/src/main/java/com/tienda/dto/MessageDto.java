package com.tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private Long id;
    private Long ticketId;
    private Long senderId;
    private String senderRole;
    private String senderName;
    private String content;
    private LocalDateTime createdAt;
}
