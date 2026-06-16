package com.homeaid.service;

import com.homeaid.domain.Notification;
import com.homeaid.dto.RequestAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationService notificationService;
    public static final String NOTIFICATION_CHANNEL = "notification_channel";
    public static final String NOTIFICATION_ADMIN_CHANNEL = "notification_admin_channel";

    public void publishNotification(RequestAlert alert) {
        persistAndPublish(NOTIFICATION_CHANNEL, alert);
    }

    public void publishAdminNotification(RequestAlert alert) {
        persistAndPublish(NOTIFICATION_ADMIN_CHANNEL, alert);
    }

    // 1) 호출자(비즈니스)의 트랜잭션 안에서 알림을 먼저 저장하고(원자성),
    // 2) 커밋이 끝난 뒤에만 Redis로 발행한다(롤백 시 유령 알림 방지).
    private void persistAndPublish(String channel, RequestAlert alert) {
        Notification saved = notificationService.createNotification(RequestAlert.toNotification(alert));
        alert.setNotificationId(saved.getId());
        publishAfterCommit(channel, alert);
    }

    private void publishAfterCommit(String channel, RequestAlert alert) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisTemplate.convertAndSend(channel, alert);
                }
            });
        } else {
            redisTemplate.convertAndSend(channel, alert);
        }
    }
}
