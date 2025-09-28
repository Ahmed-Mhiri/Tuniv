package com.tuniv.backend.qa.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
public abstract class VotablePost extends Post {

    @Column(name = "score", nullable = false)
    private int score = 0;


    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;



    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Vote> votes = new HashSet<>();

}
