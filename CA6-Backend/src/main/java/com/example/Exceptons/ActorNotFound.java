package com.example.Exceptons;

public class ActorNotFound extends Exception {
    public ActorNotFound() {
        super("No actor found with this code");
    }
}
