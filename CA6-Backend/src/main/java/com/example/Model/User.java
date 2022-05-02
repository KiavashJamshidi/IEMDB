package com.example.Model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class User {
    public int Id;
    public String Email;
    public String Password;
    public String Nickname;
    public String Name;
    public LocalDate BirthDate;
    public List<Movie> watchList;

    public User(int id, String email, String password, String nickname, String name, LocalDate birthDate){
        Id = id;
        Email = email;
        Password = password;
        Nickname = nickname;
        Name = name;
        BirthDate = birthDate;
        watchList = new ArrayList<Movie>();
    }

    public void AddToWatchList(Movie movie){
        watchList.add(movie);
    }
    public void RemoveFromWatchList(Movie movie){
        watchList.remove(movie);
    }

}
