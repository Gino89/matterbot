package org.matterbot.services.giphy;

import org.matterbot.services.URLQueryService;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.matterbot.services.giphy.response.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Random;

@Slf4j
@Service
public class GiphyService implements URLQueryService {

    private GiphyClient giphyClient;
    private static final int MAX_SEARCH_RESULT = 100;

    @NotNull
    @NotEmpty
    @NotBlank
    @Value("${giphy.client.apikey}")
    private String apiKey;

    @Autowired
    public GiphyService(GiphyClient giphyClient){
        this.giphyClient = giphyClient;
    }

    @Override
    public String getUrl(Strategy strategy) { return getUrl(strategy, ""); }

    @Override
    public String getUrl(Strategy strategy, String term) {
        Call<String> call = null;
        String jsonpath = "";

        switch (strategy){
            case RANDOM:
                call = giphyClient.getRandom(apiKey);
                jsonpath = "$.data.image_original_url";
                break;
            case TRENDING:
                call = giphyClient.getTrending(apiKey);
                jsonpath = "$.data[0].images.original.url";
                break;
            case SEARCH:
                Call<SearchResponse> callSearch = giphyClient.search(apiKey, term, MAX_SEARCH_RESULT);
                String url = "";
                try {
                    Response<SearchResponse> response = callSearch.execute();
                    if(response.isSuccessful()) {
                        SearchResponse body = response.body();
                        int random = getRandomNumberInRange(0, body.getData().length);
                        url = body.getData()[random].getImages().getOriginal().getUrl();
                    }else{
                        log.error("STATUS: {}, BODY: {}", response.code(), response.errorBody().string());
                    }
                }catch(IOException ex){
                    log.error(ex.toString());
                }
                return url;
            default:
                return "WHAT DO YOU WANT, DUDE?";
        }
        return queryCall(call, jsonpath);
    }

    private String queryCall(Call<String> call, String jsonPath){
        Response<String> callResult = null;
        try {
            callResult = call.execute();
            if (callResult.isSuccessful()) {

                DocumentContext jsonContext = JsonPath.parse(callResult.body());
                return jsonContext.read(jsonPath);
            } else {
                log.error("STATUS: {}, BODY: {}", callResult.code(), callResult.errorBody().string());
            }
        } catch (IOException e) {
            log.error(e.toString());
        }
        return "ERROR CALLING";
    }

    private static int getRandomNumberInRange(int min, int max) {
        Random r = new Random();
        return r.ints(min, max).findFirst().getAsInt();
    }
}
