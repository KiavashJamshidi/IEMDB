package com.example.Model;

import java.awt.*;
import java.io.IOException;
import java.text.ParseException;
import com.example.Services.ActorService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Movie implements Comparable<Movie>{
    @Getter @Setter public Integer Id, AgeLimit, Duration;
    @Getter @Setter public float IMDBRate;
    @Getter @Setter public String Name, Summary, Director;
    @Getter @Setter public String ReleaseDate;
    @Getter @Setter public List<String> Writers;
    @Getter @Setter public List<String> Genres;
    @Getter @Setter public List<Integer> Cast;
    @Getter @Setter public List<Comment> Comments;
    @Getter @Setter public List<Rate> Rates;
    @Getter @Setter public List<Actor> Actors;
    @Getter @Setter public BigDecimal Score;
    @Getter @Setter public String CoverImage;
    @Getter @Setter public String Image;

    @JsonIgnore
    public DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Getter @Setter public Date DateReleaseDate;
    @Getter @Setter public float ScoreRecommendation;

    public Movie(Integer id, String name, Integer ageLimit, float imdbRate, String summary, String director,
                 Integer duration, Date releaseDate, List<String> writers, List<String> genres, List<Integer> cast, String img,String Coverimg) throws ParseException, IOException {
        Id = id;
        Name = name;
        IMDBRate = imdbRate;
        AgeLimit = ageLimit;
        Summary = summary;
        Director = director;
        Duration = duration;
        ReleaseDate = dateFormat.format(releaseDate);
        DateReleaseDate = dateFormat.parse(ReleaseDate);
        Writers = writers;
        Genres = genres;
        Cast = cast;
        Actors = setActors(cast);
        Comments = new ArrayList<>();
        Rates = new ArrayList<>();
        Score = null;
        ScoreRecommendation = 0;
        Image = img;
        CoverImage = Coverimg;
    }

    public List<Actor> setActors(List<Integer> cast) throws IOException, ParseException {
        List<Actor> actors = new ArrayList<>();
        for (int castId : cast) {
            actors.add(IEMDB.getInstance().actorService.FindActor(castId, IEMDB.getInstance().actors));
        }
        return actors;
    }

    public void AddComment(Comment comment){
        Comments.add(comment);
    }

    public void AddRate(Rate rate){
        Rates.add(rate);
        UpdateScores();
    }
    private void UpdateScores(){
        double sum = 0;
        for(Rate rate : Rates){
            sum += rate.Score;
        }
        DecimalFormat df = new DecimalFormat("0.00");
        df.setRoundingMode(RoundingMode.DOWN);
        BigDecimal ans = BigDecimal.valueOf(sum / Rates.size());
        Score = ans.setScale(1, RoundingMode.HALF_UP);
    }

    public boolean checkForRateUpdates(String userEmail, Integer score){
        for(Rate rate : Rates){
            if(rate.UserEmail.equals(userEmail)) {
                rate.Score = score;
                UpdateScores();
                return true;
            }
        }
        return false;
    }
    public boolean HasGenre(String genre){
        return Genres.contains(genre);
    }

    public List<String> GetCast(ActorService actorService, List<Actor> actors){
        List<String> actorNames = new ArrayList<String>();
        for(int actorId:Cast){
           int actorIndex = actorService.FindActorIndex(actorId, actors);
           if(actorIndex != -1)
               actorNames.add(actors.get(actorIndex).Name);
           else
               actorNames.add("");
        }
        return actorNames;
    }

    public List<Actor> GetCastObject(ActorService actorService, List<Actor> actors){
        List<Actor> findActors = new ArrayList<>();
        for(int actorId : Cast){
            int actorIndex = actorService.FindActorIndex(actorId, actors);
            if(actorIndex != -1)
                findActors.add(actors.get(actorIndex));
        }
        return findActors;
    }


    public boolean ActorExists(int actorId){
        for(Integer id : Cast){
            if(id == actorId)
                return true;
        }
        return false;
    }

    public boolean GenreExists(String genre){
        for(String g : Genres){
            if(g.equals(genre))
                return true;
        }
        return false;
    }

    @Override
    public int compareTo(Movie o) {
        return DateReleaseDate.compareTo(o.DateReleaseDate);
    }
}
