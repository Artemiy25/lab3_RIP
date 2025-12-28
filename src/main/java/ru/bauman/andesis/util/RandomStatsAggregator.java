package ru.bauman.andesis.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Агрегатор для вычисления статистики случайных чисел за один проход (single-pass).
 * Эффективно вычисляет min, max, mean, stdDev, histogram без необходимости хранить все числа в памяти.
 *
 * ОПТИМИЗАЦИЯ: Вместо 6 отдельных проходов по данным, все метрики вычисляются за один проход.
 */
@Slf4j
@Getter
public class RandomStatsAggregator {

    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;
    private long sum = 0;
    private long count = 0;
    private double sumSquares = 0.0; // Для вычисления дисперсии

    // Параметры гистограммы
    private final long rangeMin;
    private final long rangeMax;
    private final int bucketCount;
    private final long bucketSize;
    private final int[] histogram;

    /**
     * Создает агрегатор для указанного диапазона значений
     *
     * @param rangeMin минимальное значение диапазона
     * @param rangeMax максимальное значение диапазона
     */
    public RandomStatsAggregator(long rangeMin, long rangeMax) {
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;

        // Количество bucket'ов для гистограммы
        long range = rangeMax - rangeMin;
        this.bucketCount = Math.min(20, (int) Math.max(10, Math.sqrt(range / 1000)));
        this.bucketSize = (range + bucketCount - 1) / bucketCount;
        this.histogram = new int[bucketCount];

        log.debug("Created RandomStatsAggregator: range=[{}, {}], buckets={}, bucketSize={}",
                rangeMin, rangeMax, bucketCount, bucketSize);
    }

    /**
     * Добавляет одно значение к агрегату (single-pass обновление).
     * ОПТИМИЗАЦИЯ: Все метрики обновляются за один вызов - O(1).
     *
     * @param value значение для добавления
     */
    public void add(long value) {
        // Обновление min/max
        if (value < min) {
            min = value;
        }
        if (value > max) {
            max = value;
        }

        // Обновление суммы для среднего
        sum += value;
        count++;

        // Обновление суммы квадратов для стандартного отклонения
        // ИСПРАВЛЕНО: Используем прямое преобразование вместо Double.parseDouble(String.valueOf())
        double doubleValue = (double) value;
        sumSquares += doubleValue * doubleValue;

        // Обновление гистограммы
        int bucketIndex = calculateBucketIndex(value);
        if (bucketIndex >= 0 && bucketIndex < bucketCount) {
            histogram[bucketIndex]++;
        }
    }

    /**
     * Вычисляет индекс bucket'а для значения
     *
     * @param value значение
     * @return индекс bucket'а
     */
    private int calculateBucketIndex(long value) {
        if (value < rangeMin) {
            return 0;
        }
        if (value > rangeMax) {
            return bucketCount - 1;
        }

        long offset = value - rangeMin;
        int index = (int) (offset / bucketSize);

        // Гарантируем, что индекс в пределах массива
        return Math.min(index, bucketCount - 1);
    }

    /**
     * Возвращает среднее значение
     *
     * @return среднее значение
     */
    public double getMean() {
        if (count == 0) {
            return 0.0;
        }
        return (double) sum / count;
    }

    /**
     * Возвращает стандартное отклонение
     * Использует формулу: σ = √(E[X²] - (E[X])²)
     *
     * @return стандартное отклонение
     */
    public double getStandardDeviation() {
        if (count == 0) {
            return 0.0;
        }

        double mean = getMean();
        double meanSquares = sumSquares / count;
        double variance = meanSquares - (mean * mean);

        // Защита от отрицательной дисперсии из-за ошибок округления
        if (variance < 0) {
            variance = 0;
        }

        return Math.sqrt(variance);
    }

    /**
     * Возвращает гистограмму в виде Map
     *
     * @return гистограмма
     */
    public Map<String, Long> getHistogramMap() {
        Map<String, Long> histogramMap = new LinkedHashMap<>();

        for (int i = 0; i < bucketCount; i++) {
            long bucketMin = rangeMin + (long) i * bucketSize;
            long bucketMax = (i == bucketCount - 1) ? rangeMax : rangeMin + (long) (i + 1) * bucketSize;

            String key = "[" + bucketMin + ", " + bucketMax + ")";
            histogramMap.put(key, (long) histogram[i]);
        }

        return histogramMap;
    }

    /**
     * Возвращает общее количество обработанных чисел
     *
     * @return количество чисел
     */
    public long getCount() {
        return count;
    }

    /**
     * Логирует статистику для отладки
     */
    public void logStatistics() {
        log.debug("Statistics: count={}, min={}, max={}, mean={}, stdDev={}",
                count, min, max, getMean(), getStandardDeviation());
    }
}
