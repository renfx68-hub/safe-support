package com.safesupport.web;

import com.safesupport.domain.ApplicationRecord;
import com.safesupport.domain.Contact;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ApplyResponse(
        Long id,
        LocalDate effectiveDate,
        String note,
        Instant createdAt,
        String status,
        int total,
        int delivered,
        List<Notification> notifications
) {
    public record Notification(String name, String email, String target, String status, String message) {
    }

    public static ApplyResponse of(ApplicationRecord record) {
        List<Notification> notifications = record.getContacts().stream()
                .map(c -> new Notification(c.getName(), c.getEmail(), c.getTarget(), c.getStatus(), c.getMessage()))
                .toList();
        int delivered = (int) record.getContacts().stream()
                .filter(c -> "SENT".equals(c.getStatus()))
                .count();
        return new ApplyResponse(
                record.getId(),
                record.getEffectiveDate(),
                record.getNote(),
                record.getCreatedAt(),
                record.getStatus().name(),
                notifications.size(),
                delivered,
                notifications
        );
    }
}
