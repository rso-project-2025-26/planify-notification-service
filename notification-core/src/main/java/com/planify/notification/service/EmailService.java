package com.planify.notification.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "notification.email", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmailService {

    private final SendGrid sendGridClient;
    private final String fromEmail;
    private final String fromName;

    public EmailService(
            @Value("${sendgrid.api-key}") String apiKey,
            @Value("${sendgrid.from-email}") String fromEmail,
            @Value("${sendgrid.from-name}") String fromName) {
        this.sendGridClient = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Retry(name = "emailService")
    public String sendEmail(String toEmail, String subject, String htmlBody) throws IOException {
        Email from = new Email(fromEmail, fromName);
        Email to = new Email(toEmail);
        Content content = new Content("text/html", htmlBody);
        Mail mail = new Mail(from, subject, to, content);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sendGridClient.api(request);

        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            log.info("Email sent successfully to {} with status code {}", toEmail, response.getStatusCode());
            String messageId = response.getHeaders().get("X-Message-Id");
            return messageId != null ? messageId : "sent-" + System.currentTimeMillis();
        } else {
            log.error("Failed to send email to {}. Status: {}, Body: {}",
                    toEmail, response.getStatusCode(), response.getBody());
            throw new IOException("Failed to send email: " + response.getBody());
        }
    }

    public String sendTemplatedEmail(String toEmail, String subject, String htmlTemplate) throws IOException {
        return sendEmail(toEmail, subject, htmlTemplate);
    }
}
