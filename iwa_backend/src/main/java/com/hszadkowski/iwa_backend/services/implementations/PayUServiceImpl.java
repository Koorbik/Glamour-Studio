package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.dto.payu.PayUTokenResponseDto;
import com.hszadkowski.iwa_backend.services.interfaces.PayUService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class PayUServiceImpl implements PayUService {

    private final RestTemplate restTemplate;

    @Value("${payu.base-url}")
    private String payuBaseUrl;

    @Value("${payu.oauth.client-id}")
    private String payuClientId;

    @Value("${payu.oauth.client-secret}")
    private String payuClientSecret;

    @Value("${payu.pos-id}")
    private String payuPosId;

    private PayUTokenResponseDto getAuthToken() {
        String authUrl = payuBaseUrl + "/pl/standard/user/oauth/authorize";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", payuClientId);
        body.add("client_secret", payuClientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<PayUTokenResponseDto> response = restTemplate.postForEntity(
                    authUrl,
                    request,
                    PayUTokenResponseDto.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                // TODO: create a custom exception
                throw new RuntimeException("Failed to authenticate with PayU: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during PayU authentication", e);
        }
    }
}