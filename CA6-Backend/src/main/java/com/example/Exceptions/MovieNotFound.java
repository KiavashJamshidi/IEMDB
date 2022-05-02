package com.example.Exceptions;

public class MovieNotFound extends Exception {
    public MovieNotFound() {
        super("No movie found with this code");
    }
}
