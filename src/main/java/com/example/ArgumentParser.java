package com.example;

import java.time.LocalDate;
import java.util.Locale;

public final class ArgumentParser {

    public record ArgsInputs(String zone, LocalDate date, Integer chargingTime, boolean sorted) {}

    private static final String[] VALID_ZONES = {"SE1","SE2","SE3","SE4"};

    public static boolean wantsHelp(String[] args) {
        if (args == null || args.length == 0) return true;
        for (String s : args) if ("--help".equalsIgnoreCase(s)) return true;
        return false;
    }

    public static ArgsInputs parse(String[] args) {
        // --zone (required)
        String zone = readValue(args, "--zone", "Error: --zone saknar värde");
        if (zone == null) {
            throw new IllegalArgumentException("Error: det obligatoriska argumentet --zone saknas!");
        }
        zone = zone.toUpperCase(Locale.ROOT);
        if (!isValidZone(zone)) {
            throw new IllegalArgumentException("Fel zon: " + zone + ". Giltiga zoner är SE1, SE2, SE3, SE4");
        }

        // --date (optional, defaults to today)
        LocalDate date;
        String dateStr = readValue(args, "--date", "Error: --date saknar värde");
        if (dateStr == null) {
            date = LocalDate.now();
        } else {
            try {
                date = LocalDate.parse(dateStr);
            } catch (Exception e) {
                throw new IllegalArgumentException("Fel datum: " + dateStr + ". Använd format YYYY-MM-DD.");
            }
        }

        // --charging (optional, 2h|4h|8h)
        Integer chargingTime = null;
        String chargingVal = readValue(args, "--charging", "Error: --charging saknar värde.");
        if (chargingVal != null) {
            chargingTime = switch (chargingVal) {
                case "2h" -> 2;
                case "4h" -> 4;
                case "8h" -> 8;
                default -> throw new IllegalArgumentException("Felaktigt charging-intervall");
            };
        }

        boolean sorted = hasFlag(args, "--sorted");

        return new ArgsInputs(zone, date, chargingTime, sorted);
    }

    public static String helpText() {
        return """
               Usage: java -cp target/classes com.example.Main --zone SE1|SE2|SE3|SE4 [--date YYYY-MM-DD] [-sorted] [--charging 2h|4h|8h]
               Arguments:
               --zone <arg>     Possible arg: SE1, SE2, SE3, SE4
               --date           Date of interest
               --charging       Charging
               --sorted         Sorted
               --help           Help
               """;
    }

    // ---- helpers ----

    private static boolean isValidZone(String z) {
        for (String vz : VALID_ZONES) if (vz.equals(z)) return true;
        return false;
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String a : args) if (flag.equalsIgnoreCase(a)) return true;
        return false;
    }

    private static String readValue(String[] args, String key, String errorIfMissingValue) {
        for (int i = 0; i < args.length; i++) {
            if (key.equalsIgnoreCase(args[i])) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    return args[i + 1];
                } else {
                    throw new IllegalArgumentException(errorIfMissingValue);
                }
            }
        }
        return null;
    }
}
