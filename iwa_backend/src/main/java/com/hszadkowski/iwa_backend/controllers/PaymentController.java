package com.hszadkowski.iwa_backend.controllers;

import com.hszadkowski.iwa_backend.dto.ApiResponse;
import com.hszadkowski.iwa_backend.dto.payment.PaymentInitiationDto;
import com.hszadkowski.iwa_backend.services.interfaces.PayUService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PayUService payUService;

    @PostMapping("/create-order/{appointmentId}")
    @PreAuthorize("isAuthenticated()")
    // later check if the logged in user owns the appointment they are trying to pay for
    public ResponseEntity<ApiResponse<PaymentInitiationDto>> createOrder(
            @PathVariable Integer appointmentId,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        PaymentInitiationDto responseDto = payUService.createOrder(appointmentId, clientIp);

        return ResponseEntity.ok(ApiResponse.success("Order created", responseDto));
    }

    @PostMapping("/notify")
    public ResponseEntity<Void> handleNotification(@RequestBody String payload,
                                                   @RequestHeader(value = "OpenPayU-Signature", required = false) String signature) {
        payUService.handleNotification(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pay-at-studio/{appointmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> payAtStudio(@PathVariable Integer appointmentId) {
        payUService.initiateCashPayment(appointmentId);
        return ResponseEntity.ok(ApiResponse.success("Cash payment selected", null));
    }

    @PostMapping("/{paymentId}/confirm-cash")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> confirmCashPayment(@PathVariable Integer paymentId) {
        payUService.finalizePayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed and invoice sent", null));
    }
}