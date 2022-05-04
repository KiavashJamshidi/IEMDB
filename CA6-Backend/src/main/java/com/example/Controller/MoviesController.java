package com.example.Controller;

import com.example.Exceptions.MovieNotFound;
import com.example.Model.Actor;
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
    public JsonNode rateMovie(@PathVariable("movieId") String id, @RequestBody JsonNode body) throws Exception {
        System.out.println("Movie controller started!");
        String rateValue = body.get("RateValue").asText();
        IEMDB.getInstance().AddRateToMovie(String.valueOf(IEMDB.getInstance().loginUser.Id), id, rateValue);
        return createSuccessResponse("movie is rated successfully");
    }

    @PostMapping("/movies/{movieId}/addToWatchlist")
    public JsonNode addMovieToWatchlist(@PathVariable("movieId") String movieId) throws Exception {
        System.out.println("Movie controller started!");
        int userId = IEMDB.getInstance().loginUser.Id;
        IEMDB.getInstance().AddToWatchlistGet(String.valueOf(userId), movieId);
        return createSuccessResponse("movie is added to the watchlist successfully");
    }

    @GetMapping("/watchlist")
    public List<Movie> getWatchlist() throws Exception {
        System.out.println("Movie controller started!");
        List<Movie> showWatchlist = IEMDB.getInstance().loginUser.watchList;
        showWatchlist.sort((o1, o2) -> Float.compare(o2.IMDBRate, o1.IMDBRate));
        return showWatchlist;
    }

    @PostMapping("/watchlist/{movieId}/remove")
    public JsonNode removeMovieFromWatchlist(@PathVariable("movieId") String movieId) throws Exception {
        System.out.println("Movie controller started!");
        IEMDB.getInstance().RemoveFromWatchlist(String.valueOf(IEMDB.getInstance().loginUser.Id), movieId);
        return createSuccessResponse("movie is removed from the watchlist successfully");
    }

    @PostMapping("/voteComment/{commentId}/{vote}")
    public JsonNode voteComment(@PathVariable("commentId") String commentId,
                            @PathVariable("vote") String vote) throws Exception {
        System.out.println("Movie controller started!");
        int userId = IEMDB.getInstance().loginUser.Id;
        IEMDB.getInstance().VoteComment(String.valueOf(userId), commentId, vote);
        return createSuccessResponse("vote is added to the comment successfully");
    }

    @GetMapping("/movies/searchByDate/{search}")
    public List<Movie> searchByDate(@PathVariable("search") String search) throws Exception {
        System.out.println("Movie controller started!");
        String[] splited = search.split("-");
        String start_year = splited[0];
        String end_year = splited[1];
        return IEMDB.getInstance().movieService.SearchMoviesByDate(
            Integer.parseInt(start_year),Integer.parseInt(end_year),IEMDB.getInstance().movies);
    }

    @GetMapping("/movies/searchByName/{name}")
    public List<Movie> searchByName(@PathVariable("name") String name) throws Exception {
        System.out.println("Movie controller started!");
        return IEMDB.getInstance().movieService.SearchMoviesByName(name, IEMDB.getInstance().movies);
    }

    @GetMapping("/movies/searchByGenre/{genre-name}")
    public List<Movie> searchByGenre(@PathVariable("genre-name") String genreName) throws Exception {
        System.out.println("Movie controller started!");
        return IEMDB.getInstance().movieService.SearchMoviesByGenre(genreName, IEMDB.getInstance().movies);
    }


    @PostMapping("/movies/sortByImdbRate")
    public List<Movie> sortByImdbRate() throws IOException, ParseException {
        System.out.println("Movie controller started!");
        List<Movie> showMovies = IEMDB.getInstance().movies;
        showMovies.sort((o1, o2) -> Float.compare(o2.IMDBRate, o1.IMDBRate));
        return showMovies;
    }

    @PostMapping("/movies/sortByReleaseDate")
    public List<Movie> sortByReleaseDate() throws IOException, ParseException {
        System.out.println("Movie controller started!");
        List<Movie> showMovies = IEMDB.getInstance().movies;
        Collections.sort(showMovies, Collections.reverseOrder());
        return showMovies;
    }

    @PostMapping("/recommendations")
    public List<Movie> getRecommendedMovies() throws IOException, ParseException {
        System.out.println("Movie controller started...recommendations");
        List<Movie> recommendedMovies = IEMDB.getInstance().showTop3Recommendations();
        System.out.println(recommendedMovies);
        return recommendedMovies;
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

    private JsonNode serializeMovies(List<Movie> movies) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode moviesArr = objectMapper.createArrayNode();
        for (Movie movie : movies)
            moviesArr.add(objectMapper.valueToTree(movie));

        ObjectNode result = objectMapper.createObjectNode();
        result.set("movies", moviesArr);
        return result;
    }
}