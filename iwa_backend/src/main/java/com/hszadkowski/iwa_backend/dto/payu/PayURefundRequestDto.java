package com.hszadkowski.iwa_backend.dto.payu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayURefundRequestDto {
    @JsonProperty("refund")
    private Refund refund;

    @Data
    @Builder
    public static class Refund {
        @JsonProperty("description")
        private String description;

        @JsonProperty("amount")
        private Integer amount;
    }
}