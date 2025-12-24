package com.example.payment.global.config;

import com.example.payment.global.security.TokenProvider;
import com.example.payment.global.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenProvider tokenProvider;
    private final TokenService tokenService;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // ✅ Basic / Form 로그인 완전 비활성화
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // ✅ CORS & OPTIONS 허용
                .cors(cors -> {})
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/api/v1/payments/**").authenticated()
                        .anyExchange().permitAll()
                )

                // JWT 필터
                .addFilterAt(jwtWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)

                // ✅ Basic 대신 그냥 401만 내려주게 엔트리포인트 설정
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(
                                new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                .build();
    }

    @Bean
    public WebFilter jwtWebFilter() {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return chain.filter(exchange); // 토큰 없으면 그냥 통과 → 위 authorize 에서 막힘
            }

            String token = authHeader.substring(7);

            String userId;
            String tokenId;
            try {
                userId = tokenProvider.validateAndGetUserId(token);
                tokenId = tokenProvider.getTokenId(token);
            } catch (Exception e) {
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT"));
            }

            Long uid = Long.parseLong(userId);

            return tokenService.validSession(uid, tokenId)
                    .flatMap(valid -> {
                        if (!valid) {
                            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired"));
                        }
                        Authentication auth = new UsernamePasswordAuthenticationToken(
                                userId, null, AuthorityUtils.NO_AUTHORITIES);

                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                    });
        };
    }
}
