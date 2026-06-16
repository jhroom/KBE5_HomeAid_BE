package com.homeaid.service;

import com.homeaid.domain.Notification;

import com.homeaid.domain.enumerate.NotificationStatus;
import com.homeaid.domain.enumerate.UserRole;
import com.homeaid.exception.CustomException;
import com.homeaid.exception.NotificationErrorCode;
import com.homeaid.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    // 발행 측(Publisher)이 비즈니스 트랜잭션 안에서 호출 → 비즈니스 변경과 원자적으로 저장된다.
    // 저장 실패 시 예외를 전파해 전체 트랜잭션을 롤백한다(알림 유실 방지).
    @Transactional
    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Notification getNotification(Long notificationId) {
        return notificationRepository.findById(notificationId).orElse(null);
    }

    //연결시 사용자의 아직 읽지 않은 알림들(UNREAD + DELIVERED)
    @Transactional(readOnly = true)
    public List<Notification> getUnReadAlerts(Long userId, UserRole userRole) {
        if (UserRole.ADMIN.equals(userRole)) {
            return notificationRepository.findByTargetRoleAndStatusNotOrderByCreatedAtDesc(userRole, NotificationStatus.READ);
        } else {
            return notificationRepository.findByTargetIdAndStatusNotOrderByCreatedAtDesc(userId, NotificationStatus.READ);
        }
    }

    // SSE 전송 성공 후 호출 — id로 재조회한 managed 엔티티를 갱신해 dirty checking으로 반영
    @Transactional
    public void markDelivered(Long notificationId) {
        notificationRepository.findById(notificationId)
                .ifPresent(Notification::markAsDelivered);
    }

    @Transactional
    public void updateMarkRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() ->
                new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
    }
}