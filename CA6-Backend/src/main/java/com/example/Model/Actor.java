package com.example.Model;

import lombok.Getter;
import lombok.Setter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Actor {
    @Getter @Setter public Integer Id;
    @Getter @Setter public String Name;
    @Getter @Setter public String BirthDate;
    @Getter @Setter public String Nationality;
    @Getter @Setter public String BirthDateString;
    @Getter @Setter public String Image;

    public Actor(Integer id, String name, String birthDate, String nationality, String img){
        Id = id;
        Name = name;
        BirthDate = birthDate;
        Nationality = nationality;
        Image = img;
        BirthDateString = birthDate;
    }
}
