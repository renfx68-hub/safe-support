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

@Component
public class EmailNotifier {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日");

    private final JavaMailSender mailSender;
    private final String username;
    private final String password;
    private final String from;
    private final String testRecipient;

    public EmailNotifier(JavaMailSender mailSender,
                         @Value("${spring.mail.username:}") String username,
                         @Value("${spring.mail.password:}") String password,
                         @Value("${app.mail.from:}") String from,
                         @Value("${app.mail.test-recipient:}") String testRecipient) {
        this.mailSender = mailSender;
        this.username = username;
        this.password = password;
        this.from = from;
        this.testRecipient = testRecipient;
    }

    /**
     * 优先发送到家属邮箱；未填写时回退到配置的测试邮箱。
     * 未配置 SMTP 账号时只记录日志，不阻塞申请落库。
     */
    public DeliveryResult send(String contactName, String email, LocalDate effectiveDate) {
        String target = isBlank(email) ? trimToNull(testRecipient) : email.trim();

        if (isBlank(username) || isBlank(password)) {
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
            helper.setSubject("【平安托付】状态申请提醒");
            helper.setText(buildBody(contactName, effectiveDate), false);
            mailSender.send(message);
            log.info("[mail sent] contact={}, target={}", contactName, target);
            return DeliveryResult.sent(target);
        } catch (Exception e) {
            log.warn("[mail failed] contact={}, target={}", contactName, target, e);
            return DeliveryResult.failed(target, "邮件发送失败：" + e.getMessage());
        }
    }

    private String buildBody(String contactName, LocalDate effectiveDate) {
        return "您好，" + contactName + "：\n\n"
                + "您的家人已于 " + effectiveDate.format(DATE_FORMAT) + " 提交状态申请，请及时关注并与其联系。\n\n"
                + "—— 平安托付";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
