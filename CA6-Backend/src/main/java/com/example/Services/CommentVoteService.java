package com.example.Services;

import com.example.Model.Comment;
import com.example.Model.CommentVote;
import com.example.Model.ErrorHandler;
import com.example.Model.User;
import com.example.Repository.IemdbRepository;
import org.json.JSONObject;

import java.util.List;

public class CommentVoteService {
    ErrorHandler errorHandler = new ErrorHandler();
    public JSONObject VoteComment(JSONObject jsonObject, List<User> users, List<Comment> comments) throws Exception {
        Integer commentId = Integer.parseInt(jsonObject.getString("commentId"));
        String userEmail = jsonObject.get("userEmail").toString();
        int vote;
        try {
            vote = Integer.parseInt(jsonObject.getString("vote"));
            System.out.println("Vote is ");
            System.out.println(vote);
        } catch(NumberFormatException e){
            return errorHandler.fail("InvalidVoteValue");
        }

        if (!(vote == 1 || vote == -1 || vote == 0)) return errorHandler.fail("InvalidVoteValue");

        CommentVote newCommentVote = new CommentVote(
                userEmail,
                commentId,
                vote
        );

        IemdbRepository.getInstance().insertCommentVote(newCommentVote);
        return errorHandler.success("comment voted successfully");
    }
}