package com.tuniv.backend.user.model;

import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.university.model.UniversityMembership;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String profilePhotoUrl;
    private String bio;
    private String major;
    private Integer reputationScore = 0;

    @OneToMany(mappedBy = "user")
    private Set<UniversityMembership> memberships;

    @OneToMany(mappedBy = "author")
    private Set<Question> questions;

    @OneToMany(mappedBy = "author")
    private Set<Answer> answers;
}