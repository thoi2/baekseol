package com.example.payment.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final ReactiveStringRedisTemplate redisTemplate;

    // 기존 MVC와 키 규칙을 맞춘다고 가정 (RT:, AS:, LLT: ...)
    public Mono<Void> saveRefreshToken(Long userId, String token) {
        return redisTemplate.opsForValue()
                .set("RT:" + userId, token, Duration.ofDays(7))
                .then();
    }

    public Mono<String> getRefreshToken(Long userId) {
        return redisTemplate.opsForValue()
                .get("RT:" + userId);
    }

    public Mono<Boolean> deleteRefreshToken(Long userId) {
        return redisTemplate.opsForValue()
                .delete("RT:" + userId);
    }

    public Mono<Void> saveActiveSession(Long userId, String tokenId) {
        return redisTemplate.opsForValue()
                .set("AS:" + userId, tokenId, Duration.ofMinutes(10))
                .then();
    }

    public Mono<String> getActiveSession(Long userId) {
        return redisTemplate.opsForValue()
                .get("AS:" + userId);
    }

    public Mono<Boolean> deleteActiveSession(Long userId) {
        return redisTemplate.opsForValue()
                .delete("AS:" + userId);
    }

    public Mono<Boolean> validSession(Long userId, String tokenId) {
        return getActiveSession(userId)
                .map(saved -> saved != null && saved.equals(tokenId))
                .defaultIfEmpty(false);
    }
}
