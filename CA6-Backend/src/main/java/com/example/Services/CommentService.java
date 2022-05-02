package com.example.Services;

import com.example.Model.Comment;
import com.example.Model.ErrorHandler;
import com.example.Model.Movie;
import com.example.Model.User;
import org.json.JSONObject;

import java.util.List;

public class CommentService {
    ErrorHandler errorHandler = new ErrorHandler();

    public  JSONObject AddCommentToMovie(JSONObject jsonObject, List<User> users, List<Movie> movies, List<Comment> comments){
        UserService userService = new UserService();
        MovieService movieService = new MovieService();
        Integer movieId = Integer.parseInt(jsonObject.get("movieId").toString());
        Integer movieIndex = movieService.FindMovieIndex(movieId, movies);
        String userEmail = jsonObject.get("userEmail").toString();

        if (!userService.UserExists(userEmail, users)) return errorHandler.fail("UserNotFound");
        if (movieIndex == -1) return errorHandler.fail("MovieNotFound");

        Comment newComment = new Comment(
                comments.size() + 1,
                userEmail,
                movieId,
                jsonObject.get("text").toString()
        );
        movies.get(movieIndex).AddComment(newComment);
        comments.add(newComment);
        return errorHandler.success("comment with id "+ newComment.Id.toString()  +" added successfully");
    }

    public void AddCommentToMovie(int movieId, String userEmail, String text, List<Movie> movies, List<Comment> comments){
        UserService userService = new UserService();
        MovieService movieService = new MovieService();
        Integer movieIndex = movieService.FindMovieIndex(movieId, movies);

        Comment newComment = new Comment(
                comments.size() + 1,
                userEmail,
                movieId,
                text
        );
        movies.get(movieIndex).AddComment(newComment);
        comments.add(newComment);
    }

    public Integer FindCommentIndex(Integer id, List<Comment> comments){
        for (Comment comment : comments)
            if (comment.Id.equals(id))
                return comments.indexOf(comment);

        return -1;
    }
}