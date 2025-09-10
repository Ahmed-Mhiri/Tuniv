package com.tuniv.backend.qa.model;

import java.util.HashSet;
import java.util.Set;

import com.tuniv.backend.university.model.Module;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
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
// ✅ ADDED: A NamedEntityGraph to define the full data fetching plan.
@NamedEntityGraph(
    name = "question-with-full-tree",
    attributeNodes = {
        @NamedAttributeNode(value = "author"),
        @NamedAttributeNode(value = "module"),
        @NamedAttributeNode(value = "attachments"),
        @NamedAttributeNode(value = "answers", subgraph = "answers-subgraph") // Fetch answers and their children
    },
    subgraphs = {
        // Define what to fetch for each answer
        @NamedSubgraph(
            name = "answers-subgraph",
            attributeNodes = {
                @NamedAttributeNode("author"),
                @NamedAttributeNode("attachments"),
                @NamedAttributeNode(value = "comments", subgraph = "comments-subgraph") // Fetch comments and their children
            }
        ),
        // Define what to fetch for each comment (including replies)
        @NamedSubgraph(
            name = "comments-subgraph",
            attributeNodes = {
                @NamedAttributeNode("author"),
                @NamedAttributeNode("attachments"),
                // This is the crucial part that solves the N+1 on replies.
                // NOTE: This assumes your Comment entity has a field named 'children' for replies.
                // If it's named 'replies', please adjust this value.
                @NamedAttributeNode("children") 
            }
        )
    }
)
public class Question extends VotablePost {

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    // Note: @JsonBackReference is often problematic with DTOs. 
    // It might be better to handle object mapping explicitly in your mappers.
    // @JsonBackReference("module-questions") 
    private Module module;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    // @JsonManagedReference("question-answers")
    private Set<Answer> answers = new HashSet<>();
}