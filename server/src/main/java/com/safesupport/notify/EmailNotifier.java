package com.safesupport.notify;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class EmailNotifier {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日");

    private final JavaMailSender mailSender;
    private final String username;
    private final String password;
    private final String from;
    private final List<String> testRecipients;

    public EmailNotifier(JavaMailSender mailSender,
                         @Value("${spring.mail.username:}") String username,
                         @Value("${spring.mail.password:}") String password,
                         @Value("${app.mail.from:}") String from,
                         @Value("${app.mail.test-recipients:}") String testRecipients) {
        this.mailSender = mailSender;
        this.username = username;
        this.password = password;
        this.from = from;
        this.testRecipients = splitRecipients(testRecipients);
    }

    /**
     * 优先发送到家属邮箱；未填写时从测试邮箱列表中随机选一个。
     * 未配置 SMTP 账号时只记录日志，不阻塞申请落库。
     */
    public DeliveryResult send(String contactName, String email, LocalDate appliedDate, LocalDate effectiveDate) {
        String target = isBlank(email) ? randomTestRecipient() : email.trim();

        if (!isConfigured()) {
            log.info("[mail skipped] 邮件通道未配置，跳过发送：contact={}, target={}", contactName, target);
            return DeliveryResult.skipped(target, "邮件通道未配置（请设置 MAIL_USERNAME / MAIL_PASSWORD）");
        }
        if (target == null) {
            log.info("[mail skipped] 未填写邮箱且未配置测试收件人：contact={}", contactName);
            return DeliveryResult.skipped(null, "未填写邮箱且未配置测试收件人");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(isBlank(from) ? username : from);
            helper.setTo(target);
            helper.setSubject("【平安托付】托付截止提醒");
            helper.setText(buildBody(contactName, appliedDate, effectiveDate), false);
            mailSender.send(message);
            log.info("[mail sent] contact={}, target={}", contactName, target);
            return DeliveryResult.sent(target);
        } catch (Exception e) {
            log.warn("[mail failed] contact={}, target={}", contactName, target, e);
            return DeliveryResult.failed(target, "邮件发送失败：" + e.getMessage());
        }
    }

    public boolean isConfigured() {
        return !isBlank(username) && !isBlank(password);
    }

    private String buildBody(String contactName, LocalDate appliedDate, LocalDate effectiveDate) {
        return "您好，" + contactName + "：\n\n"
                + "您的家人已于 " + appliedDate.format(DATE_FORMAT) + " 提交托付申请，"
                + "托付截止日为 " + effectiveDate.format(DATE_FORMAT) + "。\n"
                + "请及时关注并与其联系。\n\n"
                + "—— 平安托付";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<String> splitRecipients(String value) {
        if (isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    /** 家属未填邮箱时，从测试邮箱里随机挑一个 */
    private String randomTestRecipient() {
        if (testRecipients.isEmpty()) {
            return null;
        }
        return testRecipients.get(ThreadLocalRandom.current().nextInt(testRecipients.size()));
    }
}
