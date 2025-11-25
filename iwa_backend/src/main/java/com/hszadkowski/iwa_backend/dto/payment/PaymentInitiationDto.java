package com.hszadkowski.iwa_backend.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInitiationDto {
    private String redirectUri;
    private String orderId;
}