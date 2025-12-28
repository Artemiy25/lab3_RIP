package ru.bauman.andesis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.bauman.andesis.dto.RandomStatisticsDto;
import ru.bauman.andesis.util.ValidationUtil;

/**
 * Упрощённый калькулятор статистики.
 *
 * ОПТИМИЗАЦИЯ: Теперь просто делегирует работу оптимизированному RandomNumberGenerator,
 * вместо сложных цепочек flatMap с множественными проходами.
 */
@Slf4j
@Service
public class StatisticsCalculator {

    private final RandomNumberGenerator numberGenerator;

    public StatisticsCalculator(RandomNumberGenerator numberGenerator) {
        this.numberGenerator = numberGenerator;
    }

    /**
     * Вычисляет статистику случайных чисел (оптимизированная версия).
     *
     * ОПТИМИЗАЦИЯ: Вместо callback hell с множественными flatMap,
     * просто вызываем оптимизированный метод generateStatistics.
     *
     * @param count количество чисел
     * @param min минимальное значение
     * @param max максимальное значение
     * @return статистика
     */
    public Mono<RandomStatisticsDto> calculateStatistics(long count, long min, long max) {
        return Mono.defer(() -> {
            log.info("Starting optimized statistics calculation for count={}, range=[{}, {}]", count, min, max);

            // Валидация параметров
            ValidationUtil.validateCount(count);
            ValidationUtil.validateRange(min, max);

            // ОПТИМИЗАЦИЯ: Прямой вызов оптимизированного метода
            // Вместо:
            // - generateRandomNumbers (создание ArrayList)
            // - calculateMean (проход 1)
            // - calculateStandardDeviation (проход 2)
            // - findMin (проход 3)
            // - findMax (проход 4)
            // - generateHistogram (проход 5 + сортировка)
            // Теперь: generateStatistics (один проход, без ArrayList)

            return numberGenerator.generateStatistics(count, min, max)
                    .doOnSuccess(dto -> log.info("Optimized statistics calculation completed: count={}, mean={}, stdDev={}, time={}ms",
                            dto.getCount(), dto.getMean(), dto.getStandardDeviation(), dto.getProcessingTimeMs()))
                    .doOnError(e -> log.error("Error during statistics calculation", e));
        });
    }
}
