package fr.miniprojet.csp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class DataLoader {

    public static Map<Integer, Integer> loadCapacities(String path) throws Exception {

        Map<Integer, Integer> capacities = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = br.readLine()) != null) {

                line = line.trim();

                // ignorer lignes vides
                if (line.isEmpty()) continue;

                // ignorer commentaires
                if (line.startsWith("#")) continue;

                // ignorer FL et FA
                if (line.startsWith("FL") || line.startsWith("FA")) continue;

                // traiter les lignes "id,cap"
                if (line.contains(",")) {
                    String[] parts = line.split(",");
                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        int cap = Integer.parseInt(parts[1].trim());
                        capacities.put(id, cap);
                    } catch (Exception ignored) {
                        // ignore les lignes invalides
                    }
                }
            }
        }

        return capacities;
    }
}
