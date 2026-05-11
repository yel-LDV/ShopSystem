package com.tienda.service;

import com.tienda.dto.RegisterRequest;
import com.tienda.entity.*;
import com.tienda.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final RegistrationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final SupplierRepository supplierRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public RegistrationService(RegistrationRequestRepository requestRepository,
                               UserRepository userRepository,
                               StoreOwnerRepository storeOwnerRepository,
                               SupplierRepository supplierRepository,
                               UserService userService,
                               NotificationService notificationService,
                               AuditService auditService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.storeOwnerRepository = storeOwnerRepository;
        this.supplierRepository = supplierRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public RegistrationRequest submitRegistration(RegisterRequest req) {
        if (userService.existsByEmail(req.getEmail())) {
            throw new RuntimeException("El email ya esta registrado");
        }
        if (requestRepository.existsByEmailAndStatus(req.getEmail(), RegistrationRequest.RequestStatus.PENDING)) {
            throw new RuntimeException("Ya existe una solicitud pendiente para este email");
        }

        RegistrationRequest request = RegistrationRequest.builder()
                .email(req.getEmail())
                .password(userService.encodePassword(req.getPassword()))
                .fullName(req.getFullName())
                .role(req.getRole())
                .storeName(req.getStoreName())
                .storeAddress(req.getStoreAddress())
                .companyName(req.getCompanyName())
                .contactPhone(req.getContactPhone())
                .emergencyEmail(req.getEmergencyEmail())
                .address(req.getAddress())
                .build();

        request = requestRepository.save(request);

        try {
            auditService.log(req.getEmail(), "REGISTRATION_SUBMITTED", "USER", request.getId(), null, req.getRole());
        } catch (Exception e) {
            log.error("Error al registrar auditoria de registro: {}", e.getMessage());
        }

        return request;
    }

    @Transactional
    public User approveRegistration(Long requestId) {
        RegistrationRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (req.getStatus() != RegistrationRequest.RequestStatus.PENDING) {
            throw new RuntimeException("La solicitud ya fue procesada");
        }

        User user;

        if ("ROLE_STORE".equals(req.getRole())) {
            StoreOwner store = new StoreOwner();
            store.setEmail(req.getEmail());
            store.setPassword(req.getPassword());
            store.setFullName(req.getFullName());
            store.setRole("ROLE_STORE");
            store.setEnabled(true);
            store.setStoreName(req.getStoreName());
            store.setAddress(req.getStoreAddress());
            user = storeOwnerRepository.save(store);
        } else if ("ROLE_SUPPLIER".equals(req.getRole())) {
            Supplier supplier = new Supplier();
            supplier.setEmail(req.getEmail());
            supplier.setPassword(req.getPassword());
            supplier.setFullName(req.getFullName());
            supplier.setRole("ROLE_SUPPLIER");
            supplier.setEnabled(true);
            supplier.setCompanyName(req.getCompanyName());
            supplier.setContactPhone(req.getContactPhone());
            supplier.setEmergencyEmail(req.getEmergencyEmail());
            supplier.setAddress(req.getAddress());
            user = supplierRepository.save(supplier);
        } else {
            throw new RuntimeException("Rol no valido");
        }

        req.setStatus(RegistrationRequest.RequestStatus.APPROVED);
        requestRepository.save(req);

        try {
            auditService.log(req.getEmail(), "REGISTRATION_APPROVED", "USER", user.getId(), "PENDING", req.getRole());
        } catch (Exception e) {
            log.error("Error al auditar REGISTRATION_APPROVED: {}", e.getMessage());
        }

        return user;
    }

    @Transactional
    public void rejectRegistration(Long requestId) {
        RegistrationRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        req.setStatus(RegistrationRequest.RequestStatus.REJECTED);
        requestRepository.save(req);

        try {
            auditService.log(req.getEmail(), "REGISTRATION_REJECTED", "USER", requestId, "PENDING", null);
        } catch (Exception e) {
            log.error("Error al auditar REGISTRATION_REJECTED: {}", e.getMessage());
        }
    }
}
