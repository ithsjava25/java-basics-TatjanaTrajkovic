package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class App {
    public void start(String[] args){
        if (ArgumentParser.wantsHelp(args)) {
            System.out.println(ArgumentParser.helpText());
            return;
        }

        ArgumentParser.ArgsInputs argsInputs;
        try {
            argsInputs = ArgumentParser.parse(args);
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
            return;
        }

        String zone = argsInputs.zone();
        LocalDate date = argsInputs.date();
        int chargingTime = (argsInputs.chargingTime() == null) ? 0 : argsInputs.chargingTime();
        boolean sorted = argsInputs.sorted();

        System.out.println("Vald zon: " + zone);

        ElpriserAPI elpriserAPI = new ElpriserAPI();
        List<ElpriserAPI.Elpris> todaysPrice = elpriserAPI.getPriser(date, ElpriserAPI.Prisklass.valueOf(zone));

        if (todaysPrice == null || todaysPrice.isEmpty()){
            System.out.println("Ingen data finns för detta datum!");
            return;
        }

        PriceStats.Stats stats;
        try {
            stats = PriceStats.compute(todaysPrice);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }

        String minHour = PriceFormat.hourRange(todaysPrice.get(stats.minIdx()));
        String maxHour = PriceFormat.hourRange(todaysPrice.get(stats.maxIdx()));

        System.out.println("Lägsta pris: " + PriceFormat.oreFromSek(stats.min()) + " (" + minHour + ")");
        System.out.println("Högsta pris: " + PriceFormat.oreFromSek(stats.max()) + " (" + maxHour + ")");
        System.out.println("Medelpris: " + PriceFormat.oreFromSek(stats.average()));

        if (chargingTime != 0) {
            List<ElpriserAPI.Elpris> tomorrowPrice =
                    elpriserAPI.getPriser(date.plusDays(1), ElpriserAPI.Prisklass.valueOf(zone));

            List<ElpriserAPI.Elpris> twoDaysList = new ArrayList<>(todaysPrice);
            if (tomorrowPrice != null) {
                twoDaysList.addAll(tomorrowPrice);
            }

            ChargingWindow.Result r = ChargingWindow.find(todaysPrice, twoDaysList, chargingTime);
            if (r == null) {
                System.out.println("Inget helt laddningsfönster får plats i tillgängliga timmar.");
            } else {
                System.out.printf("Påbörja laddning kl %s%n", r.startHHmm());
                System.out.println("Medelpris för fönster: " + PriceFormat.oreValue(r.meanOre()) + " öre");
            }
        }

        if (sorted) {
            todaysPrice.stream()
                    .sorted(Comparator
                            .comparingDouble(ElpriserAPI.Elpris::sekPerKWh)
                            .thenComparing(ElpriserAPI.Elpris::timeStart))
                    .forEach(e ->
                            System.out.println(PriceFormat.hourRange(e) + " " + PriceFormat.oreFromSek(e.sekPerKWh()) + " öre"));
        }
    }


}
