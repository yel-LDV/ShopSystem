package com.tienda.repository;

import com.tienda.entity.RegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long> {

    List<RegistrationRequest> findByStatus(RegistrationRequest.RequestStatus status);

    boolean existsByEmailAndStatus(String email, RegistrationRequest.RequestStatus status);
}
