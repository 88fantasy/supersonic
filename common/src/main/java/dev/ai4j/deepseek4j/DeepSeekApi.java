package dev.ai4j.deepseek4j;


import dev.ai4j.deepseek4j.chat.ChatCompletionRequest;
import dev.ai4j.deepseek4j.chat.ChatCompletionResponse;
import dev.ai4j.deepseek4j.models.ModelsResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.Map;

public interface DeepSeekApi {

    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    Call<ChatCompletionResponse> chatCompletions(@Body ChatCompletionRequest request);

    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    Call<ChatCompletionResponse> chatCompletions(@HeaderMap Map<String, String> headers,
                                                 @Body ChatCompletionRequest request);

    @GET("models")
    @Headers("Content-Type: application/json")
    Call<ModelsResponse> models(@HeaderMap Map<String, String> headers);
}
