package com.example.Exceptions;

public class ActorNotFound extends Exception {
    public ActorNotFound() {
        super("No actor found with this code");
    }
}
