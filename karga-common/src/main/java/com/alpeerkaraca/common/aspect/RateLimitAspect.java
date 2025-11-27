package com.alpeerkaraca.common.aspect;


import com.alpeerkaraca.common.annotation.RateLimit;
import com.alpeerkaraca.common.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitAspect {
    private final RedisTemplate<String, String> redisTemplate;
    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    /**
     * Checks and enforces the rate limit for a method annotated with the {@code @RateLimit} annotation.
     * If the number of requests from a single user exceeds the allowed limit within the specified time frame,
     * a {@link RateLimitExceededException} will be thrown.
     *
     * @param joinPoint the join point representing the method being intercepted
     * @param rateLimit the rate limit configuration specified by the {@code @RateLimit} annotation
     * @throws RateLimitExceededException if the rate limit is exceeded for the identified user
     */
    @Before("@annotation(rateLimit)")
    public void checkRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        if (!rateLimitEnabled) {
            return;
        }
        String identifier = getIdentifier();
        String key = "rate_limit:" + rateLimit.key() + ":" + identifier;
        long limit = rateLimit.limit();
        long windowSizeMillis = rateLimit.unit().toMillis(rateLimit.duration());
        long currentTime = System.currentTimeMillis();

        try {
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, currentTime - windowSizeMillis);

            Long currentCount = redisTemplate.opsForZSet().zCard(key);

            if (currentCount != null && currentCount >= limit) {
                log.warn("Rate limit exceeded! User: {}, Key: {}", identifier, key);
                throw new RateLimitExceededException("You've sent too many requests. Please try again later.");
            }

            redisTemplate.opsForZSet().add(key, String.valueOf(currentTime), currentTime);

            redisTemplate.expire(key, windowSizeMillis, TimeUnit.MILLISECONDS);
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Rate limit check failed:", e);
        }
    }


    /**
     * Retrieves the identifier for the current user or client. If the user is authenticated, their username is returned.
     * Otherwise, it attempts to extract the identifier from the "X-Forwarded-For" header or the remote address of the request.
     *
     * @return the identifier for the user or client, derived from the authenticated principal,
     * the "X-Forwarded-For" header, or the remote address of the request
     */
    private String getIdentifier() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }

}
