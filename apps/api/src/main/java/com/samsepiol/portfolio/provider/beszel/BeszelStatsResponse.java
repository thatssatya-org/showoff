package com.samsepiol.portfolio.provider.beszel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record BeszelStatsResponse(@JsonProperty("items") List<Record> items) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Record(String system, Stats stats, Instant created) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Stats(Double cpu, @JsonProperty("mp") Double memoryPercent,
                 @JsonProperty("dp") Double diskPercent, @JsonProperty("la") List<Double> loadAverages) {
        Double loadAverage() {
            return loadAverages == null || loadAverages.isEmpty() ? null : loadAverages.getFirst();
        }
    }
}
