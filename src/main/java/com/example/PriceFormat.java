package com.example;

import com.example.api.ElpriserAPI;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PriceFormat {

    private static final DateTimeFormatter HH = DateTimeFormatter.ofPattern("HH", Locale.ROOT);

    public static String hourRange(ElpriserAPI.Elpris e) {
        return e.timeStart().format(HH) + "-" + e.timeEnd().format(HH);
    }

    /** SEK/kWh -> "xx,yy" öre (string) */
    public static String oreFromSek(double sekPerKWh) {
        return String.format(Locale.ROOT, "%.2f", sekPerKWh * 100.0).replace('.', ',');
    }

    /** value already in öre -> "xx,yy" (string) */
    public static String oreValue(double ore) {
        return String.format(Locale.ROOT, "%.2f", ore).replace('.', ',');
    }
}
