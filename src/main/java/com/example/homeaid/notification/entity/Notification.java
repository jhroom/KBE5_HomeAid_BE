package com.example.homeaid.notification.entity;

import com.example.homeaid.notification.entity.enumerate.AlarmType;
import com.example.homeaid.notification.entity.enumerate.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;

@Getter
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String content;

    @Enumerated(EnumType.STRING)
    private UserType userType; // "CUSTOMER", "MANAGER", "ADMIN"

    @Enumerated(EnumType.STRING)
    private AlarmType alarmType; // 알림 종류 (예: MATCH_ACCEPTED, PAYMENT_CONFIRMED 등)

    @Column(columnDefinition = "boolean default false")
    private boolean read;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
