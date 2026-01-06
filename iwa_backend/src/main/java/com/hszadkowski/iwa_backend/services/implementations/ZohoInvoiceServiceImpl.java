package com.hszadkowski.iwa_backend.services.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hszadkowski.iwa_backend.models.Payment;
import com.hszadkowski.iwa_backend.services.interfaces.ZohoInvoiceService;
import com.hszadkowski.iwa_backend.services.interfaces.ZohoTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZohoInvoiceServiceImpl implements ZohoInvoiceService {

    private final RestTemplate restTemplate;
    private final ZohoTokenService tokenService;

    @Value("${zoho.api.base.url}")
    private String baseUrl;
    @Value("${zoho.organization.id}")
    private String organizationId;
    @Value("${zoho.generic.customer.id}")
    private String genericCustomerId;

    @Override
    public byte[] createAndDownloadInvoice(Payment payment) {
        try {
            String token = tokenService.getAccessToken();

            Map<String, Object> invoiceMap = new HashMap<>();

            invoiceMap.put("customer_id", genericCustomerId);

            invoiceMap.put("date", LocalDate.now().toString());
            invoiceMap.put("status", "paid");

            // 2. Override the Address to show the REAL User's Name on PDF
            // The "attention" field appears at the top of the Billing Address on the PDF
            Map<String, String> billingAddress = new HashMap<>();
            billingAddress.put("attention", payment.getAppUser().getName() + " " + payment.getAppUser().getSurname());
            billingAddress.put("address", "Email: " + payment.getAppUser().getEmail());

            invoiceMap.put("billing_address", billingAddress);

            // 3. Line Items
            Map<String, Object> item = new HashMap<>();
            item.put("name", payment.getAppointment().getService().getName());
            item.put("rate", payment.getAmount());
            item.put("quantity", 1);
            invoiceMap.put("line_items", List.of(item));

            // Standard Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 4. Send Request
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(invoiceMap, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/invoices?organization_id=" + organizationId,
                    entity,
                    String.class
            );

            // 5. Get Invoice ID (for download only)
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            String invoiceId = root.get("invoice").get("invoice_id").asText();

            // Save this ID locally just for record keeping (it's the Document ID, not Customer ID)
            payment.setInvoiceId(Long.valueOf(invoiceId));

            // 6. Download PDF
            String pdfUrl = baseUrl + "/invoices/" + invoiceId + "?accept=pdf&organization_id=" + organizationId;
            headers.setAccept(List.of(MediaType.APPLICATION_PDF));

            return restTemplate.exchange(pdfUrl, HttpMethod.GET, new HttpEntity<>(headers), byte[].class).getBody();

        } catch (Exception e) {
            log.error("Error creating Zoho Invoice", e);
            return null;
        }
    }
}