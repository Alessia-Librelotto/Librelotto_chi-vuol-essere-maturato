import com.google.gson.Gson;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class APIClient {
    private final HttpClient client =  HttpClient.newHttpClient();

    public String fetchQuestions(int amount, String type, String difficulty) {
        //https://opentdb.com/api.php?amount=5&difficulty=easy&type=multiple
        String url = "https://opentdb.com/api.php?amount=" + amount + "&difficulty=" + difficulty + "&type=" + type;

        //Design pattern builder
        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/json")
                .uri(java.net.URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }catch (IOException | InterruptedException e){
            return "Errore nella richiesta API";
        }

        Gson gson = new Gson();
        APIResponse apiResponse = gson.fromJson(response.body(), APIResponse.class);
        for(APIQuestions question : apiResponse.results){
            System.out.print(question.question);
            System.out.println(question.correct_answer);
        }
        return response.body();
    }

}
