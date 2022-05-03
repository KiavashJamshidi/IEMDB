package com.example.Repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.Exceptions.MovieNotFound;
import com.example.Exceptions.ActorNotFound;
import com.example.Model.*;
import org.apache.commons.dbutils.DbUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.*;

public class IemdbRepository {
    private static final String TABLE_NAME = "Movie";
    private static IemdbRepository instance;

    private static final String API_MOVIES = "http://138.197.181.131:5200/api/students";
    private static final String API_ACTORS = "http://138.197.181.131:5200/api/grades/";

    private static final String IN_PROG_ST = "INSERT INTO InProgressCourses(sid, code, classCode) "
            + " VALUES(?,?,?)"
            + " ON DUPLICATE KEY UPDATE sid = sid";
    private static final String SUBMIT_ST = "INSERT INTO SubmittedCourses(sid, code, classCode) "
            + " VALUES(?,?,?)"
            + " ON DUPLICATE KEY UPDATE sid = sid";
    private static final String IN_PROG_QUEUE_ST = "INSERT INTO InProgressQueue(sid, code, classCode) "
            + " VALUES(?,?,?)"
            + " ON DUPLICATE KEY UPDATE sid = sid";
    private static final String WAIT_QUEUE_ST = "INSERT INTO WaitQueue(sid, code, classCode) "
            + " VALUES(?,?,?)"
            + " ON DUPLICATE KEY UPDATE sid = sid";
    public static final String DEL_IN_PROG = "DELETE FROM InProgressCourses WHERE sid = ? AND code = ? AND classCode = ?";
    public static final String DEL_SUBMIT = "DELETE FROM SubmittedCourses WHERE sid = ? AND code = ? AND classCode = ?";
    public static final String DEL_IN_PROG_QUEUE = "DELETE FROM InProgressQueue WHERE sid = ? AND code = ? AND classCode = ?";
    public static final String DEL_QUEUE = "DELETE FROM WaitQueue WHERE sid = ? AND code = ? AND classCode = ?";
    public static final String GET_IN_PROG = "SELECT C.code, C.classCode FROM InProgressCourses C WHERE C.sid = ?;";
    public static final String GET_SUBMIT = "SELECT C.code, C.classCode FROM SubmittedCourses C WHERE C.sid = ?;";
    public static final String GET_IN_PROG_QUEUE = "SELECT C.code, C.classCode FROM InProgressQueue C WHERE C.sid = ?;";
    public static final String GET_QUEUE  = "SELECT C.code, C.classCode FROM WaitQueue C WHERE C.sid = ?;";
    public static final String CLR_IN_PROG = "DELETE FROM InProgressCourses WHERE sid = ?;";
    public static final String CLR_SUBMIT = "DELETE FROM SubmittedCourses WHERE sid = ?;";
    public static final String CLR_IN_PROG_QUEUE = "DELETE FROM InProgressQueue WHERE sid = ?;";
    public static final String CLR_QUEUE = "DELETE FROM WaitQueue WHERE sid = ?;";

    public static IemdbRepository getInstance() throws Exception {
        if (instance == null) {
            instance = new IemdbRepository();
        }
        return instance;
    }

    private IemdbRepository() throws Exception {
//        OfferingRepository.getInstance(); // to make sure course tables are created
        Connection con = ConnectionPool.getConnection();
        Statement createTableStatement = con.createStatement();

        createTableStatement.addBatch(
            "CREATE TABLE IF NOT EXISTS Actor(id INT, name CHAR(225), " +
                    "birthDate CHAR(15), nationality CHAR(225), " +
                    "image CHAR(225), PRIMARY KEY(id));"
        );

        createTableStatement.addBatch(
            "CREATE TABLE IF NOT EXISTS Movie(id INT, name CHAR(225), ageLimit INT, " +
                    "duration INT, imdbRate FLOAT, summary CHAR(255), director CHAR(225), " +
                    "releaseDate CHAR(225), score INT, coverImage CHAR(225), image CHAR(225), PRIMARY KEY(id));"
        );

        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS Comment(id INT, likes INT, dislikes INT, userEmail CHAR(22), " +
                        "text char(255), creationDate char(255), movieId INT, " +
                        "PRIMARY KEY(id)," +
                        "FOREIGN KEY (movieId) REFERENCES Movie(id));"
        );
        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS CommentVote(id INT, userEmail CHAR(22), " +
                        "vote INT, commentId INT, " +
                        "PRIMARY KEY(id), " +
                        "FOREIGN KEY (commentId) REFERENCES Comment(id));"
        );
        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS Rate(id INT, userEmail CHAR(22), " +
                        "score float, movieId INT, " +
                        "PRIMARY KEY(id), " +
                        "FOREIGN KEY (movieId) REFERENCES Movie(id));"
        );
        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS User(id INT, email CHAR(22), " +
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

        for (Movie movie : movies) { //check ali for insert grade
            insertMovie(movie);
        }

        for (Actor actor : actors)
            insertActor(actor);

        for (Comment comment : comments)
            insertComment(comment);

        for (User user : users)
            insertUser(user);
    }


//    private void setGrades(Student student) throws IOException, InterruptedException {
//        var client = HttpClient.newHttpClient();
//        ObjectMapper objectMapper = new ObjectMapper();
//        var gradesReq = HttpRequest.newBuilder(
//                URI.create(API_GRADE + student.getStudentId())
//        ).build();
//        HttpResponse<String> gradesRes = client.send(gradesReq, HttpResponse.BodyHandlers.ofString());
//        JsonNode gradesArr = objectMapper.readTree(gradesRes.body());
//
//        ArrayList<Grade> grades = new ArrayList<>();
//        gradesArr.forEach(grade -> {
//            Grade newGrade = new Grade(grade.get("code").asText(), "", grade.get("grade").asInt(), grade.get("term").asInt(), 0);
//            grades.add(newGrade);
//        });
//        student.setGrades(grades);
//    }
//
//    protected String getFindByIdStatement() {
//        return String.format("SELECT* FROM %s S WHERE S.id = ?;", TABLE_NAME);
//    }
//
//    protected void fillFindByIdValues(PreparedStatement st, String id) throws SQLException {
//        st.setString(1, id);
//    }

    protected String getInsertStatementMovie() {
        return "INSERT INTO Movie(id, name, ageLimit, duration, imdbRate, summary, director, releaseDate, score, coverImage, image)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?)"
            + "ON DUPLICATE KEY UPDATE id = id";
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
        ps.setString(9, data.Score.toString());
        ps.setString(10, data.CoverImage);
        ps.setString(11, data.Image);
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

    protected void fillInsertValuesUser(PreparedStatement ps, User data) throws SQLException {
        ps.setString(1, String.valueOf(data.Id));
        ps.setString(2, data.Email);
        ps.setString(3, data.Password);
        ps.setString(4, data.Nickname);
        ps.setString(5, data.Name);
        ps.setString(6, data.BirthDate.toString());
    }

    protected String getFindAllStatement(String TableName) {
        return String.format("SELECT * FROM %s;", TableName);
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

    protected ArrayList<Movie> convertResultSetToDomainModelList(ResultSet rs) throws Exception {
        ArrayList<Movie> movies = new ArrayList<>();
        while (rs.next()) {
            movies.add(this.convertResultSetToDomainModelMovie(rs));
        }
        return movies;
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

    protected Movie convertResultSetToDomainModelMovie(ResultSet rs) throws Exception {
//        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString(8));
//        return new Movie(
//            Integer.parseInt(rs.getString(1)),
//            rs.getString(2),
//            Integer.parseInt(rs.getString(3)),
//            Float.parseFloat(rs.getString(4)),
//            rs.getString(5),
//            rs.getString(6),
//            Integer.parseInt(rs.getString(7)),
//            date,
//            rs.getString(9),
//            rs.getString(10),
//            rs.getString(11)
//        );
    }

//    protected ArrayList<Student> convertResultSetToDomainModelList(ResultSet rs) throws Exception {
//        ArrayList<Student> students = new ArrayList<>();
//        while (rs.next()) {
//            students.add(this.convertResultSetToDomainModel(rs));
//        }
//        return students;
//    }
//
//    public void insertGrades(Student student) throws Exception {
//        Connection con = ConnectionPool.getConnection();
//        con.setAutoCommit(false);
//        PreparedStatement st = con.prepareStatement(
//                "INSERT INTO Grade(sid, code, term, grade)"
//                        + " VALUES(?,?,?,?)"
//                        + " ON DUPLICATE KEY UPDATE sid = sid"
//        );
//        for (Grade grade : student.getGrades()) {
//            st.setString(1, student.getStudentId());
//            st.setString(2, grade.code);
//            st.setInt(3, grade.term);
//            st.setInt(4, grade.grade);
//            st.addBatch();
//        }
//        try {
//            if (student.getGrades().size() > 0) {
//                st.executeBatch();
//                con.commit();
//            }
//        } catch (Exception e) {
//            con.rollback();
//            System.out.println("error in Repository.insert query.");
//            e.printStackTrace();
//        } finally {
//            DbUtils.close(st);
//            DbUtils.close(con);
//        }
//    }
//
//    public ArrayList<Grade> getGrades(String sid) throws Exception {
//        Connection con = ConnectionPool.getConnection();
//        PreparedStatement st = con.prepareStatement("select G.code, G.term, G.grade FROM Grade G WHERE G.sid = ?");
//        PreparedStatement st2 = con.prepareStatement("select C.name, C.units FROM Course C WHERE C.code = ?");
//        st.setString(1, sid);
//        try {
//            ResultSet resultSet = st.executeQuery();
//            if (resultSet == null) {
//                return new ArrayList<>();
//            }
//            ArrayList<Grade> result = new ArrayList<>();
//            while (resultSet.next()) {
//                st2.setString(1, resultSet.getString(1));
//                ResultSet c = st2.executeQuery();
//                if (c.next()) {
//                    result.add(new Grade(
//                            resultSet.getString(1),
//                            c.getString(1),
//                            resultSet.getInt(3),
//                            resultSet.getInt(2),
//                            c.getInt(2)
//                    ));
//                }
//            }
//            return result;
//        } catch (Exception e) {
//            System.out.println("Exception in get getPrerequisites");
//            throw e;
//        } finally {
//            DbUtils.close(st);
//            DbUtils.close(st2);
//            DbUtils.close(con);
//        }
//    }
//
//    private void runCourseQuery(String statement, String sid, String code, String classCode) throws Exception {
//        Connection con = ConnectionPool.getConnection();
//        PreparedStatement st = con.prepareStatement(statement);
//        st.setString(1, sid);
//        st.setString(2, code);
//        st.setString(3, classCode);
//
//        try {
//            st.execute();
//        } catch (Exception e) {
//            System.out.println("error in addCourse.insert query.");
//            e.printStackTrace();
//        } finally {
//            DbUtils.close(st);
//            DbUtils.close(con);
//        }
//    }
//
//    private ArrayList<String[]> getCourseListQuery(String statement, String sid) throws Exception {
//        ArrayList<String[]> courses = new ArrayList<>();
//        Connection con = ConnectionPool.getConnection();
//        PreparedStatement st = con.prepareStatement(statement);
//        st.setString(1, sid);
//        try {
//            ResultSet rs = st.executeQuery();
//            while (rs.next())
//                courses.add(new String[] {rs.getString(1), rs.getString(2)});
//            return courses;
//        } catch (Exception e) {
//            System.out.println("error in addCourse.insert query.");
//            e.printStackTrace();
//            throw e;
//        } finally {
//            DbUtils.close(st);
//            DbUtils.close(con);
//        }
//    }
//
//    private void clearList(String statement, String sid) throws Exception {
//        Connection con = ConnectionPool.getConnection();
//        PreparedStatement st = con.prepareStatement(statement);
//        st.setString(1, sid);
//
//        try {
//            st.execute();
//        } catch (Exception e) {
//            System.out.println("error in clear list.insert query.");
//            e.printStackTrace();
//        } finally {
//            DbUtils.close(st);
//            DbUtils.close(con);
//        }
//    }
//
//    public void addCourseToInProgCourses(String sid, String code, String classCode) throws Exception {
//        runCourseQuery(IN_PROG_ST, sid, code, classCode);
//    }
//
//    public void addCourseToInProgQueue(String sid, String code, String classCode) throws Exception {
//        runCourseQuery(IN_PROG_QUEUE_ST, sid, code, classCode);
//    }
//
//    public void addCourseToSubmittedCourses(String sid, String code, String classCode) throws Exception {
//        runCourseQuery(SUBMIT_ST, sid, code, classCode);
//    }
//
//    public void addCourseToQueue(String sid, String code, String classCode) throws Exception {
//        runCourseQuery(WAIT_QUEUE_ST, sid, code, classCode);
//    }
//
//    public void removeCourseFromInProg(String sid, String code, String classCode) throws Exception {
//        runCourseQuery(DEL_IN_PROG, sid, code, classCode);
//    }
//
//    public void removeCourseFromSubmit(String sid, String code, String classCode) throws Exception {
//        runCourseQuery(DEL_SUBMIT, sid, code, classCode);
//    }
//
//    public void removeCourseFromInProgQueue(String sid, String code, String classCode) throws Exception {
//        runCourseQuery(DEL_IN_PROG_QUEUE, sid, code, classCode);
//    }
//
//    public void removeCourseFromQueue(String sid, String code, String classCode) throws Exception {
//        runCourseQuery(DEL_QUEUE, sid, code, classCode);
//    }
//
//    public ArrayList<String[]> getInProgCourses(String sid) throws Exception {
//        return getCourseListQuery(GET_IN_PROG, sid);
//    }
//
//    public ArrayList<String[]> getSubmittedCourses(String sid) throws Exception {
//        return getCourseListQuery(GET_SUBMIT, sid);
//    }
//
//    public ArrayList<String[]> getInProgQueueCourses(String sid) throws Exception {
//        return getCourseListQuery(GET_IN_PROG_QUEUE, sid);
//    }
//
//    public ArrayList<String[]> getQueueCourses(String sid) throws Exception {
//        return getCourseListQuery(GET_QUEUE, sid);
//    }
//
//    public void clearInProgCourses(String sid) throws Exception {
//        clearList(CLR_IN_PROG, sid);
//    }
//
//    public void clearSubmittedCourses(String sid) throws Exception {
//        clearList(CLR_SUBMIT, sid);
//    }
//
//    public void clearInProgQueue(String sid) throws Exception {
//        clearList(CLR_IN_PROG_QUEUE, sid);
//    }
//
//    public void clearQueue(String sid) throws Exception {
//        clearList(CLR_QUEUE, sid);
//    }

//    public ArrayList<String[]> getNewlyAddedCourses(String sid) throws Exception {
//        return getCourseListQuery("SELECT I.code, I.classCode FROM InProgressCourses I WHERE I.sid = ? " +
//                "AND NOT EXISTS (SELECT S.code, S.classCode " +
//                "FROM SubmittedCourses S WHERE S.code = I.code AND S.classCode = I.classCode);", sid);
//    }
//
//    public ArrayList<String[]> getRemovedCourses(String sid) throws Exception {
//        return getCourseListQuery("SELECT I.code, I.classCode FROM SubmittedCourses I WHERE I.sid = ? " +
//                "AND NOT EXISTS (SELECT S.code, S.classCode " +
//                "FROM InProgressCourses S WHERE S.code = I.code AND S.classCode = I.classCode);", sid);
//    }
//
//    public int getInProgUnitCount(String sid) throws Exception {
//        Connection con = ConnectionPool.getConnection();
//        PreparedStatement st = con.prepareStatement(
//                "SELECT SUM(C.units) FROM InProgressCourses P, Course C " +
//                        "WHERE P.sid = ? AND C.code = P.code AND C.classCode = P.classCode");
//        st.setString(1, sid);
//        try {
//            ResultSet resultSet = st.executeQuery();
//            if (resultSet.next()) {
//                return resultSet.getInt(1);
//            }
//            return 0;
//        } catch (Exception e) {
//            System.out.println("error in unit count.find query.");
//            e.printStackTrace();
//            throw e;
//        } finally {
//            DbUtils.close(st);
//            DbUtils.close(con);
//        }
//    }
//
//    public int getInProgWaitUnitCount(String sid) throws Exception {
//        Connection con = ConnectionPool.getConnection();
//        PreparedStatement st = con.prepareStatement(
//                "SELECT SUM(C.units) FROM InProgressQueue P, Course C " +
//                        "WHERE P.sid = ? AND C.code = P.code AND C.classCode = P.classCode");
//        st.setString(1, sid);
//        try {
//            ResultSet resultSet = st.executeQuery();
//            if (resultSet.next()) {
//                return resultSet.getInt(1);
//            }
//            return 0;
//        } catch (Exception e) {
//            System.out.println("error in unit count.find query.");
//            e.printStackTrace();
//            throw e;
//        } finally {
//            DbUtils.close(st);
//            DbUtils.close(con);
//        }
//    }
//
//    public ArrayList<String> getPassedCourses(String sid) throws Exception {
//        Connection con = ConnectionPool.getConnection();
//        PreparedStatement st = con.prepareStatement("SELECT G.code FROM Grade G WHERE G.sid = ? AND G.grade >= 10");
//        st.setString(1, sid);
//        try {
//            ArrayList<String> codes = new ArrayList<>();
//            ResultSet resultSet = st.executeQuery();
//            if (resultSet == null) {
//                return new ArrayList<>();
//            }
//            while (resultSet.next()) {
//                codes.add(resultSet.getString(1));
//            }
//            return codes;
//        } catch (Exception e) {
//            System.out.println("error in unit count.find query.");
//            e.printStackTrace();
//            throw e;
//        } finally {
//            DbUtils.close(st);
//            DbUtils.close(con);
//        }
//    }
//
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


    public Movie findById(String id) throws Exception {
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
            System.out.println("error in Repository.insert query.");
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
            System.out.println("error in Repository.insert query.");
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
            System.out.println("error in Repository.insert query.");
            e.printStackTrace();
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
            System.out.println("error in Repository.insert query.");
            e.printStackTrace();
        }
    }
}