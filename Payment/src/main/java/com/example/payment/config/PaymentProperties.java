package com.example.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "toss")
@Getter
@Setter
public class PaymentProperties {

    private String secretKey;    // ${TOSS_SECRET}
    private String securityKey;  // ${TOSS_SECURITY}
    private String baseUrl;      // https://api.tosspayments.com

    public String getConfirmEndpoint() {
        return baseUrl + "/v1/payments/confirm";
    }

    public String getPayoutEndpoint() {
        return baseUrl + "/v2/payouts";
    }
}
