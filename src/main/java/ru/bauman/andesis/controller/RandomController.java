package ru.bauman.andesis.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.bauman.andesis.dto.RandomStatisticsDto;
import ru.bauman.andesis.service.StatisticsCalculator;

@Slf4j
@RestController
@RequestMapping("/api/random")
public class RandomController {

    private final StatisticsCalculator statisticsCalculator;

    public RandomController(StatisticsCalculator statisticsCalculator) {
        this.statisticsCalculator = statisticsCalculator;
    }

    @GetMapping("/statistics")
    public Mono<ResponseEntity<RandomStatisticsDto>> getStatistics(
            @RequestParam(name = "count", required = false, defaultValue = "1000") Long count,
            @RequestParam(name = "min", required = false, defaultValue = "-1000000") Long min,
            @RequestParam(name = "max", required = false, defaultValue = "1000000") Long max,
            @RequestParam(name = "range", required = false) Long range) {

        log.debug("RandomController: GET /api/random/statistics with params: count={}, min={}, max={}, range={}",
                count, min, max, range);

        long actualMax = max != null ? max : 1_000_000;
        long actualMin = min != null ? min : (actualMax - 1_000_000);

        return statisticsCalculator.calculateStatistics(count, actualMin, actualMax)
                .map(stats -> {
                    log.info("Returning statistics: count={}, min={}, max={}, mean={}, stdDev={}",
                            stats.getCount(), stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation());
                    return ResponseEntity.ok(stats);
                })
                .doOnError(e -> log.error("Error calculating statistics: {}", e.getMessage(), e));
    }
}
