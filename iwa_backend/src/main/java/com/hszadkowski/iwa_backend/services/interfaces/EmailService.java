package com.hszadkowski.iwa_backend.services.interfaces;

import jakarta.mail.MessagingException;

public interface EmailService {

    void sendVerificationEmail(String to, String subject, String text) throws MessagingException;

    void sendPasswordResetEmail(String to, String subject, String text) throws MessagingException;
    void sendEmailWithAttachment(String to, String subject, String text, String fileName, byte[] attachmentData);
}
