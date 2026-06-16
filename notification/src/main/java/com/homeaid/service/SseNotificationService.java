package com.homeaid.service;

import com.homeaid.domain.Notification;

import com.homeaid.domain.enumerate.UserRole;
import com.homeaid.dto.RequestAlert;
import com.homeaid.dto.ResponseAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseNotificationService {

    private final Map<Long, SseEmitter> connections = new ConcurrentHashMap<>();
    private final Set<Long> adminIds = ConcurrentHashMap.newKeySet();
    private final NotificationService notificationService;
    private final Long SSE_TIMEOUT;

    public SseNotificationService(NotificationService notificationService,
                                  @Value("${sse.timeout}") Long sseTimeout) {
        this.notificationService = notificationService;
        this.SSE_TIMEOUT = sseTimeout;
    }

    public SseEmitter createConnection(Long userId, UserRole userRole) {

        SseEmitter existingEmitter = connections.get(userId);
        if (existingEmitter != null) {
            try {
                existingEmitter.complete();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        connections.put(userId, emitter);

        if (userRole == UserRole.ADMIN) {
            adminIds.add(userId);
        }
        // 연결 정리 이벤트 처리
        emitter.onCompletion(() -> {
            removeConnection(userId);
        });
        emitter.onTimeout(() -> {
            removeConnection(userId);
        });
        emitter.onError(e -> {
            removeConnection(userId);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected successfully"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    // 특정 사용자에게 실시간 알림 전송
    public boolean sendAlertToUser(Long targetId, ResponseAlert responseAlert) {
        SseEmitter emitter = connections.get(targetId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("new-notification")
                        .data(responseAlert));
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    // 이미 DB에 저장된 알림을 SSE로 전송한다(저장은 발행 측에서 완료됨).
    @Async
    public void deliver(RequestAlert requestAlert) {
        Long targetId = requestAlert.getTargetId();
        if (targetId == null || !connections.containsKey(targetId)) {
            return; // 미접속 → DB에 남아 재연결 시 백필
        }

        Notification notification = notificationService.getNotification(requestAlert.getNotificationId());
        if (notification == null) {
            return;
        }

        if (sendAlertToUser(targetId, ResponseAlert.toDto(notification))) {
            notificationService.markDelivered(notification.getId());
        }
    }

    @Async
    public void deliverAdmin(RequestAlert requestAlert) {
        if (adminIds.isEmpty()) {
            return;
        }

        Notification notification = notificationService.getNotification(requestAlert.getNotificationId());
        if (notification == null) {
            return;
        }

        if (broadcastAdminAlert(notification)) {
            notificationService.markDelivered(notification.getId());
        }
    }

    public boolean broadcastAdminAlert(Notification notification) {
        ResponseAlert payload = ResponseAlert.toDto(notification);
        boolean deliveredToAny = false;
        for (Long adminId : connections.keySet()) {
            if (!adminIds.contains(adminId)) {
                continue;
            }
            SseEmitter emitter = connections.get(adminId);
            try {
                emitter.send(SseEmitter.event()
                        .name("new-notification")
                        .data(payload));
                deliveredToAny = true;
            } catch (IOException e) {
                removeConnection(adminId);
            }
        }
        return deliveredToAny;
    }

    public void gracefulDisconnect(Long userId) {
        SseEmitter emitter = connections.get(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } finally {
                removeConnection(userId);
            }
        }
    }

    private void removeConnection(Long userId) {
        connections.remove(userId);
        adminIds.remove(userId);
    }

    @Scheduled(fixedRate = 30000) //30초마다 핑
    public void sendHeartbeat() {
        connections.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data(System.currentTimeMillis()));
            } catch (IOException ignored) {
                removeConnection(userId);
            }
        });
    }
}
