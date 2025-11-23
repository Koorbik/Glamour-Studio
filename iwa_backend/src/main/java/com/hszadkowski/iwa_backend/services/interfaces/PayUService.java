package com.hszadkowski.iwa_backend.services.interfaces;


import com.hszadkowski.iwa_backend.dto.payment.PaymentInitiationDto;

public interface PayUService {

    PaymentInitiationDto createOrder(Integer appointmentId, String clientIp);
    void handleNotification(String payload);
}