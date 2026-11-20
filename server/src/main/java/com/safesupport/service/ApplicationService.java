package com.safesupport.service;

import com.safesupport.domain.ApplicationRecord;
import com.safesupport.domain.Contact;
import com.safesupport.domain.RecordStatus;
import com.safesupport.notify.DeliveryResult;
import com.safesupport.notify.EmailNotifier;
import com.safesupport.repository.ApplicationRepository;
import com.safesupport.web.ApplyRequest;
import com.safesupport.web.ApplyResponse;
import com.safesupport.web.ApplySummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository repository;
    private final EmailNotifier emailNotifier;
    private final int maxAttempts;

    public ApplicationService(ApplicationRepository repository,
                              EmailNotifier emailNotifier,
                              @Value("${app.notify.max-attempts:5}") int maxAttempts) {
        this.repository = repository;
        this.emailNotifier = emailNotifier;
        this.maxAttempts = maxAttempts;
    }

    /** 提交仅落库预约，邮件由定时任务在托付截止日当天发送 */
    @Transactional
    public ApplyResponse create(ApplyRequest request) {
        LocalDate effectiveDate = request.effectiveDate();
        if (effectiveDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("托付截止日不能早于今天");
        }

        ApplicationRecord record = new ApplicationRecord(effectiveDate, trimToNull(request.note()), Instant.now());
        request.contacts().forEach(c ->
                record.addContact(new Contact(c.name().trim(), c.phone().trim(), trimToNull(c.email()))));
        repository.save(record);

        return ApplyResponse.of(record);
    }

    /** 处理所有已到生效日、待发送的申请，失败的下个周期重试 */
    @Transactional
    public void dispatchDue() {
        List<ApplicationRecord> due = repository.findDue(RecordStatus.PENDING, LocalDate.now());
        for (ApplicationRecord record : due) {
            boolean hasFailure = dispatchRecord(record);
            record.markAttempt(Instant.now());

            if (!hasFailure) {
                record.setStatus(RecordStatus.SENT);
            } else if (record.getAttempts() >= maxAttempts) {
                record.setStatus(RecordStatus.FAILED);
                log.warn("[notify failed] 重试次数用尽：id={}, attempts={}", record.getId(), record.getAttempts());
            }
            repository.save(record);
        }
    }

    private boolean dispatchRecord(ApplicationRecord record) {
        boolean hasFailure = false;
        for (Contact contact : record.getContacts()) {
            // 已发送或已确认无需发送的家属不再重复处理
            if (DeliveryResult.Status.SENT.name().equals(contact.getStatus())
                    || DeliveryResult.Status.SKIPPED.name().equals(contact.getStatus())) {
                continue;
            }
            DeliveryResult result = emailNotifier.send(
                    contact.getName(),
                    contact.getEmail(),
                    applyDate(record),
                    record.getEffectiveDate());
            contact.setTarget(result.target());
            contact.setStatus(result.status().name());
            contact.setMessage(result.message());
            if (result.status() == DeliveryResult.Status.FAILED) {
                hasFailure = true;
            }
        }
        return hasFailure;
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

    /** 申请提交日期（按服务器本地时区） */
    private LocalDate applyDate(ApplicationRecord record) {
        return record.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
