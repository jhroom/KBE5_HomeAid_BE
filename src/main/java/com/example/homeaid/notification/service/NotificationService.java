package com.example.homeaid.notification.service;

import com.example.homeaid.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequiredArgsConstructor
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private static final Long DEFAULT_TIMEOUT = 10L * 1000 * 60;

    public SseEmitter subscribe(Long userId, String lastEventId) {
        // 1
        String id = userId + "_" + System.currentTimeMillis();

        // 2
//        SseEmitter emitter = notificationRepository.save();
//
//        emitter.onCompletion(() -> notificationRepository.deleteById(id));
//        emitter.onTimeout(() -> notificationRepository.deleteById(id));
//
//        // 3
//        // 503 에러를 방지하기 위한 더미 이벤트 전송
//        sendToClient(emitter, id, "EventStream Created. [userId=" + userId + "]");
//
//        // 4
//        // 클라이언트가 미수신한 Event 목록이 존재할 경우 전송하여 Event 유실을 예방
//        if (!lastEventId.isEmpty()) {
//            Map<String, Object> events = emitterRepository.findAllEventCacheStartWithId(String.valueOf(userId));
//            events.entrySet().stream()
//                .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
//                .forEach(entry -> sendToClient(emitter, entry.getKey(), entry.getValue()));
//        }
//
//        return emitter;
        return null;
    }
}
