package com.svb.springbootresilience4jdemo.services;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ExternalAPICaller {

    @Autowired
    private RestTemplate restTemplate;


    public String callExternalApi() {
        return restTemplate.getForObject("/external-api", String.class);
    }

    public String callExternalApiWithDelay() {
        String result = restTemplate.getForObject("/external-api", String.class);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignore) {
        }
        return result;
    }

}
