package com.example.Model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Setter;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Comment {
    @Getter @Setter public Integer Id, Likes, Dislikes;
    @Getter @Setter public String UserEmail;
    @Getter @Setter public Integer MovieId;
    @Getter @Setter public String Text;
    @Getter @Setter public String CreationDate;
    @Getter @Setter public List<CommentVote> Votes;

    public Comment(Integer id, String userEmail, Integer movieId, String text) {
        Id = id;
        UserEmail = userEmail;
        MovieId = movieId;
        Text = text;
        CreationDate = LocalDate.now().toString();
        Likes = 0;
        Dislikes = 0;
        Votes = new ArrayList<>();
    }

    public void AddVote(CommentVote commentVote){
        Votes.add(commentVote);
        UpdateVotes();
    }

    private void UpdateVotes(){
        Likes = 0;
        Dislikes = 0;
        for(CommentVote vote : Votes){
            if(vote.Vote == 1)
                Likes += 1;
            else if(vote.Vote == -1)
                Dislikes += 1;
        }
    }

    public boolean checkForVoteUpdates(String userEmail, Integer vote){
        for(CommentVote cmntVote : Votes){
            if(cmntVote.UserEmail.equals(userEmail)) {
                cmntVote.Vote = vote;
                UpdateVotes();
                return true;
            }
        }
        return false;
    }

}
