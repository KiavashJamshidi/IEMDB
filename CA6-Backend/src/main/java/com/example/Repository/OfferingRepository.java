package com.marshmellow.bolbolestan.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marshmellow.bolbolestan.model.Offering;
import com.marshmellow.bolbolestan.model.Student;
import org.apache.commons.dbutils.DbUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class OfferingRepository {
    private static final String TABLE_NAME = "Course";
    private static OfferingRepository instance;

    private static final String API_COURSES = "http://138.197.181.131:5200/api/courses";

    public static OfferingRepository getInstance() throws Exception {
        if (instance == null) {
            try {
                instance = new OfferingRepository();
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("error in OfferingRepository.create query.");
            }
        }
        return instance;
    }

    private OfferingRepository() throws Exception {
        Connection con = ConnectionPool.getConnection();
        Statement createTableStatement = con.createStatement();
        con.setAutoCommit(false);
        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS Course(code CHAR(50), classCode CHAR(10), name CHAR(225), " +
                    "units INT, type CHAR(50), instructor CHAR(225), capacity INT, " +
                    "examStart CHAR(100), examEnd CHAR(100), time CHAR(100), PRIMARY KEY(code, classCode));"
        );
        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS ClassDays(code CHAR(50), classCode CHAR(10), day CHAR(50), " +
                        "PRIMARY KEY(code, classCode, day)," +
                        "FOREIGN KEY (code, classCode) REFERENCES Course(code, classCode));"
        );
        createTableStatement.addBatch(
                "CREATE TABLE IF NOT EXISTS Prerequisites(code CHAR(50), pCode CHAR(50), " +
                   "PRIMARY KEY(code, pcode)," +
                   "FOREIGN KEY (code) REFERENCES Course(code)," +
                   "FOREIGN KEY (pCode) REFERENCES Course(code));"
        );
        createTableStatement.executeBatch();
        con.commit();
        createTableStatement.close();
        con.close();

        Offering[] courses = getOfferingsFromAPI();
        for (Offering c : courses) {
            insert(c);
        }
        for (Offering c : courses) {
            insertPrerequisites(c);
        }
    }

    private Offering[] getOfferingsFromAPI() throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var coursesReq = HttpRequest.newBuilder(
                URI.create(API_COURSES)
        ).build();

        HttpResponse<String> coursesRes = client.send(coursesReq, HttpResponse.BodyHandlers.ofString());
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(coursesRes.body(), Offering[].class);
    }

    protected String getFindByIdStatement() {
        return "SELECT * FROM Course C WHERE C.code = ? AND C.ClassCode = ?;";
    }

    protected String getInsertStatement() {
        return "INSERT INTO Course(code, classCode, name, units, type, instructor, capacity, examStart, examEnd, time)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?)"
                + " ON DUPLICATE KEY UPDATE code = code;";
    }

    protected void fillInsertValues(PreparedStatement st, Offering data) throws SQLException {
        st.setString(1, data.getCode());
        st.setString(2, data.getClassCode());
        st.setString(3, data.getName());
        st.setInt(4, data.getUnits());
        st.setString(5, data.getType());
        st.setString(6, data.getInstructor());
        st.setInt(7, data.getCapacity());
        st.setTimestamp(8, new java.sql.Timestamp(data.getExamStart().getTime()));
        st.setTimestamp(9, new java.sql.Timestamp(data.getExamEnd().getTime()));
        st.setString(10, data.getClassHour());
    }

    protected String getFindAllStatement() {
        return String.format("SELECT * FROM %s;", TABLE_NAME);
    }

    protected Offering convertResultSetToDomainModel(ResultSet rs) throws Exception {
        Offering offering = new Offering();
        offering.setCode(rs.getString(1));
        offering.setClassCode(rs.getString(2));
        offering.setName(rs.getString(3));
        offering.setUnits(rs.getInt(4));
        offering.setType(rs.getString(5));
        offering.setInstructor(rs.getString(6));
        offering.setCapacity(rs.getInt(7));
        offering.setExamStart(rs.getTimestamp(8));
        offering.setExamEnd(rs.getTimestamp(9));
        offering.setClassHour(rs.getString(10));
        offering.setPrerequisites(getPrerequisites(offering.getCode()));
        offering.setClassDays(getClassDays(offering.getCode(), offering.getClassCode()));
        offering.setParticipantsCount(getParticipantsCount(offering.getCode(), offering.getClassCode()));
        return offering;
    }

    protected ArrayList<Offering> convertResultSetToDomainModelList(ResultSet rs) throws Exception {
        ArrayList<Offering> offerings = new ArrayList<>();
        while (rs.next()) {
            offerings.add(this.convertResultSetToDomainModel(rs));
        }
        return offerings;
    }

    public ArrayList<String> getClassDays(String code, String classCode) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement st = con.prepareStatement("SELECT P.day FROM ClassDays P WHERE P.code = ? AND P.classCode = ?");
        st.setString(1, code);
        st.setString(2, classCode);
        try {
            ResultSet resultSet = st.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            ArrayList<String> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(resultSet.getString(1));
            }
            return result;
        } catch (Exception e) {
            System.out.println("Exception in get getClassDays");
            throw e;
        } finally {
            DbUtils.close(st);
            DbUtils.close(con);
        }
    }

    public ArrayList<String> getPrerequisites(String code) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement st = con.prepareStatement("SELECT P.pCode FROM Prerequisites P WHERE P.code = ?");
        st.setString(1, code);
        try {
            ResultSet resultSet = st.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            ArrayList<String> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(resultSet.getString(1));
            }
            return result;
        } catch (Exception e) {
            System.out.println("Exception in get getPrerequisites");
            throw e;
        } finally {
            DbUtils.close(st);
            DbUtils.close(con);
        }
    }

    public ArrayList<String> getWaitingList(String code, String classCode) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement st = con.prepareStatement("SELECT Q.sid FROM WaitQueue Q WHERE Q.code = ? AND Q.classCode = ?");
        st.setString(1, code);
        st.setString(2, classCode);
        try {
            ResultSet resultSet = st.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            ArrayList<String> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(resultSet.getString(1));
            }
            return result;
        } catch (Exception e) {
            System.out.println("Exception in get getPrerequisites");
            throw e;
        } finally {
            DbUtils.close(st);
            DbUtils.close(con);
        }
    }

    public Offering findById(String code, String classCode) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement st = con.prepareStatement(getFindByIdStatement());
        st.setString(1, code);
        st.setString(2, classCode);
        try {
            ResultSet resultSet = st.executeQuery();
            if (!resultSet.next()) {
                return null;
            }
            return convertResultSetToDomainModel(resultSet);
        } catch (Exception e) {
            System.out.println("error in CourseRepository.find query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(st);
            DbUtils.close(con);
        }
    }

    public void insert(Offering offering) throws Exception {
        Connection con = ConnectionPool.getConnection();
        con.setAutoCommit(false);
        PreparedStatement st = con.prepareStatement(getInsertStatement());
        fillInsertValues(st, offering);
        PreparedStatement st3 = con.prepareStatement(
                "INSERT INTO ClassDays(code, classCode, day) "
                + " VALUES(?,?,?)"
                + "ON DUPLICATE KEY UPDATE code = code"
        );
        for (String classDay : offering.getClassDays()) {
            st3.setString(1, offering.getCode());
            st3.setString(2, offering.getClassCode());
            st3.setString(3, classDay);
            st3.addBatch();
        }
        try {
            st.execute();
            st3.executeBatch();
            con.commit();
        } catch (Exception e) {
            con.rollback();
            System.out.println("error in Repository.insert query.");
            e.printStackTrace();
        } finally {
            DbUtils.close(st);
            DbUtils.close(st3);
            DbUtils.close(con);
        }
    }

    public void insertPrerequisites(Offering offering) throws Exception {
        Connection con = ConnectionPool.getConnection();
        con.setAutoCommit(false);
        PreparedStatement st2 = con.prepareStatement(
                "INSERT INTO Prerequisites(code, pCode)"
                   + " VALUES(?,?)"
                   + " ON DUPLICATE KEY UPDATE code = code"
        );
        for (String prerequisite : offering.getPrerequisites()) {
            st2.setString(1, offering.getCode());
            st2.setString(2, prerequisite);
            st2.addBatch();
        }
        try {
            if (offering.getPrerequisites().size() > 0) {
                st2.executeBatch();
                con.commit();
            }
        } catch (Exception e) {
            con.rollback();
            System.out.println("error in Repository.insert query.");
            e.printStackTrace();
        } finally {
            DbUtils.close(st2);
            DbUtils.close(con);
        }
    }

    public ArrayList<Offering> getAllOfferings() throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement st = con.prepareStatement(getFindAllStatement());
        try {
            ResultSet resultSet = st.executeQuery();
            if (resultSet == null) {
                return new ArrayList<>();
            }
            return convertResultSetToDomainModelList(resultSet);
        } catch (Exception e) {
            System.out.println("error in OfferingRepository.findAll query.");
            e.printStackTrace();
            throw e;
        } finally {
            DbUtils.close(st);
            DbUtils.close(con);
        }
    }

    public int getParticipantsCount(String code, String classCode) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement st = con.prepareStatement("SELECT COUNT(*) FROM SubmittedCourses C WHERE C.code = ? AND C.classCode = ?;");
        st.setString(1, code);
        st.setString(2, classCode);
        try {
            ResultSet rs = st.executeQuery();
            if (rs.next())
                return rs.getInt(1);
            return -1;
        } catch (SQLSyntaxErrorException e) {
            return 0;
        } finally {
            DbUtils.close(st);
            DbUtils.close(con);
        }
    }

    public int getCapacity(String code, String classCode) throws Exception {
        Connection con = ConnectionPool.getConnection();
        PreparedStatement st = con.prepareStatement("SELECT C.capacity FROM Course C WHERE C.code = ? AND C.classCode = ?;");
        st.setString(1, code);
        st.setString(2, classCode);
        try {
            ResultSet rs = st.executeQuery();
            if (rs.next())
                return rs.getInt(1);
            return -1;
        } finally {
            DbUtils.close(st);
            DbUtils.close(con);
        }
    }

    public void testt() throws Exception {
        Connection con = ConnectionPool.getConnection();
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("SELECT C.name FROM Course C;");
        if (rs.next())
            System.out.println(rs.getString(1));
        else
            System.out.println("DOOOOL");
        DbUtils.close(rs);
        DbUtils.close(st);
        DbUtils.close(con);
    }
}

