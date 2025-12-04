package fr.miniprojet.csp;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

import java.util.Map;

public class Mod2Optimization {

    public static void main(String[] args) throws Exception {

        // ------------------ Paramètres ------------------
        int EMP = 500; // Nombre d'employés
        final int SHIFTS = 6; // Nombre de shifts par employé
        final int LINES = 21; // Nombre de lignes

        if (args.length >= 1) {
            try { EMP = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }

        // Capacité de chaque ligne
        Map<Integer, Integer> capacities = DataLoader.loadCapacities("data/lines.csv");

        // Lignes fortement liées (FL)
        int[][] FL = { {1,5}, {3,7}, {6,9}, {10,12}, {14,20} };

        // Lignes à forte activité (FA)
        int[] FA = {2,4,8};

        // ------------------ Modèle Choco ------------------
        Model model = new Model("MOD2 - Optimization CSP");

        // Variables X[e][s] ∈ [1..LINES]
        IntVar[][] X = new IntVar[EMP][SHIFTS];
        for (int e = 0; e < EMP; e++) {
            for (int s = 0; s < SHIFTS; s++) {
                X[e][s] = model.intVar("E" + e + "_S" + s, 1, LINES);
            }
        }

        // ======================== Contraintes MOD1 ========================

        for (int e = 0; e < EMP; e++) {
            model.allDifferent(X[e]).post();
        }

        for (int e = 0; e < EMP; e++) {
            for (int s = 0; s < SHIFTS - 1; s++) {
                for (int[] fl : FL) {
                    int l1 = fl[0], l2 = fl[1];
                    model.ifThen(
                            model.arithm(X[e][s], "=", l1),
                            model.arithm(X[e][s + 1], "=", l2)
                    );
                }
            }
        }

        for (int e = 0; e < EMP; e++) {
            IntVar[] counts = new IntVar[FA.length];
            for (int k = 0; k < FA.length; k++) {
                counts[k] = model.intVar("c_emp" + e + "_fa_" + FA[k], 0, SHIFTS);
                model.count(FA[k], X[e], counts[k]).post();
            }
            model.sum(counts, ">=", 1).post();
        }


        IntVar[] overload = new IntVar[LINES]; 
        for (int l = 1; l <= LINES; l++) {
            IntVar[] shiftCounts = new IntVar[SHIFTS];

            for (int s = 0; s < SHIFTS; s++) {
                IntVar[] shiftS = new IntVar[EMP];
                for (int e = 0; e < EMP; e++) shiftS[e] = X[e][s];
                shiftCounts[s] = model.intVar("count_l" + l + "_s" + s, 0, EMP);
                model.count(l, shiftS, shiftCounts[s]).post();
            }

            IntVar totalLine = model.intVar("totalLine" + l, 0, EMP * SHIFTS);
            model.sum(shiftCounts, "=", totalLine).post();

            int cap = capacities.getOrDefault(l, EMP * SHIFTS);

            IntVar diff = model.intVar("diff_l" + l, -EMP * SHIFTS, EMP * SHIFTS);
            model.arithm(diff, "=", totalLine, "-", cap).post();

            // surcharge = max(0, diff)
            overload[l - 1] = model.intVar("overload_l" + l, 0, EMP * SHIFTS);

        }

        // Surcharge totale à minimiser
        IntVar totalOverload = model.intVar("totalOverload", 0, EMP * SHIFTS);
        model.sum(overload, "=", totalOverload).post();

    
        model.setObjective(Model.MINIMIZE, totalOverload);

        // ======================== Solveur ========================
        Solver solver = model.getSolver();
        solver.limitTime("600s"); // 10 minutes
        solver.showStatistics();

        boolean sat = solver.solve();
        System.out.println("Résultat SAT = " + sat);

        if (sat) {
            System.out.println("\n===== Tableau des Affectations (premiers 10 employés) =====");
            System.out.println("Employé | S0 | S1 | S2 | S3 | S4 | S5");
            System.out.println("------------------------------------------");
            for (int e = 0; e < Math.min(10, EMP); e++) {
                System.out.printf("E%-6d|", e);
                for (int s = 0; s < SHIFTS; s++)
                    System.out.printf(" %2d |", X[e][s].getValue());
                System.out.println();
            }
            System.out.println("\nTotal surcharge = " + totalOverload.getValue());
        }

        solver.printStatistics();
    }
}
