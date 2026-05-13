package com.tienda.service;

import com.tienda.entity.EmailQueue;
import com.tienda.repository.EmailQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailQueueRepository emailQueueRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, EmailQueueRepository emailQueueRepository) {
        this.mailSender = mailSender;
        this.emailQueueRepository = emailQueueRepository;
    }

    @Async
    public void sendEmail(String to, String subject, String body) {
        EmailQueue queue = EmailQueue.builder()
                .recipient(to)
                .subject(subject)
                .body(body)
                .build();
        emailQueueRepository.save(queue);
        sendQueuedEmail(queue);
    }

    private void sendQueuedEmail(EmailQueue queue) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(queue.getRecipient());
            message.setSubject(queue.getSubject());
            message.setText(queue.getBody());

            mailSender.send(message);

            queue.setStatus(EmailQueue.EmailStatus.SENT);
            emailQueueRepository.save(queue);
            log.info("Email enviado a {}", queue.getRecipient());
        } catch (Exception e) {
            queue.setRetryCount(queue.getRetryCount() + 1);
            queue.setLastError(e.getMessage());
            if (queue.getRetryCount() >= queue.getMaxRetries()) {
                queue.setStatus(EmailQueue.EmailStatus.FAILED);
            }
            emailQueueRepository.save(queue);
            log.error("Error enviando email a {}: {}", queue.getRecipient(), e.getMessage());
        }
    }

    @Scheduled(fixedRate = 300000)
    public void retryFailedEmails() {
        try {
            List<EmailQueue> pending = emailQueueRepository
                    .findByStatusAndRetryCountLessThan(EmailQueue.EmailStatus.PENDING, 3);

            for (EmailQueue queue : pending) {
                sendQueuedEmail(queue);
            }
        } catch (Exception e) {
            log.error("Error en reintento de emails: {}", e.getMessage());
        }
    }
}
