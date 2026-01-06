package com.hszadkowski.iwa_backend.services.interfaces;

public interface SmsService {
    void sendSms(String toPhoneNumber, String messageBody);
}
