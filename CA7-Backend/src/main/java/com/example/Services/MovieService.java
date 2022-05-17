package com.example.Services;

import com.example.Model.Actor;
import com.example.Model.ErrorHandler;
import com.example.Model.Functions;
import com.example.Model.Movie;
import com.example.Repository.IemdbRepository;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MovieService {
    ErrorHandler errorHandler = new ErrorHandler();
    public Integer FindMovieIndex(Integer id, List<Movie> movies){
        for(Movie movie : movies)
            if(movie.Id.equals(id))
                return movies.indexOf(movie);
        return -1;
    }
    public boolean IsCastValid(List<Integer> cast, List<Actor> actors, ActorService actorService){
        for (Integer actorId : cast)
            if (actorService.FindActorIndex(actorId, actors) == -1)
                return false;
        return true;
    }

    public JSONObject AddMovie(JSONObject jsonObject, List<Movie> movies, List<Actor> actors, ActorService actorService) throws java.text.ParseException, IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        JSONArray writers = jsonObject.getJSONArray("writers");
        JSONArray genres = jsonObject.getJSONArray("genres");

        String cast = jsonObject.get("cast").toString();
        List<String> writersList = Functions.ConvertStringToStringList(writers);
        List<String> genresList = Functions.ConvertStringToStringList(genres);
        List<Integer> castList = Functions.ConvertStringToIntList(cast);
        if (!IsCastValid(castList, actors, actorService)) return errorHandler.fail("ActorNotFound");

        Date myDate;
        try {
            myDate = formatter.parse(jsonObject.getString("releaseDate"));
        } catch (Exception e){
            return errorHandler.fail("ActorNotFound");
        }

        Movie newMovie = new Movie(
                Integer.parseInt(jsonObject.get("id").toString()),
                jsonObject.get("name").toString(),
                Integer.parseInt(jsonObject.get("ageLimit").toString()),
                Float.parseFloat(jsonObject.get("imdbRate").toString()),
                jsonObject.get("summary").toString(),
                jsonObject.get("director").toString(),
                Integer.parseInt(jsonObject.get("duration").toString()),
                myDate,
                writersList,
                genresList,
                castList,
                jsonObject.getString("image"),
                jsonObject.getString("coverImage")
        );

        Integer movieId = Integer.parseInt(jsonObject.get("id").toString());
        Integer movieIndex = FindMovieIndex(movieId, movies);
        if (movieIndex != -1) return UpdateMovie(movieIndex, newMovie, movies);

        movies.add(newMovie);

        return errorHandler.success("movie added successfully");
    }

    public JSONObject UpdateMovie(Integer movieIndex, Movie movie, List<Movie> movies) throws java.text.ParseException {
        movies.set(movieIndex, movie);
        return errorHandler.success("movie updated successfully");
    }

    public List<Movie> SearchMoviesByDate(int start_year, int end_year,List<Movie> movies) throws ParseException {
        List<Movie> moviesInRange = new ArrayList<Movie>();
        for (Movie movie : movies) {
            Date releaseDate = new SimpleDateFormat("yyyy-MM-dd").parse(movie.ReleaseDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(releaseDate);

            if (calendar.get(Calendar.YEAR) < end_year && calendar.get(Calendar.YEAR) > start_year)
                moviesInRange.add(movie);

        }
        return moviesInRange;
    }

    public List<Movie> SearchMoviesByName(String movieName,List<Movie> movies) throws ParseException {
        List<Movie> showMovies = new ArrayList<>();
        for (Movie movie : movies)
            if (movie.Name.contains(movieName))
                showMovies.add(movie);
        return showMovies;
    }

    public List<Movie> SearchMoviesByGenre(String movieGenre, List<Movie> movies) throws java.text.ParseException {
        List<Movie> moviesList = new ArrayList<>();
        for (Movie movie : movies)
            if (movie.HasGenre(movieGenre))
                moviesList.add(movie);
        return moviesList;
    }


    public Movie GetMovieByID(int id, List<Movie> movies) throws ParseException {
        for (Movie movie : movies)
            if (movie.Id == id) {
                return movie;
            }
        return null;
    }

    public float GenreSimilarity(List<Movie> watchList, Movie movie){
        float genreSimilarity = 0;
        for (Movie mv : watchList) {
            try {
                List<String> temp = IemdbRepository.getInstance().getMovieGenres(String.valueOf(mv.Id));
                temp.retainAll(IemdbRepository.getInstance().getMovieGenres(String.valueOf(movie.Id)));
                genreSimilarity += temp.size();
            } catch (Exception e) {
               return 0;
            }
        }
        movie.ScoreRecommendation = 3 * genreSimilarity + movie.IMDBRate + ((movie.Score == -1) ? 0 : movie.Score);
        return movie.ScoreRecommendation;
    }
}
