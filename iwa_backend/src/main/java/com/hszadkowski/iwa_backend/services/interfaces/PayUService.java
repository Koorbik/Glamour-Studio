package com.hszadkowski.iwa_backend.services.interfaces;


import com.hszadkowski.iwa_backend.dto.payment.PaymentInitiationDto;

import java.math.BigDecimal;

public interface PayUService {

    PaymentInitiationDto createOrder(Integer appointmentId, String clientIp);
    void handleNotification(String payload, String signatureHeader);

    void refundTransaction(String orderId, BigDecimal amount, String description);
}