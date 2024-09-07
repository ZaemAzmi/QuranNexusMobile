package com.example.qurannexus.interfaces;
import com.example.qurannexus.models.Joke;
import retrofit2.Call;
import retrofit2.http.GET;
public interface JokeApi {
    @GET("/random_joke")
    Call<Joke> getRandomJoke();
}
