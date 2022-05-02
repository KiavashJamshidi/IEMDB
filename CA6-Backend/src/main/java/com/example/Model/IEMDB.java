package com.example.Model;

//import org.apache.http.HttpResponse;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
import com.example.Services.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
//import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.*;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IEMDB {
    public static List<Actor> actors = new ArrayList<>();
    public static List<Movie> movies = new ArrayList<>();

    public static List<User> users = new ArrayList<>();
    public static List<Comment> comments = new ArrayList<>();

    public static MovieService movieService = new MovieService();
    public static ActorService actorService = new ActorService();
    public static UserService userService = new UserService();
    public static CommentService commentService = new CommentService();
    public static RateService rateService = new RateService();
    public static CommentVoteService commentVoteService = new CommentVoteService();

    public static User loginUser;

    private static IEMDB instance;

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException | NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }

    public static IEMDB getInstance() throws IOException, ParseException {
        if (instance == null) {
            instance = new IEMDB();
            instance.initEntities();
        }
        return instance;
    }

    public void login(String email, String password){
        loginUser = userService.FindUserByEmail(email, users);
        if(loginUser != null){
            if(!loginUser.Password.equals(password))
                loginUser = null;
        }
    }

    public void logout(){
        loginUser = null;
    }

    public JSONArray Getter(String command) throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var usersReq = HttpRequest.newBuilder(
                URI.create("http://138.197.181.131:5000/api/" + command)
        ).build();

        HttpResponse<String> studentsRes = client.send(usersReq, HttpResponse.BodyHandlers.ofString());
        return new JSONArray(studentsRes.body());
    }

    public void initEntities() throws IOException, ParseException {
        try {
            JSONArray moviesJson = Getter("v2/movies");
            JSONArray usersJson = Getter("users");
            JSONArray commentsJson = Getter("comments");
            JSONArray actorsJson = Getter("v2/actors");
            for (int i = 0; i < actorsJson.length(); i++) {
                actorService.AddActor((JSONObject) actorsJson.get(i), actors);
            }
            System.out.println("actors done");
            for (int i = 0; i < usersJson.length(); i++) {
                userService.AddUser((JSONObject) usersJson.get(i), users);
            }
            System.out.println("users done");
            loginUser = users.get(0);;
            for (int i = 0; i < moviesJson.length(); i++) {
                movieService.AddMovie((JSONObject) moviesJson.get(i), movies, actors, actorService);
            }
            System.out.println("movies done");
            for (int i = 0; i < commentsJson.length(); i++) {
                commentService.AddCommentToMovie((JSONObject) commentsJson.get(i), users, movies, comments);
            }
            System.out.println("comments done");

        }catch (Exception e){ System.out.println("exception midam");}

    }


    public int AddToWatchlistGet(String userId, String movieId) {
        if(!isInteger(userId) || !isInteger(movieId))
            return -1;

        int userIndex = userService.FindUserIndexById(Integer.parseInt(userId), users);
        if (userIndex == -1)
            return -1;

        User user = users.get(userIndex);
        JSONObject newJSONObject = new JSONObject();
        newJSONObject.put("userEmail", user.Email);
        newJSONObject.put("movieId", movieId);
        JSONObject result = userService.AddToWatchList(newJSONObject, users, movies);
        if(result.get("data").equals("AgeLimitError"))
            return -2;
        if (result.get("success").equals(false))
            return -1;
        return 0;
    }

    public boolean RemoveFromWatchlist(String userId, String movieId) {
        if(!isInteger(userId) || !isInteger(movieId))
            return false;

        int userIndex = userService.FindUserIndexById(Integer.parseInt(userId), users);
        if (userIndex == -1)
            return false;

        User user = users.get(userIndex);
        JSONObject newJSONObject = new JSONObject();
        newJSONObject.put("userEmail", user.Email);
        newJSONObject.put("movieId", movieId);
        JSONObject result = userService.RemoveFromWatchList(newJSONObject, users, movies);

        return !result.get("success").equals(false);
    }

    public boolean AddRateToMovie(String userId, String movieId, String rate)  {
        if(!isInteger(userId) || !isInteger(movieId) || !isInteger(rate))
            return false;

        int userIndex = userService.FindUserIndexById(Integer.parseInt(userId), users);
        if(userIndex == -1)
            return false;
        User user = users.get(userIndex);
        JSONObject newJSONObject = new JSONObject();
        newJSONObject.put("userEmail", user.Email);
        newJSONObject.put("movieId", movieId);
        newJSONObject.put("score", rate);

        JSONObject result = rateService.AddRateToMovie(newJSONObject, users, movies);
        return !result.get("success").equals(false);
    }

    public boolean VoteComment(String userId, String commentId, String vote)  {
        if(!isInteger(userId) || !isInteger(commentId) || !isInteger(vote))
            return false;
        int userIndex = userService.FindUserIndexById(Integer.parseInt(userId), users);
        if(userIndex == -1)
            return false;
        User user = users.get(userIndex);
        JSONObject newJSONObject = new JSONObject();
        newJSONObject.put("userEmail", user.Email);
        newJSONObject.put("commentId", commentId);
        newJSONObject.put("vote", vote);

        JSONObject result = commentVoteService.VoteComment(newJSONObject, users, comments);
        return !result.get("success").equals(false);
    }

    public void setScoreRecommendations(){
        for (Movie movie : movies) {
            movieService.GenreSimilarity(loginUser.watchList, movie);
        }
    }

    public List<Movie> showTop3Recommendations() {
        List<Movie> recommends = new ArrayList<>();
        setScoreRecommendations();
        movies.sort((o1, o2) -> Float.compare(o2.ScoreRecommendation, o1.ScoreRecommendation));
        for (Movie movie : movies) {
            if (recommends.size() == 3)
                break;
            if (!loginUser.watchList.contains(movie))
                recommends.add(movie);
        }
        return recommends;
    }
}
