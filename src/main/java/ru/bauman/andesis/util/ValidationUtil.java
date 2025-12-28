package ru.bauman.andesis.util;

import lombok.extern.slf4j.Slf4j;
import ru.bauman.andesis.exception.InvalidParametersException;

@Slf4j
public class ValidationUtil {

    private static final long MAX_COUNT = 10_000_000;
    private static final long DEFAULT_COUNT = 1_000;
    private static final long MIN_RANGE = -1_000_000_000;
    private static final long MAX_RANGE = 1_000_000_000;

    public static void validateCount(long count) {
        if (count <= 0) {
            throw new InvalidParametersException("Count must be greater than 0");
        }
        if (count > MAX_COUNT) {
            throw new InvalidParametersException(
                    "Count cannot exceed " + MAX_COUNT + " (provided: " + count + ")");
        }
    }

    public static void validateRange(long min, long max) {
        if (min >= max) {
            throw new InvalidParametersException(
                    "Min value must be less than max value (min: " + min + ", max: " + max + ")");
        }
        if (min < MIN_RANGE || max > MAX_RANGE) {
            throw new InvalidParametersException(
                    "Values must be within range [" + MIN_RANGE + ", " + MAX_RANGE + "]");
        }
    }

    public static long getValidatedCount(Long count) {
        if (count == null) {
            return DEFAULT_COUNT;
        }
        validateCount(count);
        return count;
    }

    public static long getValidatedMin(Long min, long max) {
        if (min == null) {
            return max - 1_000_000;
        }
        if (min < MIN_RANGE) {
            throw new InvalidParametersException("Min value cannot be less than " + MIN_RANGE);
        }
        if (min >= max) {
            throw new InvalidParametersException("Min must be less than max");
        }
        return min;
    }

    public static long getValidatedMax(Long max) {
        if (max == null) {
            return 1_000_000;
        }
        if (max > MAX_RANGE) {
            throw new InvalidParametersException("Max value cannot exceed " + MAX_RANGE);
        }
        return max;
    }

    public static long getValidatedRange(Long range, long min, long max) {
        if (range == null) {
            return max - min;
        }
        if (range <= 0) {
            throw new InvalidParametersException("Range must be greater than 0");
        }
        return range;
    }
}
