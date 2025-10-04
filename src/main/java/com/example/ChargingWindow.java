package com.example;

import com.example.api.ElpriserAPI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ChargingWindow {

    public record Result(int startIndex, String startHHmm, double meanOre) {}

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    public static Result find(List<ElpriserAPI.Elpris> today,
                              List<ElpriserAPI.Elpris> twoDays,
                              int chargingHours) {
        if (chargingHours <= 0 || today == null || today.isEmpty() || twoDays == null || twoDays.isEmpty()) {
            return null;
        }

        double minSum = Double.POSITIVE_INFINITY;
        int minIndex = -1;

        for (int i = 0; i < today.size() && i + chargingHours <= twoDays.size(); i++) {
            double sum = 0.0;
            for (int j = 0; j < chargingHours; j++) {
                sum += twoDays.get(i + j).sekPerKWh();
            }
            if (sum < minSum) {
                minSum = sum; minIndex = i;
            } else if (sum == minSum && i < minIndex){
                minIndex = i;
            }
        }

        if (minIndex == -1) return null;

        var startAt = twoDays.get(minIndex).timeStart();
        double meanInOre = (minSum / chargingHours) * 100.0; // already öre

        return new Result(minIndex, startAt.format(HHMM), meanInOre);
    }
}
