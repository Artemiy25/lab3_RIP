package ru.bauman.andesis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RandomStatisticsDto {

    @JsonProperty("count")
    private long count;

    @JsonProperty("min")
    private long min;

    @JsonProperty("max")
    private long max;

    @JsonProperty("mean")
    private double mean;

    @JsonProperty("standardDeviation")
    private double standardDeviation;

    @JsonProperty("histogram")
    private Map<String, Long> histogram;

    @JsonProperty("generatedAt")
    private long generatedAt;

    @JsonProperty("processingTimeMs")
    private long processingTimeMs;
}
