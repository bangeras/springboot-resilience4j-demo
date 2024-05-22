package com.svb.springbootresilience4jdemo.controller;

import com.svb.springbootresilience4jdemo.services.ExternalAPICaller;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api")
public class ResilientController {

    @Autowired
    private ExternalAPICaller externalAPICaller;


    @GetMapping("/circuitbreaker-api")
    @CircuitBreaker(name = "externalApi")
    public String circuitBreakerApi() {
        return externalAPICaller.callExternalApi();
    }


    @GetMapping("/retry-api")
    @Retry(name = "externalApi", fallbackMethod = "fallbackAfterRetry")
    public String retryApi() {
        return externalAPICaller.callExternalApi();
    }

    public String fallbackAfterRetry(Exception exception) {
        return "all retries have exhausted";
    }

    @GetMapping("/timelimiter-api")
    @TimeLimiter(name = "externalApi")
    public CompletableFuture<String> timeLimiterApi() {
        return CompletableFuture.supplyAsync(externalAPICaller::callExternalApiWithDelay);
    }


    @GetMapping("/bulkhead-api")
    @Bulkhead(name="externalApi")
    public String bulkheadApi() {
        return externalAPICaller.callExternalApi();
    }


    @GetMapping("/rateLimiter-api")
    @RateLimiter(name="externalApi")
    public String rateLimiterApi() {
        return externalAPICaller.callExternalApi();
    }
}
