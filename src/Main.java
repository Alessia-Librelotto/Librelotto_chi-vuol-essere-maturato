import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static boolean used5050 = false;
    private static boolean usedAudience = false;
    private static int correctAnswers = 0;

    public static void main(String[] args) {
        showTitle();

        System.out.print("Inserisci il tuo nome: ");
        String playerName = scanner.nextLine();

        System.out.println("\nBenvenuto " + playerName + "! Preparati a diventare MATURATO!");
        System.out.println("Dovrai rispondere a 10 domande (5 facili, 3 medie, 2 difficili)");
        System.out.println("Hai 2 aiuti disponibili:");
        System.out.println("  - Guarda il bigliettino (50/50)");
        System.out.println("  - Suggerimento dei compagni (pubblico)");
        System.out.println("\nComandi disponibili:");
        System.out.println("  A/B/C/D - Scegli una risposta");
        System.out.println("  H - Usa un aiuto");
        System.out.println("  R - Ritirati\n");

        System.out.print("Premi INVIO per iniziare...");
        scanner.nextLine();

        APIClient client = new APIClient();

        System.out.println("\nCaricamento domande...");
        List<APIQuestions> allQuestions = new ArrayList<>();

        // Strategia: prova a caricare le domande, se non ci sono abbastanza riduci gradualmente
        // Carica 5 domande facili
        List<APIQuestions> easy = client.fetchQuestions(5, "multiple", "easy");
        allQuestions.addAll(easy);

        try {
            Thread.sleep(5000); // Pausa di 500ms tra le richieste
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Carica domande medie
        List<APIQuestions> medium = client.fetchQuestions(3, "multiple", "medium");
        allQuestions.addAll(medium);

        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Carica domande difficili
        List<APIQuestions> hard = client.fetchQuestions(2, "multiple", "hard");
        allQuestions.addAll(hard);

        // Se non abbiamo abbastanza domande, aggiungi altre domande facili
        if (allQuestions.size() < 10) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int needed = 10 - allQuestions.size();
            System.out.println("Aggiunta di " + needed + " domande aggiuntive...");
            List<APIQuestions> extra = client.fetchQuestions(needed, "multiple", "easy");
            allQuestions.addAll(extra);
        }

        System.out.println("\nTotale domande caricate: " + allQuestions.size() + " su 10");

        if (allQuestions.size() < 5) {
            System.out.println("\nERRORE: Non ci sono abbastanza domande disponibili.");
            System.out.println("Gioco annullato. Riprova più tardi.");
            return;
        }

        if (allQuestions.size() < 10) {
            System.out.println("\nATTENZIONE: Caricate solo " + allQuestions.size() + " domande invece di 10.");
            System.out.println("Vuoi continuare comunque? (S/N)");
            String answer = scanner.nextLine().trim().toUpperCase();

            if (!answer.equals("S")) {
                System.out.println("Gioco annullato. Riprova più tardi.");
                return;
            }
        }

        System.out.println("Domande caricate con successo!\n");

        // Loop principale del gioco
        int totalQuestions = allQuestions.size();
        for (int i = 0; i < allQuestions.size(); i++) {
            System.out.println("═".repeat(70));
            System.out.println("DOMANDA " + (i + 1) + " di " + totalQuestions + " - Difficoltà: " + allQuestions.get(i).difficulty.toUpperCase());
            System.out.println("═".repeat(70));

            boolean isCorrect = playQuestion(allQuestions.get(i));

            if (!isCorrect) {
                System.out.println("\n❌ RISPOSTA SBAGLIATA! Il gioco è terminato.");
                System.out.println("Hai risposto correttamente a " + correctAnswers + " domande.");
                break;
            }

            correctAnswers++;
            System.out.println("\n✓ RISPOSTA CORRETTA!");

            if (i < allQuestions.size() - 1) {
                System.out.print("Premi INVIO per la prossima domanda...");
                scanner.nextLine();
            }
        }

        // Risultato finale
        System.out.println("\n" + "═".repeat(70));
        if (correctAnswers == 10) {
            System.out.println("🎓 COMPLIMENTI " + playerName.toUpperCase() + "! SEI MATURATO! 🎓");
        } else {
            System.out.println("Gioco terminato. Hai risposto correttamente a " + correctAnswers + " domande su 10.");
        }
        System.out.println("═".repeat(70));

        // Salva statistiche
        saveStatistics(playerName, correctAnswers, used5050, usedAudience);

        System.out.println("\nLe tue statistiche sono state salvate!");
    }

    private static boolean playQuestion(APIQuestions question) {
        // Decodifica HTML entities
        String questionText = decodeHtml(question.question);
        System.out.println("\n" + questionText + "\n");

        // Crea lista di opzioni
        List<AnswerOption> options = new ArrayList<>();
        options.add(new AnswerOption(decodeHtml(question.correct_answer), true));
        for (String incorrect : question.incorrect_answers) {
            options.add(new AnswerOption(decodeHtml(incorrect), false));
        }

        // Mescola le opzioni
        Collections.shuffle(options);

        // Variabili per gli aiuti
        Set<Integer> hiddenOptions = new HashSet<>();

        while (true) {
            // Mostra opzioni
            char letter = 'A';
            for (int i = 0; i < options.size(); i++) {
                if (!hiddenOptions.contains(i)) {
                    System.out.println(letter + ") " + options.get(i).getText());
                }
                letter++;
            }

            System.out.print("\nLa tua risposta (A/B/C/D, H per aiuto, R per ritirarti): ");
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.equals("R")) {
                System.out.println("Hai scelto di ritirarti.");
                return false;
            }

            if (input.equals("H")) {
                if (!showHelpMenu(options, hiddenOptions)) {
                    continue;
                }
                continue;
            }

            if (input.length() == 1 && input.charAt(0) >= 'A' && input.charAt(0) <= 'D') {
                int selectedIndex = input.charAt(0) - 'A';

                if (hiddenOptions.contains(selectedIndex)) {
                    System.out.println("Questa opzione è stata eliminata. Scegline un'altra.");
                    continue;
                }

                return options.get(selectedIndex).isCorrect();
            }

            System.out.println("Input non valido. Riprova.");
        }
    }

    private static boolean showHelpMenu(List<AnswerOption> options, Set<Integer> hiddenOptions) {
        System.out.println("\n--- AIUTI DISPONIBILI ---");
        if (!used5050) {
            System.out.println("1) Guarda il bigliettino (50/50)");
        }
        if (!usedAudience) {
            System.out.println("2) Suggerimento dei compagni (pubblico)");
        }
        System.out.println("0) Torna alla domanda");

        if (used5050 && usedAudience) {
            System.out.println("Hai già usato tutti gli aiuti!");
            return false;
        }

        System.out.print("Scegli un aiuto: ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("0")) {
            return false;
        }

        if (choice.equals("1") && !used5050) {
            use5050(options, hiddenOptions);
            used5050 = true;
            return true;
        }

        if (choice.equals("2") && !usedAudience) {
            useAudience(options, hiddenOptions);
            usedAudience = true;
            return true;
        }

        System.out.println("Scelta non valida o aiuto già utilizzato.");
        return false;
    }

    private static void use5050(List<AnswerOption> options, Set<Integer> hiddenOptions) {
        System.out.println("\n🔍 GUARDA IL BIGLIETTINO (50/50)");
        System.out.println("Elimino 2 risposte sbagliate...\n");

        List<Integer> incorrectIndices = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            if (!options.get(i).isCorrect() && !hiddenOptions.contains(i)) {
                incorrectIndices.add(i);
            }
        }

        Collections.shuffle(incorrectIndices);

        for (int i = 0; i < Math.min(2, incorrectIndices.size()); i++) {
            hiddenOptions.add(incorrectIndices.get(i));
        }

        System.out.println("Due risposte sbagliate sono state eliminate!");
    }

    private static void useAudience(List<AnswerOption> options, Set<Integer> hiddenOptions) {
        System.out.println("\n👥 SUGGERIMENTO DEI COMPAGNI");
        System.out.println("Il pubblico ha votato:\n");

        Random random = new Random();
        int[] percentages = new int[4];

        // Trova l'indice della risposta corretta
        int correctIndex = -1;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).isCorrect()) {
                correctIndex = i;
                break;
            }
        }

        // La risposta corretta ha una probabilità più alta
        percentages[correctIndex] = 40 + random.nextInt(35); // 40-74%

        int remaining = 100 - percentages[correctIndex];
        List<Integer> otherIndices = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (i != correctIndex && !hiddenOptions.contains(i)) {
                otherIndices.add(i);
            }
        }

        for (int i = 0; i < otherIndices.size(); i++) {
            if (i == otherIndices.size() - 1) {
                percentages[otherIndices.get(i)] = remaining;
            } else {
                int value = random.nextInt(remaining / (otherIndices.size() - i));
                percentages[otherIndices.get(i)] = value;
                remaining -= value;
            }
        }

        char letter = 'A';
        for (int i = 0; i < options.size(); i++) {
            if (!hiddenOptions.contains(i)) {
                System.out.println(letter + ") " + percentages[i] + "%");
            }
            letter++;
        }

        System.out.println();
    }

    private static void saveStatistics(String playerName, int correctAnswers, boolean used5050, boolean usedAudience) {
        String filename = "statistics.json";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        List<PlayerStatistics> allStats = new ArrayList<>();

        // Leggi le statistiche esistenti
        File file = new File(filename);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<ArrayList<PlayerStatistics>>(){}.getType();
                List<PlayerStatistics> existing = gson.fromJson(reader, listType);
                if (existing != null) {
                    allStats = existing;
                }
            } catch (IOException e) {
                System.err.println("Errore nella lettura del file: " + e.getMessage());
            }
        }

        // Aggiungi nuova statistica
        allStats.add(new PlayerStatistics(playerName, correctAnswers, used5050, usedAudience));

        // Salva tutte le statistiche
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(allStats, writer);
        } catch (IOException e) {
            System.err.println("Errore nel salvataggio delle statistiche: " + e.getMessage());
        }
    }

    private static void showTitle() {
        System.out.println("Chi vuol essere maturato?");
    }

    private static String decodeHtml(String text) {
        if (text == null) return "";

        // Decodifica le entità HTML comuni
        return text.replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&rsquo;", "'")
                .replace("&ldquo;", "\"")
                .replace("&rdquo;", "\"")
                .replace("&eacute;", "é")
                .replace("&ntilde;", "ñ");
    }
}