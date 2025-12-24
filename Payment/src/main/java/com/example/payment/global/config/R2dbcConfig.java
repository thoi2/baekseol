package com.example.payment.global.config;// 예: com.example.payment.config.R2dbcConfig

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@Configuration
@EnableR2dbcAuditing   // ✅ 이거 필수
public class R2dbcConfig {
}
