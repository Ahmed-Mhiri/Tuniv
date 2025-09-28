package com.tuniv.backend.qa.model;

import java.util.HashSet;
import java.util.Set;

import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.university.model.Module;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("QUESTION")
@Getter
@Setter
@NamedEntityGraph(
    name = "question-with-full-tree",
    attributeNodes = {
        @NamedAttributeNode(value = "author"),
        @NamedAttributeNode(value = "module"),
        @NamedAttributeNode(value = "community"), // NEW: Added community
        @NamedAttributeNode(value = "attachments"),
        @NamedAttributeNode(value = "tags"),
        @NamedAttributeNode(value = "answers", subgraph = "answers-subgraph")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "answers-subgraph",
            attributeNodes = {
                @NamedAttributeNode("author"),
                @NamedAttributeNode("attachments"),
                @NamedAttributeNode(value = "comments", subgraph = "comments-subgraph")
            }
        ),
        @NamedSubgraph(
            name = "comments-subgraph",
            attributeNodes = {
                @NamedAttributeNode("author"),
                @NamedAttributeNode("attachments"),
                @NamedAttributeNode("children") 
            }
        )
    }
)
public class Question extends VotablePost {

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id") // CHANGED: nullable = true now
    private Module module;

    // NEW: Added community relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id") // nullable = true
    private Community community;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Answer> answers = new HashSet<>();
    
    @Column(name = "answer_count", nullable = false)
    private int answerCount = 0;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
        name = "post_tags",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();
}