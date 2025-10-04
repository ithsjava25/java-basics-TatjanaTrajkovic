package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        ElpriserAPI elpriserAPI = new ElpriserAPI();

        if(args == null || args.length == 0){
            printHelp();
            return;
        }
        for(String s : args){
            if("--help".equalsIgnoreCase(s)){
                printHelp();
                return;
            }
        }

        //zone
        String zone = null;
        String[] validZones = {"SE1", "SE2", "SE3", "SE4"};

        for(int i = 0; i < args.length; i++){
            if("--zone".equalsIgnoreCase(args[i])){
                if(i + 1 < args.length && !(args[i + 1].contains("--"))){
                    zone = args[i + 1].toUpperCase();
                }else{
                    System.out.println("ERROR! Missing value for --zone");
                    return;
                }
            }
        }
        if(zone == null){
            System.out.println("Error: --zone is required!");
            return;
        }
        boolean isValid = false;
        for(String z : validZones){
            if(z.equals(zone)){
                isValid = true;
                break;
            }
        }
        if(!isValid){
            System.out.println("Fel zon: " + zone + ". Giltiga zoner är SE1, SE2, SE3, SE4");
            return;
        }
        System.out.println("Vald zon: " + zone);


        //Date
        String dateStr = null;
        LocalDate date = null;

        for(int i = 0; i < args.length; i++){
            if("--date".equalsIgnoreCase(args[i])) {
                if (i + 1 < args.length && !(args[i + 1].contains("--"))) {
                    dateStr = args[i + 1];
                } else {
                    System.out.println("Error: --date saknar värde");
                    return;
                }
            }
        }

        if(dateStr == null){
            date = LocalDate.now();
        }else{
            try{
                date = LocalDate.parse(dateStr);
            }catch(Exception e){
                System.out.println("Fel datum: " + dateStr + ". Använd format YYYY-MM-DD.");
                return;
            }
        }

        // Charging
        int chargingTime = 0;
        for(int i = 0; i < args.length; i++){
            if("--charging".equalsIgnoreCase(args[i])){
                if(i + 1 < args.length && !(args[i + 1].contains("--"))){
                    switch (args[i + 1]) {
                        case "2h" -> chargingTime = 2;
                        case "4h" -> chargingTime = 4;
                        case "8h" -> chargingTime = 8;
                        default -> {
                            System.out.println("Invalid charging interval");
                            return;
                        }
                    };
                }else{
                    System.out.println("Error: --charging saknar värde.");
                    return;
                }
            }
        }

        // Sorted
        boolean sorted = false;
        for(int i = 0; i < args.length; i++){
            if("--sorted".equalsIgnoreCase(args[i])){
                sorted = true;
            }
        }

        List<ElpriserAPI.Elpris> todaysPrice = elpriserAPI.getPriser(date, ElpriserAPI.Prisklass.valueOf(zone));
        //System.out.println("Todaysprice: " + todaysPrice);
        if (todaysPrice.isEmpty()){
            System.out.println("Ingen data finns för detta datum!");
            return;
        }

        // min, max, average price
        double min = Double.POSITIVE_INFINITY;
        int minIdx = -1;
        double max = -1;
        int maxIdx = -1;
        double averagePrice = 0;
        double sum = 0;
        double price = 0;

        for(int i = 0; i < todaysPrice.size(); i++){
            price = todaysPrice.get(i).sekPerKWh();
            sum += price;
            if(price > max){
                max = price;
                maxIdx = i;
            }
            if(price < min){
                min = price;
                minIdx = i;
            }
        }

        String minHour = String.format("%02d-%02d", todaysPrice.get(minIdx).timeStart().getHour(), todaysPrice.get(minIdx).timeEnd().getHour());
        String maxHour = String.format("%02d-%02d", todaysPrice.get(maxIdx).timeStart().getHour(), todaysPrice.get(maxIdx).timeEnd().getHour());

        averagePrice = sum / todaysPrice.size();
        String minStr = String.format("%.2f", min * 100).replace(".", ",");
        String maxStr = String.format("%.2f", max * 100).replace(".", ",");
        String avgStr = String.format("%.2f", averagePrice * 100).replace(".", ",");

        System.out.println("Lägsta pris: " + minStr + " (" + minHour + ")");
        System.out.println("Högsta pris: " + maxStr + " (" + maxHour + ")");
        System.out.println("Medelpris: " + avgStr);


        // Charging
        if(chargingTime != 0){
            List<ElpriserAPI.Elpris> tomorrowPrice = elpriserAPI.getPriser(date.plusDays(1), ElpriserAPI.Prisklass.valueOf(zone));
            System.out.println(tomorrowPrice);

            List<ElpriserAPI.Elpris> twoDaysList = new ArrayList<>(todaysPrice);
            if (tomorrowPrice != null) {
                twoDaysList.addAll(tomorrowPrice);
            }

            double minSum = Double.POSITIVE_INFINITY;
            int minIndex = -1;
            String meanStr = "";

            System.out.println("Today size " + todaysPrice.size());
            System.out.println("Tomorrow size " + tomorrowPrice.size());


            for(int i = 0; i < todaysPrice.size() && i + chargingTime <= twoDaysList.size(); i++){
                sum = 0;
                for(int j = 0; j < chargingTime; j++){
                    sum += twoDaysList.get(i+j).sekPerKWh();
                };
                if(sum < minSum){
                    minSum = sum;
                    minIndex = i;
                }
            }
            if (minIndex == -1) {
                System.out.println("Inget helt laddningsfönster får plats i tillgängliga timmar.");
            } else {
                var startAt = twoDaysList.get(minIndex).timeStart(); // ZonedDateTime / LocalDateTime
                String startStr = startAt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

                double meanInOre = (minSum / chargingTime) * 100.0;
                meanStr = String.format("%.2f", meanInOre).replace(".", ",");

                System.out.printf("Påbörja laddning kl %s%n", startStr);
                System.out.println("Medelpris för fönster: " + meanStr + " öre");
            }
        }

        // Sorted
        if (sorted) {
            // Fetch prices for the requested day
            //List<ElpriserAPI.Elpris> slots = elpriserAPI.getPriser(date, ElpriserAPI.Prisklass.valueOf(zone));

            // Sort by price (ascending), then by start time (earliest first on ties)
            todaysPrice.sort((x, y) -> {
                if (x.sekPerKWh() < y.sekPerKWh()) return -1;
                if (x.sekPerKWh() > y.sekPerKWh()) return 1;
                return x.timeStart().compareTo(y.timeStart());
            });

            // Format results like "01-02 10,00 öre"
            for (ElpriserAPI.Elpris e : todaysPrice) {
                System.out.println(formatPrice(e));
            }
            return;
        }

    }

    public static void printHelp(){
        System.out.println("Usage: java -cp target/classes com.example.Main --zone SE1|SE2|SE3|SE4 [--date YYYY-MM-DD] [-sorted] [--charging 2h|4h|8h]" );
        System.out.println("Arguments: ");
        System.out.println("--zone <arg>     Possible arg: SE1, SE2, SE3, SE4");
        System.out.println("--date           Date of interest");
        System.out.println("--charging       Charging");
        System.out.println("--sorted         Sorted");
        System.out.println("--help           Help");
    }

    private static String formatPrice(ElpriserAPI.Elpris e){
        String range = String.format("%02d-%02d", e.timeStart().getHour(), e.timeEnd().getHour());
        String ore = String.format("%.2f", e.sekPerKWh() * 100.0).replace('.', ',');
        return range + " " + ore + " öre";
    }

}