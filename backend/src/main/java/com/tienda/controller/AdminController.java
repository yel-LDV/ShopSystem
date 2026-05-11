package com.tienda.controller;

import com.tienda.dto.*;
import com.tienda.entity.*;
import com.tienda.repository.*;
import com.tienda.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final RegistrationService registrationService;
    private final RegistrationRequestRepository registrationRequestRepository;
    private final TicketService ticketService;
    private final AuditService auditService;
    private final BackupService backupService;
    private final ProductService productService;
    private final UserService userService;
    private final UserRepository userRepository;

    public AdminController(RegistrationService registrationService,
                           RegistrationRequestRepository registrationRequestRepository,
                           TicketService ticketService,
                           AuditService auditService,
                           BackupService backupService,
                           ProductService productService,
                           UserService userService,
                           UserRepository userRepository) {
        this.registrationService = registrationService;
        this.registrationRequestRepository = registrationRequestRepository;
        this.ticketService = ticketService;
        this.auditService = auditService;
        this.backupService = backupService;
        this.productService = productService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/registrations")
    public ResponseEntity<ApiResponse<List<RegistrationRequest>>> getPendingRegistrations() {
        List<RegistrationRequest> pending = registrationRequestRepository
                .findByStatus(RegistrationRequest.RequestStatus.PENDING);
        return ResponseEntity.ok(ApiResponse.ok(pending));
    }

    @PostMapping("/registrations/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveRegistration(@PathVariable Long id) {
        registrationService.approveRegistration(id);
        return ResponseEntity.ok(ApiResponse.ok("Registro aprobado", null));
    }

    @PostMapping("/registrations/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRegistration(@PathVariable Long id) {
        registrationService.rejectRegistration(id);
        return ResponseEntity.ok(ApiResponse.ok("Registro rechazado", null));
    }

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<TicketDto>>> getTickets(
            @RequestParam(required = false) String status) {
        List<TicketDto> tickets;
        if (status != null && !status.isBlank()) {
            List<Ticket.TicketStatus> statuses = Arrays.stream(status.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(Ticket.TicketStatus::valueOf)
                    .toList();
            tickets = ticketService.getTicketsByStatus(statuses);
        } else {
            tickets = ticketService.getTicketsByStatus(
                    List.of(Ticket.TicketStatus.OPEN, Ticket.TicketStatus.VOTING, Ticket.TicketStatus.NEGOTIATING));
        }
        return ResponseEntity.ok(ApiResponse.ok(tickets));
    }

    @GetMapping("/tickets/history")
    public ResponseEntity<ApiResponse<List<TicketDto>>> getTicketHistory() {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getAllTickets()));
    }

    @PostMapping("/tickets/{id}/vote")
    public ResponseEntity<ApiResponse<TicketDto>> voteOnTicket(
            @PathVariable Long id,
            @RequestBody VoteRequest request,
            org.springframework.security.core.Authentication auth) {
        Long adminId = getUserId(auth);
        var ticket = ticketService.adminVote(id, adminId, request);
        return ResponseEntity.ok(ApiResponse.ok("Voto registrado", ticketService.getTicket(ticket.getId())));
    }

    @PostMapping("/tickets/{id}/cancel")
    public ResponseEntity<ApiResponse<TicketDto>> cancelTicket(
            @PathVariable Long id,
            org.springframework.security.core.Authentication auth) {
        Long adminId = getUserId(auth);
        var ticket = ticketService.cancelTicket(id, adminId, "ROLE_ADMIN");
        return ResponseEntity.ok(ApiResponse.ok("Ticket cancelado", ticketService.getTicket(ticket.getId())));
    }

    @PostMapping("/tickets/{id}/resolve")
    public ResponseEntity<ApiResponse<TicketDto>> resolveTicket(@PathVariable Long id,
                                                                  @RequestBody VoteRequest request) {
        TicketDto ticket = ticketService.getTicket(ticketService.adminResolve(id, request).getId());
        return ResponseEntity.ok(ApiResponse.ok("Ticket resuelto", ticket));
    }

    @GetMapping("/audit")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(auditService.getLogs(pageable)));
    }

    @GetMapping("/audit/search")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> searchAuditLogs(
            @RequestParam(required = false) String username, Pageable pageable) {
        if (username != null && !username.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(auditService.searchByUser(username, pageable)));
        }
        return ResponseEntity.ok(ApiResponse.ok(auditService.getLogs(pageable)));
    }

    @PostMapping("/backup/restore")
    public ResponseEntity<ApiResponse<Void>> restoreBackup(@RequestParam("file") MultipartFile file) {
        try {
            Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), file.getOriginalFilename());
            Files.write(tempFile, file.getBytes());
            backupService.restoreBackup(tempFile.toString());
            Files.deleteIfExists(tempFile);
            return ResponseEntity.ok(ApiResponse.ok("Backup restaurado exitosamente", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Error restaurando backup: " + e.getMessage()));
        }
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAllProducts()));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDto dto) {
        var product = productService.updateProduct(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Producto actualizado", productService.toDto(product)));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Producto eliminado", null));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers()));
    }

    @PostMapping("/users/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleUser(@PathVariable Long id) {
        userService.toggleUserEnabled(id);
        return ResponseEntity.ok(ApiResponse.ok("Estado del usuario actualizado", null));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario eliminado", null));
    }

    private Long getUserId(org.springframework.security.core.Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return user.getId();
    }
}
