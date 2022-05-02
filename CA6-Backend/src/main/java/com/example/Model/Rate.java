package com.example.Model;

public class Rate {
    String UserEmail;
    Integer MovieId;
    public float Score;
    public Rate(String userEmail, Integer movieId, Integer score){
        UserEmail = userEmail;
        MovieId = movieId;
        Score = score;
    }

}
