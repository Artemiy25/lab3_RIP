package ru.bauman.andesis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.bauman.andesis.dto.RandomStatisticsDto;
import ru.bauman.andesis.util.RandomStatsAggregator;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Оптимизированный генератор случайных чисел и вычислитель статистики.
 *
 * ОПТИМИЗАЦИИ:
 * 1. ThreadLocalRandom.current() вместо new Random() - устранена синхронизация
 * 2. Single-pass алгоритм - все метрики за один проход вместо 6
 * 3. Streaming без хранения - не храним все числа в памяти
 * 4. Примитивные типы - прямое преобразование long → double без String
 */
@Slf4j
@Service
public class RandomNumberGenerator {

    /**
     * Генерирует статистику случайных чисел за один проход (оптимизированная версия).
     *
     * ОПТИМИЗАЦИИ:
     * - ThreadLocalRandom вместо new Random()
     * - Streaming генерация без ArrayList<Long>
     * - Single-pass через RandomStatsAggregator
     * - Нет множественных проходов по данным
     *
     * @param count количество чисел для генерации
     * @param min минимальное значение диапазона
     * @param max максимальное значение диапазона
     * @return статистика
     */
    public Mono<RandomStatisticsDto> generateStatistics(long count, long min, long max) {
        return Mono.fromCallable(() -> {
            log.debug("Starting optimized generation of {} random numbers in range [{}, {}]", count, min, max);
            long startTime = System.currentTimeMillis();

            // ОПТИМИЗАЦИЯ 1: Создаём агрегатор для single-pass вычислений
            RandomStatsAggregator aggregator = new RandomStatsAggregator(min, max);

            // ОПТИМИЗАЦИЯ 2: ThreadLocalRandom - нет синхронизации, нет аллокаций
            ThreadLocalRandom random = ThreadLocalRandom.current();

            // ОПТИМИЗАЦИЯ 3: Single-pass - генерируем и агрегируем за один цикл
            // Не храним числа в памяти - только статистика
            for (long i = 0; i < count; i++) {
                // Генерация случайного числа в диапазоне [min, max)
                long value = random.nextLong(min, max + 1);

                // Добавляем к агрегатору - обновляет все метрики за O(1)
                aggregator.add(value);
            }

            // Вычисляем финальные метрики
            long actualMin = aggregator.getMin();
            long actualMax = aggregator.getMax();
            double mean = aggregator.getMean();
            double stdDev = aggregator.getStandardDeviation();
            Map<String, Long> histogram = aggregator.getHistogramMap();

            long processingTime = System.currentTimeMillis() - startTime;

            log.info("Generated {} numbers in {}ms (optimized): min={}, max={}, mean={}, stdDev={}",
                    count, processingTime, actualMin, actualMax, mean, stdDev);

            aggregator.logStatistics();

            // Построение результата
            return RandomStatisticsDto.builder()
                    .count(count)
                    .min(actualMin)
                    .max(actualMax)
                    .mean(mean)
                    .standardDeviation(stdDev)
                    .histogram(histogram)
                    .generatedAt(System.currentTimeMillis())
                    .processingTimeMs(processingTime)
                    .build();

        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Старый метод для обратной совместимости (DEPRECATED).
     * Теперь использует оптимизированную реализацию внутри.
     *
     * @deprecated Используйте generateStatistics() напрямую
     */
    @Deprecated
    public Mono<RandomStatisticsDto> generateRandomNumbersWithStatistics(long count, long min, long max) {
        log.warn("DEPRECATED: generateRandomNumbersWithStatistics() вызван, используйте generateStatistics()");
        return generateStatistics(count, min, max);
    }
}
