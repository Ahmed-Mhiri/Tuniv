package com.tuniv.backend.user.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference; // <-- IMPORT ADDED
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Comment;
import com.tuniv.backend.qa.model.CommentVote;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.university.model.UniversityMembership;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonIgnore // Always ignore the password in JSON responses
    private String password;

    private String profilePhotoUrl;
    private String bio;
    private String major;
    private Integer reputationScore = 0;

    @OneToMany(mappedBy = "user")
    @JsonManagedReference("user-memberships")
    private Set<UniversityMembership> memberships = new HashSet<>();

    @OneToMany(mappedBy = "author")
    @JsonManagedReference("user-questions")
    private Set<Question> questions = new HashSet<>();

    @OneToMany(mappedBy = "author")
    @JsonManagedReference("user-answers")
    private Set<Answer> answers = new HashSet<>();
    
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    @JsonManagedReference("user-comments")
    private Set<Comment> comments = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonManagedReference("user-comment-votes")
    private Set<CommentVote> commentVotes = new HashSet<>();
}