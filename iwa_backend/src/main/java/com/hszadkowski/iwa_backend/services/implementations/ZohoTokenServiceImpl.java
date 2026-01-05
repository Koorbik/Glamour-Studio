package com.hszadkowski.iwa_backend.services.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hszadkowski.iwa_backend.services.interfaces.ZohoTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ZohoTokenServiceImpl implements ZohoTokenService {

    private final RestTemplate restTemplate;

    @Value("${zoho.client.id}")
    private String clientId;

    @Value("${zoho.client.secret}")
    private String clientSecret;

    @Value("${zoho.refresh.token}")
    private String refreshToken;

    @Value("${zoho.auth.url}")
    private String authUrl;

    private String accessToken;
    private long tokenExpiryTime = 0;

    @Override
    public synchronized String getAccessToken() {
        if (System.currentTimeMillis() < tokenExpiryTime && accessToken != null) {
            return accessToken;
        }
        return refreshAccessToken();
    }

    private String refreshAccessToken() {
        try {
            log.info("Refreshing Zoho Access Token...");

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("refresh_token", refreshToken);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("grant_type", "refresh_token");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            // send POST request to https://accounts.zoho.eu/oauth/v2/token
            ResponseEntity<String> response = restTemplate.postForEntity(authUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());

                this.accessToken = root.get("access_token").asText();

                // Set expiry (default 1 hour, subtract 5 mins to be safe)
                int expiresIn = root.has("expires_in") ? root.get("expires_in").asInt() : 3600;
                this.tokenExpiryTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;

                return accessToken;
            }
        } catch (Exception e) {
            log.error("Failed to refresh Zoho Token", e);
        }
        throw new RuntimeException("Unable to connect to Zoho Invoice services.");
    }
}