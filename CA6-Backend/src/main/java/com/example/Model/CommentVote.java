package com.example.Model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CommentVote {
    String UserEmail;
    Integer CommentId, Vote;

    public CommentVote(String userEmail, Integer commentId, Integer vote){
        UserEmail = userEmail;
        CommentId = commentId;
        Vote = vote;
    }
}
