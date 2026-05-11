package com.tienda.service;

import com.tienda.dto.NotificationDto;
import com.tienda.entity.Notification;
import com.tienda.entity.User;
import com.tienda.repository.NotificationRepository;
import com.tienda.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyUser(Long userId, String message, String type, Long referenceId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .build();

        notification = notificationRepository.save(notification);

        NotificationDto dto = NotificationDto.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .type(notification.getType())
                .referenceId(notification.getReferenceId())
                .build();

        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/notifications",
                dto
        );
    }

    public void notifyStoreOwner(Long storeOwnerId, String message, String type, Long referenceId) {
        notifyUser(storeOwnerId, message, type, referenceId);
    }

    public void notifySupplier(Long supplierId, String message, String type, Long referenceId) {
        notifyUser(supplierId, message, type, referenceId);
    }

    public void notifyAdmin(String message, String type, Long referenceId) {
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> "ROLE_ADMIN".equals(u.getRole()))
                .collect(Collectors.toList());

        for (User admin : admins) {
            notifyUser(admin.getId(), message, type, referenceId);
        }
    }

    public List<NotificationDto> getNotifications(Long userId) {
        return notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> NotificationDto.builder()
                        .id(n.getId())
                        .message(n.getMessage())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .type(n.getType())
                        .referenceId(n.getReferenceId())
                        .build())
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
