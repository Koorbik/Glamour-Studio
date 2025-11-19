package com.hszadkowski.iwa_backend.dto.payu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PayUOrderResponseDto {
    @JsonProperty("status")
    private Status status;

    @JsonProperty("redirectUri")
    private String redirectUri;

    @JsonProperty("orderId")
    private String orderId;

    @Data
    @NoArgsConstructor
    public static class Status {
        @JsonProperty("statusCode")
        private String statusCode;
    }
}