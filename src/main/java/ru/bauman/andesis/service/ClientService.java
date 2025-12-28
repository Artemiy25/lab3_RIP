package ru.bauman.andesis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.bauman.andesis.dto.RandomStatisticsDto;
import ru.bauman.andesis.exception.InvalidParametersException;
import ru.bauman.andesis.util.ValidationUtil;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class ClientService {

    private final WebClient webClient;

    @Value("${app.webclient.service-b-url:http://localhost:8081}")
    private String serviceBUrl;

    @Value("${app.webclient.timeout-seconds:60}")
    private long timeoutSeconds;

    public ClientService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<RandomStatisticsDto> fetchRandomStatistics(Long count, Long min, Long max, Long range) {
        return Mono.defer(() -> {
            log.info("Fetching random statistics: count={}, min={}, max={}, range={}", count, min, max, range);

            try {
                long validatedCount = ValidationUtil.getValidatedCount(count);
                long validatedMax = ValidationUtil.getValidatedMax(max);
                long validatedMin = ValidationUtil.getValidatedMin(min, validatedMax);
                long validatedRange = ValidationUtil.getValidatedRange(range, validatedMin, validatedMax);

                return performRequest(validatedCount, validatedMin, validatedMax)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .doOnSuccess(stats -> log.info("Successfully received statistics from Service B: " +
                                        "count={}, mean={}, stdDev={}",
                                stats.getCount(), stats.getMean(), stats.getStandardDeviation()))
                        .doOnError(error -> log.error("Error fetching from Service B: {}", error.getMessage()));
            } catch (InvalidParametersException e) {
                log.warn("Invalid parameters provided to client service: {}", e.getMessage());
                return Mono.error(e);
            }
        });
    }

    private Mono<RandomStatisticsDto> performRequest(long count, long min, long max) {
        String url = String.format("%s/api/random/statistics?count=%d&min=%d&max=%d",
                serviceBUrl, count, min, max);

        log.debug("Sending request to Service B: {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(RandomStatisticsDto.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(100))
                        .maxBackoff(Duration.ofSeconds(1))
                        .filter(this::isRetryable)
                        .doBeforeRetry(signal -> log.warn("Retrying request to Service B (attempt {})",
                                signal.totalRetries() + 1)))
                .onErrorMap(WebClientResponseException.class, e -> {
                    log.error("WebClient error (status {}): {}", e.getRawStatusCode(), e.getResponseBodyAsString());
                    return new RuntimeException("Service B returned error: " + e.getRawStatusCode(), e);
                })
                .onErrorMap(io.netty.handler.timeout.TimeoutException.class, e -> {
                    log.error("Request timeout while fetching from Service B");
                    return new TimeoutException("Service B request timeout");
                });
    }

    private boolean isRetryable(Throwable ex) {
        if (ex instanceof WebClientResponseException) {
            WebClientResponseException webEx = (WebClientResponseException) ex;
            return webEx.getStatusCode().is5xxServerError();
        }
        return ex instanceof io.netty.handler.timeout.TimeoutException ||
                ex instanceof java.util.concurrent.TimeoutException;
    }
}
