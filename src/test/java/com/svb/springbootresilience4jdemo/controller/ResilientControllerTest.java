package com.svb.springbootresilience4jdemo.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ResilientControllerTest {


    @RegisterExtension
    static WireMockExtension EXTERNAL_SERVICE = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().port(8091))
            .build();
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    /**
     * https://arnoldgalovics.com/resilience4j-resttemplate/
     * https://www.baeldung.com/spring-boot-resilience4j
     */
    public void testExternalApiWithCircuitBreaker() throws Exception {
        EXTERNAL_SERVICE.resetRequests();

        EXTERNAL_SERVICE.stubFor(get("/external-api").willReturn(serverError()));

        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity("/api/circuitbreaker-api", String.class);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity("/api/circuitbreaker-api", String.class);
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        }

        EXTERNAL_SERVICE.verify(5, getRequestedFor(urlEqualTo("/external-api")));


    }


    @Test
    public void testExternalApiRetry() {
        EXTERNAL_SERVICE.stubFor(WireMock.get("/external-api").willReturn(ok()));

        ResponseEntity<String> response1 = restTemplate.getForEntity("/api/retry-api", String.class);
        EXTERNAL_SERVICE.verify(1, getRequestedFor(urlEqualTo("/external-api")));

        EXTERNAL_SERVICE.resetRequests();

        EXTERNAL_SERVICE.stubFor(WireMock.get("/external-api").willReturn(serverError()));
        ResponseEntity<String> response2 = restTemplate.getForEntity("/api/retry-api", String.class);
        assertEquals(response2.getBody(), "all retries have exhausted");
        EXTERNAL_SERVICE.verify(3, getRequestedFor(urlEqualTo("/external-api")));
    }


    @Test
    public void testTimeLimiter() {
        EXTERNAL_SERVICE.stubFor(WireMock.get("/external-api").willReturn(ok()));
        ResponseEntity<String> response = restTemplate.getForEntity("/api/timelimiter-api", String.class);

        assertEquals(HttpStatus.REQUEST_TIMEOUT, response.getStatusCode());
        EXTERNAL_SERVICE.verify(1, getRequestedFor(urlEqualTo("/external-api")));
    }


    @Test
    public void testBulkhead() throws InterruptedException {
        EXTERNAL_SERVICE.stubFor(WireMock.get("/external-api").willReturn(ok()));
        Map<Integer, Integer> responseStatusCount = new ConcurrentHashMap<>();

        IntStream.rangeClosed(1, 5)
                .parallel()
                .forEach(i -> {
                    ResponseEntity<String> response = restTemplate.getForEntity("/api/bulkhead-api", String.class);
                    int statusCode = response.getStatusCodeValue();
                    responseStatusCount.put(statusCode, responseStatusCount.getOrDefault(statusCode, 0) + 1);
                });

        assertEquals(2, responseStatusCount.keySet().size());
        assertTrue(responseStatusCount.containsKey(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED.value()));
        assertTrue(responseStatusCount.containsKey(HttpStatus.OK.value()));
        EXTERNAL_SERVICE.verify(3, getRequestedFor(urlEqualTo("/external-api")));
    }


    @Test
    public void testRatelimiter() {
        EXTERNAL_SERVICE.stubFor(WireMock.get("/external-api").willReturn(ok()));

        Map<Integer, Integer> responseStatusCount = new ConcurrentHashMap<>();

        IntStream.rangeClosed(1, 50)
                .parallel()
                .forEach(i -> {
                    ResponseEntity<String> response = restTemplate.getForEntity("/api/rateLimiter-api", String.class);
                    int statusCode = response.getStatusCodeValue();
                    responseStatusCount.put(statusCode, responseStatusCount.getOrDefault(statusCode, 0) + 1);
                });

        assertEquals(2, responseStatusCount.keySet().size());
        assertTrue(responseStatusCount.containsKey(HttpStatus.TOO_MANY_REQUESTS.value()));
        assertTrue(responseStatusCount.containsKey(HttpStatus.OK.value()));

        EXTERNAL_SERVICE.verify(5, getRequestedFor(urlEqualTo("/external-api")));
    }
}
