package com.example.Services;

import com.example.Model.ErrorHandler;
import com.example.Model.Movie;
import com.example.Model.Rate;
import com.example.Model.User;
import com.example.Repository.IemdbRepository;
import org.json.JSONObject;

import java.util.List;

public class RateService {
    UserService userService = new UserService();
    MovieService movieService = new MovieService();
    ErrorHandler errorHandler = new ErrorHandler();

    public JSONObject AddRateToMovie(JSONObject jsonObject) throws Exception {
        String movieId = jsonObject.getString("movieId");
        Movie movie = IemdbRepository.getInstance().findMovie(movieId);
        if(movie == null)
            throw new Exception("movie not found");

        String userEmail = jsonObject.get("userEmail").toString();

        int score = jsonObject.getInt("score");
        if (score < 1 || score > 10) return errorHandler.fail("InvalidRateScore");

        Rate newRate = new Rate(
                userEmail,
                movie.Id,
                score
        );
        IemdbRepository.getInstance().insertRate(newRate);
        return errorHandler.success("movie rated successfully");
    }
}