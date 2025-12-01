package fr.miniprojet.csp;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;

public class Mod1Feasibility {

    public static void main(String[] args) throws Exception {
        // --- option : passer le nombre d'employés en argument (sinon 500)
        int EMP = 500;
        if (args.length >= 1) {
            try { EMP = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }
        final int SHIFTS = 6; // 6 shifts par employé (j = 0..5)
        final int LINES = 21;



        // ============================
        // 1. Charger les données
        // ============================
        Map<Integer, Integer> capacities = DataLoader.loadCapacities("data/lines.csv");

        // Vérifications / debug
        System.out.println("=== DEBUG : données chargées ===");
        for (int k = 1; k <= LINES; k++) {
            Integer c = capacities.get(k);
            System.out.printf("Ligne %2d -> cap = %s%n", k, c == null ? "NULL" : c);
        }

        // Exemple FL
        int[][] FL = {{1, 5}, {3, 7}, {6, 9}, {10, 12}, {14, 20}};
        System.out.println("FL pairs (exemples) : " + Arrays.deepToString(FL));

        // Exemple FA
        int[] FA = {2, 4, 8};
        System.out.println("FA lines (exemples) : " + Arrays.toString(FA));
        System.out.println("EMP=" + EMP + ", SHIFTS=" + SHIFTS + ", LINES=" + LINES);

        // ============================
        // 2. Créer le modèle
        // ============================
        Model model = new Model("MOD1 - Feasibility CSP");

        // Variables X[i][j] ∈ {1..LINES}
        IntVar[][] X = new IntVar[EMP][SHIFTS];
        for (int i = 0; i < EMP; i++) {
            for (int j = 0; j < SHIFTS; j++) {
                X[i][j] = model.intVar("E" + i + "_S" + j, 1, LINES);
            }
        }

        // ============================
        // 3. C2 : allDifferent par employé (il ne doit pas travailler 2 fois sur la même ligne)
        // ============================
        for (int i = 0; i < EMP; i++) {
            model.allDifferent(X[i]).post();
        }

        // ============================
        // 4. C3 : Contraintes FL (si X[i][j] == l1 -> X[i][j+1] == l2)
        // ============================
        for (int i = 0; i < EMP; i++) {
            for (int j = 0; j < SHIFTS - 1; j++) {
                for (int[] fl : FL) {
                    int l1 = fl[0], l2 = fl[1];

                    if (l1 >= 1 && l1 <= LINES && l2 >= 1 && l2 <= LINES) {
                        model.ifThen(
                                model.arithm(X[i][j], "=", l1),
                                model.arithm(X[i][j + 1], "=", l2)
                        );
                    }
                }
            }
        }

        // ============================
        // 5. C4 : FA (au moins une fois par employé)
        // On crée pour chaque employé la somme des occurrences de toutes les FA et on impose >= 1
        // ============================
        for (int i = 0; i < EMP; i++) {
            IntVar[] counts = new IntVar[FA.length];
            for (int k = 0; k < FA.length; k++) {
                int lineId = FA[k];
                // sécurité: si FA contient un id hors domaine, on l'ignore
                if (lineId < 1 || lineId > LINES) {
                    counts[k] = model.intVar("c_emp" + i + "_fa_" + lineId, 0, 0); // forcé à 0
                } else {
                    counts[k] = model.intVar("c_emp" + i + "_fa_" + lineId, 0, SHIFTS);
                    model.count(lineId, X[i], counts[k]).post();
                }
            }
            // somme des counts >= 1
            model.sum(counts, ">=", 1).post();
        }

        // ============================
        // 6. C5 : Capacités par shift
        // Pour chaque shift j et chaque ligne, le nombre d'employés assignés doit être <= capacité
        // ============================
        for (int j = 0; j < SHIFTS; j++) {
            IntVar[] shiftJ = new IntVar[EMP];
            for (int i = 0; i < EMP; i++) shiftJ[i] = X[i][j];

            for (int line = 1; line <= LINES; line++) {
                Integer cap = capacities.get(line);
                if (cap == null) {
                    // sécurité : si cap manquante, on met une grande capacité pour ne pas bloquer
                    cap = EMP;
                }
                // on crée une variable de comptage nommée
                IntVar occ = model.intVar("occ_day" + j + "_l" + line, 0, Math.min(EMP, cap));
                model.count(line, shiftJ, occ).post();
                model.arithm(occ, "<=", cap).post();
            }
        }

        // ============================
        // 7. Solve
        // ============================
        Solver solver = model.getSolver();

        // time limit 10 minutes (600s)
        solver.limitTime("600s");

        // Affiche uniquement les statistiques (pas toutes les solutions)
        solver.showStatistics();


        System.out.println("Lancement du solveur (timeout 600s). Patientez ...");
        boolean sat = solver.solve();

        System.out.println("Résultat : SAT = " + sat);
        if (sat) {
            // exemple : afficher les 5 premiers employés
            System.out.println("--- Exemple d'affectations (5 premiers employés) ---");
            for (int i = 0; i < Math.min(5, EMP); i++) {
                System.out.print("E" + i + ": ");
                for (int j = 0; j < SHIFTS; j++) {
                    System.out.print(X[i][j].getValue() + (j + 1 == SHIFTS ? "" : ","));
                }
                System.out.println();
            }

            // ====== Affichage formaté : TABLEAU DES AFFECTATIONS ======
            System.out.println("\n===== TABLEAU DES AFFECTATIONS =====");
            System.out.println("Employé | S0 | S1 | S2 | S3 | S4 | S5");
            System.out.println("--------------------------------------------");

            for (int e = 0; e < EMP; e++) {
                System.out.printf("E%-6d|", e);
                for (int s = 0; s < SHIFTS; s++) {
                    System.out.printf(" %2d |", X[e][s].getValue());
                }
                System.out.println();
            }


        }

        System.out.println("=== Statistiques solver ===");
        solver.printStatistics();
    }
}

