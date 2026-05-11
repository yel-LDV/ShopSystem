package com.tienda.controller;

import com.tienda.dto.MessageDto;
import com.tienda.entity.Message;
import com.tienda.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Map;

@Controller
public class ChatController {

    private final TicketService ticketService;

    public ChatController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @MessageMapping("/chat.send/{ticketId}")
    @SendTo("/topic/tickets/{ticketId}")
    public MessageDto sendMessage(@DestinationVariable Long ticketId,
                                  @Payload MessageDto dto,
                                  Principal principal) {
        Message message = ticketService.addMessage(
                ticketId,
                dto.getSenderId(),
                dto.getSenderRole(),
                dto.getContent()
        );

        return MessageDto.builder()
                .id(message.getId())
                .ticketId(message.getTicket().getId())
                .senderId(message.getSenderId())
                .senderRole(message.getSenderRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    @PostMapping("/api/messages")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessageRest(@RequestBody MessageDto dto) {
        Message message = ticketService.addMessage(
                dto.getTicketId(),
                dto.getSenderId(),
                dto.getSenderRole(),
                dto.getContent()
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", MessageDto.builder()
                        .id(message.getId())
                        .ticketId(message.getTicket().getId())
                        .senderId(message.getSenderId())
                        .senderRole(message.getSenderRole())
                        .content(message.getContent())
                        .createdAt(message.getCreatedAt())
                        .build()
        ));
    }
}
