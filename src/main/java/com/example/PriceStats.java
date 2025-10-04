package com.example;

import com.example.api.ElpriserAPI;
import java.util.List;

public final class PriceStats {

    public record Stats(double min, int minIdx, double max, int maxIdx, double average) {}

    public static Stats compute(List<ElpriserAPI.Elpris> today) {
        if (today == null || today.isEmpty()) {
            throw new IllegalArgumentException("Ingen data finns för detta datum!");
        }
        double min = Double.POSITIVE_INFINITY, max = -1, sum = 0.0;
        int minIdx = -1, maxIdx = -1;

        for (int i = 0; i < today.size(); i++) {
            double p = today.get(i).sekPerKWh();
            sum += p;
            if (p < min) { min = p; minIdx = i; }
            if (p > max) { max = p; maxIdx = i; }
        }
        return new Stats(min, minIdx, max, maxIdx, sum / today.size());
    }
}
