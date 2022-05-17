package com.example.Services;

import com.example.Model.Actor;
import com.example.Model.ErrorHandler;
import com.example.Model.Movie;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ActorService {
    ErrorHandler errorHandler = new ErrorHandler();

    public JSONObject AddActor(JSONObject jsonObject, List<Actor> actors) throws java.text.ParseException {
        Actor newActor = new Actor(
                jsonObject.getInt("id"),
                jsonObject.getString("name"),
                jsonObject.getString("birthDate"),
                jsonObject.getString("nationality"),
                jsonObject.getString("image")
        );

        Integer actorId = Integer.parseInt(jsonObject.get("id").toString());
        Integer actorIndex = FindActorIndex(actorId, actors);
        if (actorIndex != -1) return UpdateActor(actorIndex, newActor, actors);
        actors.add(newActor);
        return errorHandler.success("actor added successfully");
    }

    public JSONObject UpdateActor(Integer actorIndex, Actor actor, List<Actor> actors) throws java.text.ParseException {
        actors.set(actorIndex, actor);
        return errorHandler.success("actor updated successfully");
    }

    public Integer FindActorIndex(Integer id, List<Actor> actors){
        for(Actor actor : actors)
            if(actor.Id.equals(id))
                return actors.indexOf(actor);
        return -1;
    }

    public Actor FindActor(Integer id, List<Actor> actors){
        for(Actor actor : actors)
            if(actor.Id.equals(id))
                return actor;

        return null;
    }

    public List<Movie> moviesOfActor(int id, List<Movie> movies) {
        List<Movie> moviesActed = new ArrayList<>();
        for (Movie movie : movies)
            if (movie.Cast.contains(id))
                moviesActed.add(movie);
        return moviesActed;
    }

}
