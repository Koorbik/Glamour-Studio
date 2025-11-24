package com.hszadkowski.iwa_backend.services.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hszadkowski.iwa_backend.dto.payment.PaymentInitiationDto;
import com.hszadkowski.iwa_backend.dto.payu.PayUOrderCreateRequestDto;
import com.hszadkowski.iwa_backend.dto.payu.PayUOrderResponseDto;
import com.hszadkowski.iwa_backend.dto.payu.PayUTokenResponseDto;
import com.hszadkowski.iwa_backend.exceptions.AppointmentNotFoundException;
import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.Payment;
import com.hszadkowski.iwa_backend.repos.AppointmentRepository;
import com.hszadkowski.iwa_backend.repos.PaymentRepository;
import com.hszadkowski.iwa_backend.services.interfaces.PayUService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayUServiceImpl implements PayUService {

    private final RestTemplate restTemplate;
    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;

    @Value("${payu.base-url}")
    private String payuBaseUrl;
    @Value("${payu.oauth.client-id}")
    private String payuClientId;
    @Value("${payu.oauth.client-secret}")
    private String payuClientSecret;
    @Value("${payu.pos-id}")
    private String payuPosId;
    @Value("${payu.notify-url}")
    private String notifyUrl;
    @Value("${payu.continue-url}")
    private String continueUrl;

    @Value("${payu.second-key-md5}")
    private String secondKeyMd5;

    @Override
    @Transactional
    public PaymentInitiationDto createOrder(Integer appointmentId, String clientIp) {

        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow(() -> new AppointmentNotFoundException("Appointment not found"));

        PayUTokenResponseDto token = getAuthToken();

        BigDecimal price = appointment.getService().getPrice();
        int totalAmount = price.multiply(new BigDecimal(100)).intValue();
        String description = appointment.getService().getName();

        java.util.Optional<Payment> existingPayment = paymentRepository.findByAppointment(appointment);
        Payment payment;
        if (existingPayment.isPresent()) {
            payment = existingPayment.get();
        } else {
            payment = new Payment();
            payment.setAppointment(appointment);
            payment.setAppUser(appointment.getAppUser());
            payment.setAmount(price);
        }

        payment.setStatus("PENDING");
        paymentRepository.save(payment);

        PayUOrderCreateRequestDto requestDto = PayUOrderCreateRequestDto.builder().notifyUrl(notifyUrl).continueUrl(continueUrl).customerIp(clientIp).merchantPosId(payuPosId).description(description).currencyCode("PLN").totalAmount(totalAmount).products(Collections.singletonList(PayUOrderCreateRequestDto.Product.builder().name(description).unitPrice(totalAmount).quantity(1).build())).buyer(PayUOrderCreateRequestDto.Buyer.builder().email(appointment.getAppUser().getEmail()).firstName(appointment.getAppUser().getName()).lastName(appointment.getAppUser().getSurname()).language("pl").build()).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token.getAccessToken());
        HttpEntity<PayUOrderCreateRequestDto> request = new HttpEntity<>(requestDto, headers);

        try {
            ResponseEntity<PayUOrderResponseDto> response = restTemplate.postForEntity(payuBaseUrl + "/api/v2_1/orders", request, PayUOrderResponseDto.class);

            PayUOrderResponseDto responseBody = response.getBody();
            boolean isSuccessStatus = response.getStatusCode().is2xxSuccessful() || response.getStatusCode().value() == 302;

            if (isSuccessStatus && responseBody != null && responseBody.getStatus().getStatusCode().equals("SUCCESS")) {

                payment.setTransactionId(responseBody.getOrderId());
                paymentRepository.save(payment);

                return new PaymentInitiationDto(responseBody.getRedirectUri(), responseBody.getOrderId());
            } else {
                throw new RuntimeException("PayU Error: " + (responseBody != null ? responseBody.getStatus().getStatusCode() : "Unknown"));
            }
        } catch (Exception e) {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            throw new RuntimeException("Failed to initiate PayU order", e);
        }
    }

    @Override
    @Transactional
    public void handleNotification(String payload, String signatureHeader) {
        // 1. Verify Signature
        verifySignature(payload, signatureHeader);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(payload);

            String orderId = root.path("order").path("orderId").asText();
            String status = root.path("order").path("status").asText();

            Payment payment = paymentRepository.findByTransactionId(orderId)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));

            log.info("Updating payment {} status to {}", orderId, status);

            if ("COMPLETED".equals(status)) {
                payment.setStatus("COMPLETED");
                payment.setPaidAt(java.time.LocalDateTime.now());
                paymentRepository.save(payment);
            } else if ("CANCELED".equals(status)) {
                payment.setStatus("CANCELED");
                paymentRepository.save(payment);
            } else if ("PENDING".equals(status)) {
                payment.setStatus("PENDING");
                paymentRepository.save(payment);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error handling notification", e);
        }
    }

    private void verifySignature(String payload, String signatureHeader) {
        if (signatureHeader == null) {
            throw new SecurityException("Missing OpenPayU-Signature header");
        }

        String incomingSignature = null;
        String[] parts = signatureHeader.split(";");
        for (String part : parts) {
            String[] kv = part.trim().split("=");
            if (kv.length == 2 && "signature".equalsIgnoreCase(kv[0])) {
                incomingSignature = kv[1];
            }
        }

        if (incomingSignature == null) {
            throw new SecurityException("Signature not found in header");
        }

        String dataToHash = payload + secondKeyMd5;
        String expectedSignature = DigestUtils.md5DigestAsHex(dataToHash.getBytes());

        if (!incomingSignature.equalsIgnoreCase(expectedSignature)) {
                        log.error("Signature Mismatch detected during PayU notification verification.");
            throw new SecurityException("Invalid PayU Signature");
        }
    }

    private PayUTokenResponseDto getAuthToken() {
        String authUrl = payuBaseUrl + "/pl/standard/user/oauth/authorize";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", payuClientId);
        body.add("client_secret", payuClientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<PayUTokenResponseDto> response = restTemplate.postForEntity(authUrl, request, PayUTokenResponseDto.class);
        if (response.getBody() == null) throw new RuntimeException("Auth failed");
        return response.getBody();
    }
}