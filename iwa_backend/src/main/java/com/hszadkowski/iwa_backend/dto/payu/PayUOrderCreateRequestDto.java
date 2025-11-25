package com.hszadkowski.iwa_backend.dto.payu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PayUOrderCreateRequestDto {
    @JsonProperty("notifyUrl")
    private String notifyUrl;

    @JsonProperty("continueUrl")
    private String continueUrl;

    @JsonProperty("customerIp")
    private String customerIp;

    @JsonProperty("merchantPosId")
    private String merchantPosId;

    @JsonProperty("description")
    private String description;

    @JsonProperty("currencyCode")
    private String currencyCode;

    @JsonProperty("totalAmount")
    private Integer totalAmount;

    @JsonProperty("buyer")
    private Buyer buyer;

    @JsonProperty("products")
    private List<Product> products;

    @Data
    @Builder
    public static class Buyer {
        @JsonProperty("email")
        private String email;
        @JsonProperty("phone")
        private String phone;
        @JsonProperty("firstName")
        private String firstName;
        @JsonProperty("lastName")
        private String lastName;
        @JsonProperty("language")
        private String language;
    }

    @Data
    @Builder
    public static class Product {
        @JsonProperty("name")
        private String name;
        @JsonProperty("unitPrice")
        private Integer unitPrice;
        @JsonProperty("quantity")
        private Integer quantity;
    }
}