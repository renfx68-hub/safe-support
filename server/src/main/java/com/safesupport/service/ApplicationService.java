package com.safesupport.service;

import com.safesupport.domain.ApplicationRecord;
import com.safesupport.domain.Contact;
import com.safesupport.notify.DeliveryResult;
import com.safesupport.notify.EmailNotifier;
import com.safesupport.repository.ApplicationRepository;
import com.safesupport.web.ApplyRequest;
import com.safesupport.web.ApplyResponse;
import com.safesupport.web.ApplySummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class ApplicationService {

    private final ApplicationRepository repository;
    private final EmailNotifier emailNotifier;

    public ApplicationService(ApplicationRepository repository, EmailNotifier emailNotifier) {
        this.repository = repository;
        this.emailNotifier = emailNotifier;
    }

    @Transactional
    public ApplyResponse create(ApplyRequest request) {
        LocalDate effectiveDate = request.effectiveDate();
        if (effectiveDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("生效日期不能早于今天");
        }

        ApplicationRecord record = new ApplicationRecord(effectiveDate, trimToNull(request.note()), Instant.now());
        request.contacts().forEach(c ->
                record.addContact(new Contact(c.name().trim(), c.phone().trim(), trimToNull(c.email()))));
        repository.save(record);

        int delivered = 0;
        for (Contact contact : record.getContacts()) {
            DeliveryResult result = emailNotifier.send(contact.getName(), contact.getEmail(), record.getEffectiveDate());
            contact.setTarget(result.target());
            contact.setStatus(result.status().name());
            contact.setMessage(result.message());
            if (result.status() == DeliveryResult.Status.SENT) {
                delivered++;
            }
        }
        repository.save(record);

        return ApplyResponse.of(record, delivered);
    }

    @Transactional(readOnly = true)
    public List<ApplySummary> recent() {
        return repository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(ApplySummary::of)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
