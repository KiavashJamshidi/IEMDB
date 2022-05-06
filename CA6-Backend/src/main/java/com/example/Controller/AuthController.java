package com.example.Controller;

import com.example.Model.IEMDB;
import com.example.Model.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    @PostMapping("/login")
    public User login(@RequestBody JsonNode body) throws Exception {
        System.out.println("Auth controller started: login");

        if (!body.has("email") || !body.has("password"))
            throw new Exception("Missing Parameter");

        IEMDB.getInstance().login(body.get("email").asText(), body.get("password").asText());
        if (IEMDB.getInstance().loginUser != null)
            return IEMDB.loginUser;
        else
            throw new Exception("No user found with this email!");
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
            throw new Exception("No user is logged in!");
        else
            return IEMDB.getInstance().loginUser;
    }
}
