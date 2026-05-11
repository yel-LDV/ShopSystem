package com.tienda.repository;

import com.tienda.entity.EmailQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailQueueRepository extends JpaRepository<EmailQueue, Long> {

    List<EmailQueue> findByStatusAndRetryCountLessThan(EmailQueue.EmailStatus status, int retryCount);
}
