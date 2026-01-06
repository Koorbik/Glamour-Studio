package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.services.interfaces.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async
    public void sendVerificationEmail(String to, String subject, String text) {
        try {
            sendEmail(to, subject, text);
            log.info("Email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String subject, String text) {
        try {
            sendEmail(to, subject, text);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email", e);
        }
    }

    @Override
    @Async
    public void sendEmailWithAttachment(String to, String subject, String text, String fileName, byte[] attachmentData) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);

            if (attachmentData != null && attachmentData.length > 0) {
                helper.addAttachment(fileName, new ByteArrayResource(attachmentData));
            }

            emailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send attachment email", e);
        }
    }

    private void sendEmail(String to, String subject, String text) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, true);
        emailSender.send(message);
    }
}