import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

class MineUsageRecord {
    private String date;
    private double waterUsage;
    private double landUsage;

    public MineUsageRecord(String date, double waterUsage, double landUsage) {
        this.date = date;
        this.waterUsage = waterUsage;
        this.landUsage = landUsage;
    }

    public String getDate() {
        return date;
    }

    public double getWaterUsage() {
        return waterUsage;
    }

    public double getLandUsage() {
        return landUsage;
    }

    public String toCSV() {
        return String.format("%s,%.2f,%.2f", date, waterUsage, landUsage);
    }

    public static MineUsageRecord fromCSV(String csvLine) {
        String[] parts = csvLine.split(",");
        return new MineUsageRecord(parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    }

    @Override
    public String toString() {
        return String.format("Date: %s, Water used: %.2f acre-feet, Land used: %.2f acres", date, waterUsage, landUsage);
    }
}

class Mine {
    private String name;
    private double waterLimit; // in acre-feet
    private List<MineUsageRecord> usageRecords;

    public Mine(String name, double waterLimit) {
        this.name = name;
        this.waterLimit = waterLimit;
        this.usageRecords = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public double getWaterLimit() {
        return waterLimit;
    }

    public List<MineUsageRecord> getUsageRecords() {
        return usageRecords;
    }

    public void addUsage(double waterUsage, double landUsage, String date) throws Exception {
        double totalWaterUsed = getTotalWaterUsed();
        if (waterUsage > (waterLimit - totalWaterUsed)) {
            throw new Exception("Water usage exceeds the limit for the year.");
        }
        usageRecords.add(new MineUsageRecord(date, waterUsage, landUsage));
    }

    public double getTotalWaterUsed() {
        return usageRecords.stream().mapToDouble(MineUsageRecord::getWaterUsage).sum();
    }

    public double getWaterRemaining() {
        return waterLimit - getTotalWaterUsed();
    }

    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        for (MineUsageRecord record : usageRecords) {
            sb.append(name).append(",").append(record.toCSV()).append("\n");
        }
        return sb.toString();
    }

    public static Mine fromCSV(List<String> csvLines, double waterLimit) {
        Mine mine = new Mine(csvLines.get(0).split(",")[0], waterLimit);
        for (String line : csvLines) {
            mine.usageRecords.add(MineUsageRecord.fromCSV(line.split(",", 2)[1]));
        }
        return mine;
    }

    public void showRecords() {
        for (MineUsageRecord record : usageRecords) {
            System.out.println(record);
        }
        System.out.printf("Total water used: %.2f acre-feet, Water remaining: %.2f acre-feet%n", getTotalWaterUsed(), getWaterRemaining());
    }
}

class WaterUsageSensor {
    private HashMap<String, Mine> mines;
    private String filePath = "mine_usage.txt";

    public WaterUsageSensor() throws IOException {
        mines = new HashMap<>();
        mines.put("Rosemont", new Mine("Rosemont", 6000));
        mines.put("Sierrita", new Mine("Sierrita", 27180));
        mines.put("Mission", new Mine("Mission", 12590));
        loadUsageData();
    }

    public void requestData(String mineName, double waterUsage, double landUsage, String date) throws IOException {
        if (!mines.containsKey(mineName)) {
            System.out.println("Unknown mine, please provide documentation.");
            return;
        }

        Mine mine = mines.get(mineName);

        try {
            mine.addUsage(waterUsage, landUsage, date);
            saveUsageData();
            System.out.println("Usage updated successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void saveUsageData() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Mine mine : mines.values()) {
                writer.write(mine.toCSV());
            }
        }
    }

    private void loadUsageData() throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                HashMap<String, List<String>> tempData = new HashMap<>();
                while ((line = reader.readLine()) != null) {
                    String mineName = line.split(",")[0];
                    tempData.putIfAbsent(mineName, new ArrayList<>());
                    tempData.get(mineName).add(line);
                }

                for (String mineName : tempData.keySet()) {
                    if (mines.containsKey(mineName)) {
                        double waterLimit = mines.get(mineName).getWaterLimit();
                        Mine mine = Mine.fromCSV(tempData.get(mineName), waterLimit);
                        mines.put(mineName, mine);
                    }
                }
            }
        }
    }

    public void getRecords(String mineName) {
        if (!mines.containsKey(mineName)) {
            System.out.println("No records found for this mine.");
            return;
        }

        mines.get(mineName).showRecords();
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        WaterUsageSensor sensor = null;

        try {
            sensor = new WaterUsageSensor();
        } catch (IOException e) {
            System.out.println("Error loading data: " + e.getMessage());
            return;
        }

        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1. Look at records");
            System.out.println("2. Type in new data");
            System.out.println("3. Exit");
            System.out.print("Choose an option (1/2/3): ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume the newline character after int input

            switch (choice) {
                case 1:
                    System.out.print("Enter mine name: ");
                    String mineName = scanner.nextLine();
                    sensor.getRecords(mineName);
                    break;

                case 2:
                    System.out.print("Enter mine name: ");
                    mineName = scanner.nextLine();

                    System.out.print("Enter date (YYYY-MM-DD): ");
                    String date = scanner.nextLine();

                    System.out.print("Enter land usage (in acres): ");
                    double landUsage = scanner.nextDouble();

                    System.out.print("Enter water usage (in acre-feet): ");
                    double waterUsage = scanner.nextDouble();

                    try {
                        sensor.requestData(mineName, waterUsage, landUsage, date);
                    } catch (IOException e) {
                        System.out.println("File error: " + e.getMessage());
                    }
                    break;

                case 3:
                    System.out.println("Exiting the program.");
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid choice. Please choose 1, 2, or 3.");
            }
        }
    }
}
