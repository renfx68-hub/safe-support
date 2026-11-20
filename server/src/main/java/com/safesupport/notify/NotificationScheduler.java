package com.safesupport.notify;

import com.safesupport.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每分钟检查一次到期的申请，由配置的生效日决定实际发送时间。
 */
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final ApplicationService applicationService;
    private final EmailNotifier emailNotifier;

    public NotificationScheduler(ApplicationService applicationService,
                                 EmailNotifier emailNotifier) {
        this.applicationService = applicationService;
        this.emailNotifier = emailNotifier;
    }

    @Scheduled(
            initialDelayString = "${app.notify.initial-delay-ms:30000}",
            fixedDelayString = "${app.notify.poll-interval-ms:60000}")
    public void dispatchDue() {
        if (!emailNotifier.isConfigured()) {
            log.debug("[notify] 邮件通道未配置，跳过本轮");
            return;
        }
        applicationService.dispatchDue();
    }
}
