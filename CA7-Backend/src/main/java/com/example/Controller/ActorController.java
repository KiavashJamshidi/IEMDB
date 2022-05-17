package com.example.Controller;

import com.example.Exceptions.ActorNotFound;
import com.example.Model.IEMDB;
import com.example.Model.Actor;
import com.example.Model.Movie;
import org.springframework.web.bind.annotation.*;
import com.example.Repository.IemdbRepository;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/actors")
public class ActorController {
    @GetMapping("/{actorId}")
    public Actor getActor(@PathVariable("actorId") String id) throws ActorNotFound, Exception {
        System.out.println("Actor controller started: getActor");
        IemdbRepository repo = IemdbRepository.getInstance();
        Actor actor = repo.findActor(id);
        if (actor == null) {
            System.out.println("Actor not Found");
            throw new ActorNotFound();
        }
        return actor;
    }

    @PostMapping("/{actorId}/moviesActed")
    public List<Movie> getActorMovies(@PathVariable("actorId") String id) throws ActorNotFound, Exception {
        int actorId = Integer.parseInt(id);
        System.out.println("Actor controller started: getActorMovies");
        IemdbRepository repo = IemdbRepository.getInstance();
        try {
            return IemdbRepository.getInstance().getActorMovies(actorId);
        } catch (Exception e) {
            throw new ActorNotFound();
        }
    }
}