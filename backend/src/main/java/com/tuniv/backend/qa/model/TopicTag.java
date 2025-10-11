package com.tuniv.backend.qa.model;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "topic_tags")
@Getter
@Setter
@NoArgsConstructor
public class TopicTag extends Auditable {

    @EmbeddedId
    private TopicTagId id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("topicId")
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    // ✅ IMPROVED: Proper JPA relationship instead of raw ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_user_id")
    private User addedBy;

    // Constructor
    public TopicTag(Topic topic, Tag tag, User addedBy) {
        this.id = new TopicTagId(topic.getId(), tag.getId());
        this.topic = topic;
        this.tag = tag;
        this.addedBy = addedBy; // Now directly set the User object
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicTagId implements Serializable {
        @Column(name = "topic_id")
        private Integer topicId;

        @Column(name = "tag_id")
        private Integer tagId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TopicTagId that = (TopicTagId) o;
            return Objects.equals(topicId, that.topicId) && Objects.equals(tagId, that.tagId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(topicId, tagId);
        }
    }
}