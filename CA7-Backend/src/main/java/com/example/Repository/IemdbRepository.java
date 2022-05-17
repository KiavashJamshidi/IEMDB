package com.example.Repository;

import com.example.Model.*;
import org.apache.commons.dbutils.DbUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;

public class IemdbRepository {
    private static final String TABLE_NAME = "Movie";
    private static IemdbRepository instance;
    public static IemdbRepository getInstance() throws Exception {
        if (instance == null) {
            instance = new IemdbRepository();
        }
        return instance;
    }

    private IemdbRepository() throws Exception {
        Connection con = ConnectionPool.getConnection();
        Statement createTableStatement = con.createStatement();

        createTableStatement.addBatch(
            "CREATE TABLE IF NOT EXISTS Actor(id INT, name CHAR(225), " +
                    "birthDate CHAR(255), nationality CHAR(225), " +
                    "image CHAR(225), PRIMARY KEY(id));"
        );

        createTableStatement.addBatch(
            "CREATE TABLE IF NOT EXISTS Movie(id INT, name CHAR(225), ageLimit INT, " +
                    "duration INT, imdbRate FLOAT, summary VARCHAR(10000), director CHAR(225), " +
                    "releaseDate CHAR(225), score INT, coverImage CHAR(225), image CHAR(225), writers CHAR(255), PRIMARY KEY(id));"
        );

        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS Actor_Movie(actorId INT, movieId INT, " +
                        "PRIMARY KEY(actorId, movieId), " +
                        "FOREIGN KEY (movieId) REFERENCES Movie(id), " +
                        "FOREIGN KEY (actorId) REFERENCES Actor(id));"
        );

        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS Movie_Genre(movieId INT, genre char(255), " +
                        "PRIMARY KEY(movieId, genre), " +
                        "FOREIGN KEY (movieId) REFERENCES Movie(id));"
        );

        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS Watchlist(userId INT, movieId INT, " +
                        "PRIMARY KEY(userId, movieId), " +
                        "FOREIGN KEY (userId) REFERENCES User(id)," +
                        "FOREIGN KEY (movieId) REFERENCES Movie(id));"
        );

        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS Comment(id INT, likes INT, dislikes INT, userEmail CHAR(22), " +
                        "text char(255), creationDate char(255), movieId INT, " +
                        "PRIMARY KEY(id)," +
                        "FOREIGN KEY (movieId) REFERENCES Movie(id));"
//                        "FOREIGN KEY (userEmail) REFERENCES User(email));"
        );
        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS CommentVote(userEmail CHAR(225), " +
                        "vote INT, commentId INT, " +
                        "PRIMARY KEY(userEmail, commentId), " +
                        "FOREIGN KEY (commentId) REFERENCES Comment(id), " +
                        "FOREIGN KEY (userEmail) REFERENCES User(email));"
        );
        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS Rate(userEmail CHAR(22), " +
                        "score float, movieId INT, " +
                        "PRIMARY KEY(userEmail, movieId), " +
                        "FOREIGN KEY (movieId) REFERENCES Movie(id));"
//                        "FOREIGN KEY (userEmail) REFERENCES User(email));"
        );
        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS User(id INT, email CHAR(22) UNIQUE, " +
                        "pass char(255), nickname char(255), name char(255), " +
                        "birthDate date, PRIMARY KEY(id));"
        );

        createTableStatement.executeBatch();
        createTableStatement.close();
        con.close();

        List<Movie> movies = IEMDB.getInstance().movies;
        List<Actor> actors = IEMDB.getInstance().actors;
        List<User> users = IEMDB.getInstance().users;
        List<Comment> comments = IEMDB.getInstance().comments;

        for (Actor actor : actors)
            insertActor(actor);

        for (Movie movie : movies) { //check ali for insert grade
            insertMovie(movie);
            insertMovieActors(movie);
            insertMovieGenres(movie);
        }

        for (Comment comment : comments)
            insertComment(comment);

        for (User user : users)
            insertUser(user);
    }



    protected String getInsertStatementMovie() {
        return "INSERT INTO Movie(id, name, ageLimit, duration, imdbRate, summary, director, releaseDate, score, coverImage, image, writers)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)"
            + "ON DUPLICATE KEY UPDATE id = id";
    }

    protected String getInsertStatementActor_Movie(){
        return "INSERT INTO Actor_Movie(actorId, movieId)"
                + " VALUES(?,?)"
                + "ON DUPLICATE KEY UPDATE actorId=actorId, movieId=movieId";
    }

    protected String getInsertStatementMovie_Genre(){
        return "INSERT INTO Movie_Genre(movieId, genre)"
                + " VALUES(?,?)"
                + "ON DUPLICATE KEY UPDATE movieId=movieId, genre=genre";
    }

    protected String getInsertStatementWatchlist(){
        return "INSERT INTO Watchlist(userId, movieId)"
                + " VALUES(?,?)"
                + "ON DUPLICATE KEY UPDATE movieId=movieId, userId=userId";
    }

    protected String getInsertStatementActor() {
        return "INSERT INTO Actor(id, name, birthDate, nationality, image)"
            + " VALUES(?,?,?,?,?)"
            + "ON DUPLICATE KEY UPDATE id = id";
    }

    protected String getInsertStatementComment() {
        return "INSERT INTO Comment(id, likes, dislikes, userEmail, text, creationDate, movieId)"
            + " VALUES(?,?,?,?,?,?,?)"
            + "ON DUPLICATE KEY UPDATE id = id";
    }

    protected String getInsertStatementCommentVote() {
        return "INSERT INTO CommentVote(userEmail, vote, commentId)"
                + " VALUES(?,?,?)"
                + "ON DUPLICATE KEY UPDATE vote=VALUES(vote)";
    }

    protected String getInsertStatementRate() {
        return "INSERT INTO Rate(userEmail, score, movieId)"
                + " VALUES(?,?,?)"
                + "ON DUPLICATE KEY UPDATE score=VALUES(score)";
    }


    protected String getInsertStatementUser() {
        return "INSERT INTO User(id, email, pass, nickname, name, birthDate)"
            + " VALUES(?,?,?,?,?,?)"
            + "ON DUPLICATE KEY UPDATE id = id";
    }

    protected void fillInsertValuesMovie(PreparedStatement ps, Movie data) throws SQLException {
        ps.setString(1, data.Id.toString());
        ps.setString(2, data.Name);
        ps.setString(3, data.AgeLimit.toString());
        ps.setString(4, data.Duration.toString());
        ps.setString(5, Float.toString(data.IMDBRate));
        ps.setString(6, data.Summary);
        ps.setString(7, data.Director);
        ps.setString(8, data.ReleaseDate);
        ps.setString(9, Float.toString(data.Score));
        ps.setString(10, data.CoverImage);
        ps.setString(11, data.Image);
        String writers = String.join(", ", data.Writers);
        ps.setString(12, writers);
    }

    protected void fillInsertValuesActed_in(PreparedStatement ps, int actorId, int movieId) throws SQLException {
        ps.setString(1, String.valueOf(actorId));
        ps.setString(2, String.valueOf(movieId));
    }

    protected void fillInsertValuesMovie_Genre(PreparedStatement ps, int movieId, String genre) throws SQLException {
        ps.setString(1, String.valueOf(movieId));
        ps.setString(2, genre);
    }

    protected void fillInsertValuesWatchlist(PreparedStatement ps, String userId, String movieId) throws SQLException {
        ps.setString(1, userId);
        ps.setString(2, movieId);
    }

    protected void fillInsertValuesActor(PreparedStatement ps, Actor data) throws SQLException {
        ps.setString(1, data.Id.toString());
        ps.setString(2, data.Name);
        ps.setString(3, data.BirthDate);
        ps.setString(4, data.Nationality);
        ps.setString(5, data.Image);
    }

    protected void fillInsertValuesComment(PreparedStatement ps, Comment data) throws SQLException {
        ps.setString(1, data.Id.toString());
        ps.setString(2, data.Likes.toString());
        ps.setString(3, data.Dislikes.toString());
        ps.setString(4, data.UserEmail);
        ps.setString(5, data.Text);
        ps.setString(6, data.CreationDate);
        ps.setString(7, data.MovieId.toString());
    }

    protected void fillInsertValuesRate(PreparedStatement ps, Rate data) throws SQLException {
        ps.setString(1, data.UserEmail);
        ps.setString(2, Float.toString(data.Score));
        ps.setString(3, data.MovieId.toString());
    }

    protected void fillInsertValuesCommentVote(PreparedStatement ps, CommentVote data) throws SQLException {
        ps.setString(1, data.UserEmail);
        ps.setString(2, data.Vote.toString());
        ps.setString(3, data.CommentId.toString());
    }

    protected void fillInsertValuesUser(PreparedStatement ps, User data) throws SQLException {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String pw = encoder.encode(data.Password);

        ps.setString(1, String.valueOf(data.Id));
        ps.setString(2, data.Email);
        ps.setString(3, pw);
        ps.setString(4, data.Nickname);
        ps.setString(5, data.Name);
        ps.setString(6, data.BirthDate.toString());
    }

    protected String getFindAllStatement(String TableName) {
        return String.format("SELECT * FROM %s;", TableName);
    }

    protected String sortStatementBy(String sortType) {
        return "SELECT * FROM Movie ORDER BY "+sortType+" desc;";
    }

    protected String getFindAllStatementSearchByName(String name) {
        return "SELECT * FROM Movie m WHERE m.name LIKE '%" + name + "%' ;";
    }

    protected String getFindAllStatementSearchByGenre(String genreName) {
        return "SELECT m.* FROM Movie m, movie_genre mg WHERE m.id = mg.movieId AND mg.genre LIKE '%" + genreName + "%' ;";
    }

    protected String getFindAllStatementSearchByDate(String startYear, String endYear) {
        return "SELECT * FROM Movie m WHERE extract(year from m.releaseDate) > "+ startYear+ " and extract(year from m.releaseDate) < "+ endYear +";";
    }

    protected String getFindAllStatementMoviesActedIn(String actorId) {
        return "SELECT m.* FROM Movie m, actor_movie am WHERE m.id = am.movieId AND am.actorId = " + actorId + ";";
    }

    protected String getFindAllStatementMovieActors(String movieId) {
        return "SELECT a.* FROM Actor a, actor_movie am WHERE a.id = am.actorId AND am.movieId = " + movieId + ";";
    }

    protected String getFindAllStatementMovieComments(String movieId) {
        return "SELECT * FROM Comment c WHERE c.movieId = " + movieId + ";";
    }

    protected String getCommentLikesOrDislikesStatement(String commentId, String type){
        return "SELECT COUNT(*) FROM commentvote WHERE vote = " + type +" AND commentId = "+commentId+";";
    }

    protected String getMovieRatesStatement(String movieId){
        return "SELECT score FROM Rate WHERE movieId = " + movieId +";";
    }

    protected String getMovieGenresStatement(String movieId){
        return "SELECT genre FROM movie_genre WHERE movieId = " + movieId +";";
    }

    protected String getFindWatchlist(int userId) {
        return String.format("SELECT m.* FROM Watchlist w, Movie m WHERE w.userId = %s AND w.movieId = m.id;", userId);
    }

    protected String getRemoveFromWatchlist(String userId, String movieId){
        return String.format("DELETE FROM Watchlist w WHERE w.userId = %s AND w.movieId = %s;", userId, movieId);
    }


    protected Actor convertResultSetToDomainModelActor(ResultSet rs) throws Exception {
        return new Actor (
            Integer.parseInt(rs.getString(1)),
            rs.getString(2),
            rs.getString(3),
            rs.getString(4),
            rs.getString(5)
        );
    }

    protected Comment convertResultSetToDomainModelComment(ResultSet rs) throws Exception {
        Comment newComment = new Comment (
                Integer.parseInt(rs.getString(1)),
                rs.getString(4),
                Integer.parseInt(rs.getString(7)),
                rs.getString(5)
        );

        newComment.Likes = Integer.parseInt(rs.getString(2));
        newComment.Dislikes = Integer.parseInt(rs.getString(3));
        newComment.CreationDate = rs.getString(6);

        return newComment;
    }

    protected User convertResultSetToDomainModelUser(ResultSet rs) throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String birthDate = rs.getString(6);
        LocalDate localDate = LocalDate.parse(birthDate, formatter);

        return new User (
                rs.getInt(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                localDate
        );
    }
    protected ArrayList<Movie> convertResultSetToDomainModelList(ResultSet rs) throws Exception {
        ArrayList<Movie> movies = new ArrayList<>();
        while (rs.next()) {
            movies.add(this.convertResultSetToDomainModelMovie(rs));
        }
        return movies;
    }

    protected ArrayList<Float> convertResultSetToIntegerList(ResultSet rs) throws Exception {
        ArrayList<Float> rateScores = new ArrayList<>();
        while (rs.next()) {
            rateScores.add(Float.parseFloat(rs.getString(1)));
        }
        return rateScores;
    }

    protected ArrayList<String> convertResultSetToStringList(ResultSet rs) throws Exception {
        ArrayList<String> genres = new ArrayList<>();
        while (rs.next()) {
            genres.add(rs.getString(1));
        }
        return genres;
    }
    protected ArrayList<Actor> convertResultSetToDomainModelListActor(ResultSet rs) throws Exception {
        ArrayList<Actor> actors = new ArrayList<>();
        while (rs.next()) {
            actors.add(this.convertResultSetToDomainModelActor(rs));
        }
        return actors;
    }

    protected ArrayList<Comment> convertResultSetToDomainModelListComment(ResultSet rs) throws Exception {
        ArrayList<Comment> comments = new ArrayList<>();
        while (rs.next()) {
            comments.add(this.convertResultSetToDomainModelComment(rs));
        }
        return comments;
    }

    public ArrayList<Movie> getAllMovies() throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement(getFindAllStatement("Movie"));
        try {
            ResultSet resultSet = mv.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            return convertResultSetToDomainModelList(resultSet);
        } catch (Exception e) {
            System.out.println("error in Movies.findAll query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }

    public ArrayList<Movie> getAllMovies_SortedBy(String sortType) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement(sortStatementBy(sortType));
        try {
            ResultSet resultSet = mv.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            return convertResultSetToDomainModelList(resultSet);
        } catch (Exception e) {
            System.out.println("error in Movies.findAll query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }

    public int getCommentLikeOrDislikes(String CommentId, int type) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement(getCommentLikesOrDislikesStatement(CommentId, String.valueOf(type)));
        try {
            ResultSet resultSet = mv.executeQuery();
            if (!resultSet.next()) {
                return 0;
            }
            return resultSet.getInt(1);
        } catch (Exception e) {
            System.out.println("error in getCommentLikeOrDislikes query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }

    public List<Float> getMovieRates(String movieId) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement(getMovieRatesStatement(movieId));
        try {
            ResultSet resultSet = mv.executeQuery();
            return convertResultSetToIntegerList(resultSet);
        } catch (Exception e) {
            System.out.println("error in getMovieRates query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }

    public List<String> getMovieGenres(String movieId) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement(getMovieGenresStatement(movieId));
        try {
            ResultSet resultSet = mv.executeQuery();
            return convertResultSetToStringList(resultSet);
        } catch (Exception e) {
            System.out.println("error in getMovieRates query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }
    public ArrayList<Movie> getActorMovies(int actorId) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement(getFindAllStatementMoviesActedIn(String.valueOf(actorId)));
        try {
            ResultSet resultSet = mv.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            return convertResultSetToDomainModelList(resultSet);
        } catch (Exception e) {
            System.out.println("error in get actor movies query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }

    public ArrayList<Actor> getMovieActors(String movieId) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement(getFindAllStatementMovieActors(movieId));
        try {
            ResultSet resultSet = mv.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            return convertResultSetToDomainModelListActor(resultSet);
        } catch (Exception e) {
            System.out.println("error in get actor movies query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }

    public ArrayList<Comment> getMovieComments(String movieId) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement(getFindAllStatementMovieComments(movieId));
        try {
            ResultSet resultSet = mv.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            return convertResultSetToDomainModelListComment(resultSet);
        } catch (Exception e) {
            System.out.println("error in get actor movies query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }

    public ArrayList<Movie> searchMovieByName(String name) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement(getFindAllStatementSearchByName(name));
        try {
            ResultSet resultSet = mv.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            return convertResultSetToDomainModelList(resultSet);
        } catch (Exception e) {
            System.out.println("error in search by name query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }


    public ArrayList<Movie> searchMovieByGenre(String genreName) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement(getFindAllStatementSearchByGenre(genreName));
        try {
            ResultSet resultSet = mv.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            return convertResultSetToDomainModelList(resultSet);
        } catch (Exception e) {
            System.out.println("error in search by genre query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }

    public ArrayList<Movie> searchMovieByDate(String startYear, String endYear) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement(getFindAllStatementSearchByDate(startYear, endYear));
        try {
            ResultSet resultSet = mv.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            return convertResultSetToDomainModelList(resultSet);
        } catch (Exception e) {
            System.out.println("error in search by name query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }

    public ArrayList<Movie> getWatchlist(int userId) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement(getFindWatchlist(userId));
        try {
            ResultSet resultSet = mv.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            return convertResultSetToDomainModelList(resultSet);
        } catch (Exception e) {
            System.out.println("error in Watchlist.findAll query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }

    public void removeFromWatchlist(String userId, String movieId) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement ps = con.prepareStatement(getRemoveFromWatchlist(userId, movieId));
        try {
            ps.execute();
            ps.close();
            con.close();
        } catch (Exception e) {
            ps.close();
            con.close();
            System.out.println("removeFromWatchlist: error in Repository. query.");
            e.printStackTrace();
            throw e;
        }
    }


    protected Movie convertResultSetToDomainModelMovie(ResultSet rs) throws Exception {
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString(8));
        String string_writers = rs.getString(12);
        List<String> writers = new ArrayList<String>(Arrays.asList(string_writers.split(", ")));

        return new Movie(
            Integer.parseInt(rs.getString(1)),
            rs.getString(2),
            Integer.parseInt(rs.getString(3)),
            Float.parseFloat(rs.getString(5)),
            rs.getString(6),
            rs.getString(7),
            Integer.parseInt(rs.getString(4)),
            date,
            writers, //writers
            new ArrayList<String>(),
            new ArrayList<Integer>(),
            rs.getString(11),
            rs.getString(10)
        );
    }

    public Actor findActor(String id) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement actr = con.prepareStatement("SELECT * FROM Actor WHERE id = ?");
        actr.setString(1, id);
        try {
            ResultSet resultSet = actr.executeQuery();
            if (!resultSet.next()) {
                return null;
            }
            return convertResultSetToDomainModelActor(resultSet);
        } catch (Exception e) {
            System.out.println("error in Actor.find query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(actr);
            DbUtils.close(con);
        }
    }


    public Movie findMovie(String id) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement mv = con.prepareStatement("SELECT * FROM Movie WHERE id = ?");
        mv.setString(1, id);
        try {
            ResultSet resultSet = mv.executeQuery();
            if (!resultSet.next()) {
                return null;
            }
            return convertResultSetToDomainModelMovie(resultSet);
        } catch (Exception e) {
            System.out.println("error in CourseRepository.find query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(mv);
            DbUtils.close(con);
        }
    }

    public User findUser(String id) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM User WHERE id = ?");
        ps.setString(1, id);
        try {
            ResultSet resultSet = ps.executeQuery();
            if (!resultSet.next()) {
                return null;
            }
            return convertResultSetToDomainModelUser(resultSet);
        } catch (Exception e) {
            System.out.println("error in User.find query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(ps);
            DbUtils.close(con);
        }
    }

    public User findUserByEmail(String userEmail) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM User WHERE email = ?");
        ps.setString(1, userEmail);
        try {
            ResultSet resultSet = ps.executeQuery();
            if (!resultSet.next()) {
                return null;
            }
            return convertResultSetToDomainModelUser(resultSet);
        } catch (Exception e) {
            System.out.println("error in User.find query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(ps);
            DbUtils.close(con);
        }
    }

    public void insertMovie(Movie movie) throws SQLException {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement ps = con.prepareStatement(getInsertStatementMovie());
        fillInsertValuesMovie(ps, movie);
        try {
            ps.execute();
            ps.close();
            con.close();
        } catch (Exception e) {
            ps.close();
            con.close();
            System.out.println("Movie: error in Repository.insert query.");
            e.printStackTrace();
        }
    }

    public void insertMovieActors(Movie movie) throws SQLException {
        Connection con = ConnectionPool.getConnection();
        for (int actorId : movie.Cast) {
            PreparedStatement ps = con.prepareStatement(getInsertStatementActor_Movie());
            fillInsertValuesActed_in(ps, actorId, movie.Id);
            try {
                ps.execute();
                ps.close();
            } catch (Exception e) {
                ps.close();
                con.close();
                System.out.println("MovieActors: error in Repository.insert query.");
                e.printStackTrace();
            }
        }
        con.close();
    }

    public void insertMovieGenres(Movie movie) throws SQLException {
        Connection con = ConnectionPool.getConnection();
        for (String genre : movie.Genres) {
            PreparedStatement ps = con.prepareStatement(getInsertStatementMovie_Genre());
            fillInsertValuesMovie_Genre(ps, movie.Id, genre);
            try {
                ps.execute();
                ps.close();
            } catch (Exception e) {
                ps.close();
                con.close();
                System.out.println("Movie_Genre: error in Repository.insert query.");
                e.printStackTrace();
            }
        }
        con.close();
    }

    public void insertToWatchlist(String userId, String movieId) throws SQLException {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement ps = con.prepareStatement(getInsertStatementWatchlist());
        fillInsertValuesWatchlist(ps, userId, movieId);
        try {
            ps.execute();
            ps.close();
            con.close();
        } catch (Exception e) {
            ps.close();
            con.close();
            System.out.println("Movie_Genre: error in Repository.insert query.");
            e.printStackTrace();
        }
    }

    public void insertActor(Actor actor) throws SQLException {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement ps = con.prepareStatement(getInsertStatementActor());
        fillInsertValuesActor(ps, actor);
        try {
            ps.execute();
            ps.close();
            con.close();
        } catch (Exception e) {
            ps.close();
            con.close();
            System.out.println("Actor: error in Repository.insert query.");
            e.printStackTrace();
        }
    }

    public void insertComment(Comment comment) throws SQLException {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement ps = con.prepareStatement(getInsertStatementComment());
        fillInsertValuesComment(ps, comment);
        try {
            ps.execute();
            ps.close();
            con.close();
        } catch (Exception e) {
            ps.close();
            con.close();
            System.out.println("Comment: error in Repository.insert query.");
            throw e;
        }
    }

    public void insertCommentVote(CommentVote commentVote) throws SQLException {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement ps = con.prepareStatement(getInsertStatementCommentVote());
        fillInsertValuesCommentVote(ps, commentVote);
        try {
            ps.execute();
            ps.close();
            con.close();
        } catch (Exception e) {
            ps.close();
            con.close();
            System.out.println("CommentVote: error in Repository.insert query.");
            e.printStackTrace();
            throw e;
        }
    }

    public void insertRate(Rate rate) throws SQLException {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement ps = con.prepareStatement(getInsertStatementRate());
        fillInsertValuesRate(ps, rate);
        try {
            ps.execute();
            ps.close();
            con.close();
        } catch (Exception e) {
            ps.close();
            con.close();
            System.out.println("Rate: error in Repository.insert query.");
            e.printStackTrace();
            throw e;
        }
    }

    public void insertUser(User user) throws SQLException {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement ps = con.prepareStatement(getInsertStatementUser());
        fillInsertValuesUser(ps, user);
        try {
            ps.execute();
            ps.close();
            con.close();
        } catch (Exception e) {
            ps.close();
            con.close();
            System.out.println("User: error in Repository.insert query.");
            e.printStackTrace();
        }
    }


    public int getDataSize(String table) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM " + table);
        try {
            ResultSet resultSet = ps.executeQuery();
            if (!resultSet.next()) {
                return 0;
            }
            return resultSet.getInt(1);
        } catch (Exception e) {
            System.out.println("error in select count(*) query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(ps);
            DbUtils.close(con);
        }
    }

}