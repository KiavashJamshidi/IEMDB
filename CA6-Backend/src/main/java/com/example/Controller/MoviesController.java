package com.example.Controller;

import com.example.Exceptions.MovieNotFound;
import com.example.Model.Actor;
import com.example.Model.Comment;
import com.example.Model.IEMDB;
import com.example.Model.Movie;
import com.example.Repository.IemdbRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MoviesController {

    @GetMapping("/movies/{movieId}")
    public Movie getMovie(@PathVariable("movieId") String id) throws Exception {
        System.out.println("Movie controller started: getMovie");
        IemdbRepository repo = IemdbRepository.getInstance();
        Movie movie = repo.findMovie(id);

        if (movie == null) {
            System.out.println("Movie not Found");
            throw new MovieNotFound();
        }
        return movie;
    }

    @GetMapping("/movies/{movieId}/actors")
    public List<Actor> getMovieActors(@PathVariable("movieId") String id) throws Exception {
        System.out.println("Movie controller started: getMovieActors");
        return IemdbRepository.getInstance().getMovieActors(id);
    }

    @GetMapping("/movies/{movieId}/comments")
    public List<Comment> getMovieComments(@PathVariable("movieId") String id) throws Exception {
        System.out.println("Movie controller started: getMovieComments");
        return IemdbRepository.getInstance().getMovieComments(id);
    }

    @GetMapping("/movies")
    public List<Movie>  getMovies() throws Exception {
        System.out.println("Movie controller started: getMovies");
        IemdbRepository repo = IemdbRepository.getInstance();
        return repo.getAllMovies();
    }

    @PostMapping("/movies/{movieId}/addComment")
    public JsonNode addComment(@PathVariable("movieId") String id, @RequestBody JsonNode body) {
        System.out.println("Movie controller started: addComment");
        int movieId = Integer.parseInt(id);
        try{
            IEMDB.getInstance().commentService.AddCommentToMovie(
                    movieId,
                    IEMDB.getInstance().loginUser.Email,
                    body.get("text").asText()
            );
            return createSuccessResponse("comment is added to the movie successfully");
        }catch (Exception e){
            return createFailureResponse("comment not added!");
        }
    }

    @PostMapping("/movies/{movieId}/rate")
    public JsonNode rateMovie(@PathVariable("movieId") String id, @RequestBody JsonNode body){
        System.out.println("Movie controller started: rateMovie");
        String rateValue = body.get("RateValue").asText();
        try {
            IEMDB.getInstance().AddRateToMovie(String.valueOf(IEMDB.getInstance().loginUser.Id), id, rateValue);
            return createSuccessResponse("movie is rated successfully");
        }catch (Exception e){
            return createFailureResponse("rate not added!");
        }
    }

    @GetMapping("/movies/{movieId}/rates")
    public List<Float> getMovieRates(@PathVariable("movieId") String id) throws Exception {
        System.out.println("Movie controller started: getMovieRates");
//        try {
            return IemdbRepository.getInstance().getMovieRates(id);
//        }catch (Exception e){
//            return new ArrayList<>();
//        }
    }

    @PostMapping("/movies/{movieId}/addToWatchlist")
    public JsonNode addMovieToWatchlist(@PathVariable("movieId") String movieId) throws Exception {
        System.out.println("Movie controller started: addMovieToWatchlist");
        int userId = IEMDB.getInstance().loginUser.Id;
        try{
            IEMDB.getInstance().AddToWatchlistGet(String.valueOf(userId), movieId);
            return createSuccessResponse("movie is added to the watchlist successfully");
        }catch (Exception e){
            return createFailureResponse("could not add movie to watchlist!");
        }
    }

    @GetMapping("/watchlist")
    public List<Movie> getWatchlist() {
        System.out.println("Movie controller started: getWatchlist");
        try {
            List<Movie> showWatchlist = IemdbRepository.getInstance().getWatchlist(IEMDB.getInstance().loginUser.Id);
            showWatchlist.sort((o1, o2) -> Float.compare(o2.IMDBRate, o1.IMDBRate));
            return showWatchlist;
        }catch (Exception e){
            return new ArrayList<>();
        }
    }

    @PostMapping("/watchlist/{movieId}/remove")
    public JsonNode removeMovieFromWatchlist(@PathVariable("movieId") String movieId) throws Exception {
        System.out.println("Movie controller started: removeMovieFromWatchlist");
        try{
            IemdbRepository.getInstance().removeFromWatchlist(String.valueOf(IEMDB.getInstance().loginUser.Id), movieId);
            return createSuccessResponse("movie is removed from the watchlist successfully");
        }catch (Exception e){
            return createFailureResponse("could not remove movie from watchlist!");
        }
    }

    @PostMapping("/voteComment/{commentId}/{vote}")
    public JsonNode voteComment(@PathVariable("commentId") String commentId,
                            @PathVariable("vote") String vote) throws Exception {
        System.out.println("Movie controller started: voteComment");
        int userId = IEMDB.getInstance().loginUser.Id;
        try{
            IEMDB.getInstance().VoteComment(String.valueOf(userId), commentId, vote);
            return createSuccessResponse("vote is added to the comment successfully");
        }catch (Exception e){
            return createFailureResponse("could not vote comment!");
        }
    }

    @GetMapping("/voteComment/{commentId}/likes")
    public int getCommentLikes(@PathVariable("commentId") String commentId) throws Exception {
        System.out.println("Movie controller started: getCommentLikes");
        try{
            return IemdbRepository.getInstance().getCommentLikeOrDislikes(commentId, 1);
        }catch (Exception e){
            return 0;
        }
    }
    @GetMapping("/voteComment/{commentId}/dislikes")
    public int getCommentDisLikes(@PathVariable("commentId") String commentId) throws Exception {
        System.out.println("Movie controller started: getCommentDisLikes");
        try{
            return IemdbRepository.getInstance().getCommentLikeOrDislikes(commentId, -1);
        }catch (Exception e){
            return 0;
        }
    }

    @GetMapping("/movies/searchByDate/{search}")
    public List<Movie> searchByDate(@PathVariable("search") String search) throws Exception {
        System.out.println("Movie controller started: searchByDate");
        String[] splited = search.split("-");
        String start_year = splited[0];
        String end_year = splited[1];
        return IemdbRepository.getInstance().searchMovieByDate(start_year, end_year);
    }

    @GetMapping("/movies/searchByName/{name}")
    public List<Movie> searchByName(@PathVariable("name") String name) throws Exception {
        System.out.println("Movie controller started: searchByName");
        return IemdbRepository.getInstance().searchMovieByName(name);
    }

    @GetMapping("/movies/searchByGenre/{genre-name}")
    public List<Movie> searchByGenre(@PathVariable("genre-name") String genreName) throws Exception {
        System.out.println("Movie controller started: searchByGenre");
        return IemdbRepository.getInstance().searchMovieByGenre(genreName);
    }

    @PostMapping("/movies/sortByImdbRate")
    public List<Movie> sortByImdbRate() throws Exception {
        System.out.println("Movie controller started: sortByImdbRate");
        List<Movie> showMovies = IemdbRepository.getInstance().getAllMovies_SortedBy("imdbRate");
        return showMovies;
    }

    @PostMapping("/movies/sortByReleaseDate")
    public List<Movie> sortByReleaseDate() throws Exception {
        System.out.println("Movie controller started: sortByReleaseDate");
        List<Movie> showMovies = IemdbRepository.getInstance().getAllMovies_SortedBy("releaseDate");
        return showMovies;
    }

    @PostMapping("/recommendations")
    public List<Movie> getRecommendedMovies(){
        System.out.println("Movie controller started: recommendations");
        try {
            return IEMDB.getInstance().showTop3Recommendations();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private JsonNode createSuccessResponse(String msg) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode resp = objectMapper.createObjectNode();
        resp.put("success", "true");
        resp.put("message", msg);
        return resp;
    }

    private JsonNode createFailureResponse(String msg) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode resp = objectMapper.createObjectNode();
        resp.put("success", "false");
        resp.put("message", msg);
        return resp;
    }

}