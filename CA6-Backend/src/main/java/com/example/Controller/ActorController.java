package com.example.Controller;

import com.example.Exceptions.ActorNotFound;
import com.example.Model.IEMDB;
import com.example.Model.Actor;
import com.example.Model.Movie;
import org.springframework.web.bind.annotation.*;
import com.example.Repository.IemdbRepository;
import java.util.List;

@RestController
@RequestMapping("/api/actors")
public class ActorController {
    @GetMapping("/{actorId}")
    public Actor getActor(@PathVariable("actorId") String id) throws ActorNotFound, Exception {
        Integer actorId = Integer.parseInt(id);
        System.out.println("Actor controller started!");
        IemdbRepository repo = IemdbRepository.getInstance();
        if (IEMDB.getInstance().actorService.FindActorIndex(actorId, IEMDB.getInstance().actors) == -1) {
            System.out.println("Actor not Found");
            throw new ActorNotFound();
        }
        return IEMDB.getInstance().actorService.FindActor(actorId, IEMDB.getInstance().actors);
    }

    @PostMapping("/{actorId}/moviesActed")
    public List<Movie> getActorMovies(@PathVariable("actorId") String id) throws ActorNotFound, Exception {
        Integer actorId = Integer.parseInt(id);
        System.out.println("Actor controller started!");
        if (IEMDB.getInstance().actorService.FindActorIndex(actorId, IEMDB.getInstance().actors) == -1) {
            System.out.println("Actor not Found");
            throw new ActorNotFound();
        }
        return IEMDB.getInstance().actorService.moviesOfActor(Integer.parseInt(id),IEMDB.getInstance().movies);
    }
}