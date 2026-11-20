package com.safesupport.web;

import com.safesupport.domain.ApplicationRecord;
import com.safesupport.domain.Contact;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ApplySummary(
        Long id,
        LocalDate effectiveDate,
        Instant createdAt,
        int total,
        int delivered
) {
    public static ApplySummary of(ApplicationRecord record) {
        List<Contact> contacts = record.getContacts();
        int delivered = (int) contacts.stream().filter(c -> "SENT".equals(c.getStatus())).count();
        return new ApplySummary(record.getId(), record.getEffectiveDate(), record.getCreatedAt(), contacts.size(), delivered);
    }
}
