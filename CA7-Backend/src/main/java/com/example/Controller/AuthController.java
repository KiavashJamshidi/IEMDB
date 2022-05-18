package com.example.Controller;

import com.example.Model.IEMDB;
import com.example.Model.User;
import com.example.Repository.IemdbRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import io.jsonwebtoken.Jwts;

@RestController
@RequestMapping("/api")
public class AuthController {
    public static final String KEY = "iemdb1401iemdb1401iemdb1401iemdb1401iemdb1401iemdb1401";

    @PostMapping("/oauth")
    public ObjectNode oauth(@RequestBody JsonNode body) throws Exception {
        System.out.println("Auth controller started: OAUTH");

        if (!body.has("code"))
            throw new Exception("Missing Parameter");

        String code = body.get("code").asText();
        String client_id = "4a4cc2f558b4c85a843b&scope";
        String client_secret = "26768f20fce54ee712182b5ca3adcd4cb201aa41";
        System.out.println(code);
        String accessTokenURL = String.format(
                "https://github.com/login/oauth/access_token?client_id=%s&client_secret=%s&code=%s",
                client_id, client_secret, code
        );

        HttpClient client = HttpClient.newHttpClient();
        URI accessTokenUri = URI.create(accessTokenURL);
        HttpRequest.Builder accessTokenBuilder = HttpRequest.newBuilder().uri(accessTokenUri);
        HttpRequest accessTokenRequest =
                accessTokenBuilder
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .header("Accept", "application/json")
                        .build();
            return null;
    }

    @PostMapping("/login")
    public ObjectNode login(@RequestBody JsonNode body) throws Exception {
        System.out.println("Auth controller started: login");

        if (!body.has("email") || !body.has("password"))
            throw new Exception("Missing Parameter");

        String userEmail = body.get("email").asText();
        String userPassword = body.get("password").asText();

        IEMDB.getInstance().login(userEmail, userPassword);

        if (IEMDB.getInstance().loginUser != null) {
            String jwt = createToken(userEmail);
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode resp = objectMapper.createObjectNode();
            resp.put("token", jwt);
            resp.put("userEmail", userEmail);
            return resp;
        }
        else
            return null;
    }

    @PostMapping("/signup")
    public User signup(@RequestBody JsonNode body) throws Exception {
        System.out.println("Auth controller started: signup");

        IEMDB.getInstance().login(body.get("email").asText(), body.get("password").asText());
        IemdbRepository repo = IemdbRepository.getInstance();
        int users_size = repo.getDataSize("User");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String birthDate = body.get("birthDate").asText();
        LocalDate localDate = LocalDate.parse(birthDate, formatter);

        if(repo.findUserByEmail(body.get("email").asText()) != null){
            System.out.println("User with this email already exists");
            return null;
        }

        User newUSer = new User(
                users_size + 1,
                body.get("email").asText(),
                body.get("password").asText(),
                body.get("username").asText(),
                body.get("name").asText(),
                localDate
        );

        try{
            repo.insertUser(newUSer);
            return newUSer;
        }catch (Exception e){
            System.out.println("User not inserted");
            return null;
        }
    }

    @PostMapping("/logout")
    public void logout(@RequestBody JsonNode body) throws Exception {
        System.out.println("Auth controller started: logout");

        if (IEMDB.getInstance().loginUser == null)
            throw new Exception("No user is logged in!");
        else
            IEMDB.getInstance().logout();
    }

    @PostMapping("/user")
    public User getUser(@RequestBody JsonNode body) throws Exception {
        System.out.println("AuthController started: getUser");
        if (IEMDB.getInstance().loginUser == null)
            return null;
        else
            return IEMDB.getInstance().loginUser;
    }


    private String createToken(String userEmail) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        Date exp = c.getTime();

        SecretKey key = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        String jws = Jwts.builder()
                .signWith(key)
                .setHeaderParam("typ", "JWT")
                .setIssuer("iemdb.ir")
                .setIssuedAt(new Date())
                .setExpiration(exp)
                .claim("userEmail", userEmail)
                .compact();

        return jws;
    }
}
