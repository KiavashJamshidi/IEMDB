package com.example.Exceptons;

public class MovieNotFound extends Exception {
    public MovieNotFound() {
        super("No movie found with this code");
    }
}
