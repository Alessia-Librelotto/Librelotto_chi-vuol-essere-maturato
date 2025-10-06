import com.google.gson.Gson;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class APIClient {
    private final HttpClient client = HttpClient.newHttpClient();

    public List<APIQuestions> fetchQuestions(int amount, String type, String difficulty) {
        String url = "https://opentdb.com/api.php?amount=" + amount + "&difficulty=" + difficulty + "&type=" + type;

        System.out.println("Caricamento " + amount + " domande " + difficulty + "...");

        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/json")
                .uri(java.net.URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Status code: " + response.statusCode());
        } catch (IOException | InterruptedException e) {
            System.err.println("Errore nella richiesta API: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }

        try {
            Gson gson = new Gson();
            APIResponse apiResponse = gson.fromJson(response.body(), APIResponse.class);

            if (apiResponse == null) {
                System.err.println("APIResponse è null");
                return new ArrayList<>();
            }

            if (apiResponse.response_code != 0) {
                System.err.println("Errore API: response_code = " + apiResponse.response_code);
                return new ArrayList<>();
            }

            if (apiResponse.results == null) {
                System.err.println("Results è null");
                return new ArrayList<>();
            }

            System.out.println("Caricate " + apiResponse.results.size() + " domande " + difficulty);
            return apiResponse.results;

        } catch (Exception e) {
            System.err.println("Errore nel parsing JSON: " + e.getMessage());
            e.printStackTrace();
            System.err.println("Response body: " + response.body());
            return new ArrayList<>();
        }
    }
}