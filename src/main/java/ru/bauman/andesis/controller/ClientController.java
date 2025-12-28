package ru.bauman.andesis.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.bauman.andesis.dto.RandomStatisticsDto;
import ru.bauman.andesis.service.ClientService;

@Slf4j
@RestController
@RequestMapping("/api/client")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/random-stats")
    public Mono<ResponseEntity<RandomStatisticsDto>> getRandomStats(
            @RequestParam(name = "count", required = false) Long count,
            @RequestParam(name = "min", required = false) Long min,
            @RequestParam(name = "max", required = false) Long max,
            @RequestParam(name = "range", required = false) Long range) {

        log.debug("ClientController: GET /api/client/random-stats with params: count={}, min={}, max={}, range={}",
                count, min, max, range);

        return clientService.fetchRandomStatistics(count, min, max, range)
                .map(stats -> {
                    log.info("Returning statistics to client: count={}, mean={}", stats.getCount(), stats.getMean());
                    return ResponseEntity.ok(stats);
                })
                .onErrorResume(e -> {
                    log.error("Error in getRandomStats: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}
