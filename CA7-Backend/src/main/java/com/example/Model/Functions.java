package com.example.Model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Functions {

    public static List<Integer> ConvertStringToIntList(String str){
        str = str.substring(1,str.length() - 1);
        List<Integer> myListInt = new ArrayList<Integer>();
        if(str.length() == 0)
            return  myListInt;
        List<String> myList = new ArrayList<String>(Arrays.asList(str.split(",")));
        myListInt = myList.stream().map(Integer::parseInt).collect(Collectors.toList());
        return myListInt;
    }

    public static List<String> ConvertStringToStringList(JSONArray arr){
        List<String> myList = new ArrayList<String>();
        for(Object obj : arr){
            myList.add((String)obj);
        }
        return myList;
    }
    public static String RemoveBracket(List<String> list){
        String stringList = list.toString();
        return stringList.substring(1,stringList.length() - 1);
    }
}
