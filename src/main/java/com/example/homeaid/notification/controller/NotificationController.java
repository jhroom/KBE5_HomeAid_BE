package com.example.homeaid.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller

public class NotificationController {

    @GetMapping("/match")
    public String managerMatching() {
        return "managerMatch.html";
    }

    @PostMapping("/api/notification/{id}")
    public ResponseEntity<Void> subscribe(@PathVariable Integer id) {

        return null;
    }
}
