package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.services.interfaces.SmsService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwilioSmsServiceImpl implements SmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    @PostConstruct
    public void init() {
        try {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized successfully with Account SID: {}", accountSid);
        } catch (Exception e) {
            log.error("Failed to initialize Twilio: {}", e.getMessage());
        }
    }

    @Override
    @Async
    public void sendSms(String toPhoneNumber, String messageBody) {
        try {
            if (toPhoneNumber == null || toPhoneNumber.trim().isEmpty()) {
                log.warn("Cannot send SMS: Phone number is empty");
                return;
            }

            if (!toPhoneNumber.startsWith("+")) {
                toPhoneNumber = "+48" + toPhoneNumber;
            }

            log.info("Sending SMS to {}", toPhoneNumber);

            Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    messageBody
            ).create();

            log.info("SMS sent successfully to {}", toPhoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", toPhoneNumber, e.getMessage());
        }
    }
}