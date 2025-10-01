package com.tuniv.backend.qa.specification;

import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.qa.model.TopicType;
import com.tuniv.backend.qa.model.Tag;
import com.tuniv.backend.user.model.User;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.tuniv.backend.community.model.Community;

public class TopicSpecifications {

    // ✅ MAIN: Personalized feed specification with optimized OR conditions
    public static Specification<Topic> withPersonalizedFeed(
            List<Integer> userIds, List<Integer> communityIds, 
            List<Integer> tagIds, List<Integer> moduleIds) {
        
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // User-based filtering
            if (userIds != null && !userIds.isEmpty()) {
                predicates.add(root.get("author").get("userId").in(userIds));
            }
            
            // Community-based filtering
            if (communityIds != null && !communityIds.isEmpty()) {
                Join<Topic, Community> communityJoin = root.join("community", JoinType.LEFT);
                predicates.add(communityJoin.get("communityId").in(communityIds));
            }
            
            // Module-based filtering
            if (moduleIds != null && !moduleIds.isEmpty()) {
                Join<Topic, Module> moduleJoin = root.join("module", JoinType.LEFT);
                predicates.add(moduleJoin.get("moduleId").in(moduleIds));
            }
            
            // Tag-based filtering
            if (tagIds != null && !tagIds.isEmpty()) {
                Join<Topic, Tag> tagsJoin = root.join("tags", JoinType.INNER);
                predicates.add(tagsJoin.get("id").in(tagIds));
                
                // Ensure distinct results when joining tags (only for non-count queries)
                if (query.getResultType() != Long.class) {
                    query.distinct(true);
                }
            }
            
            // Return OR combination of all predicates, or all topics if no filters
            return predicates.isEmpty() ? cb.conjunction() : cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    // ✅ TIME-BASED FILTERING
    public static Specification<Topic> createdAfter(Instant since) {
        return (root, query, cb) -> since == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }

    public static Specification<Topic> createdBefore(Instant until) {
        return (root, query, cb) -> until == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), until);
    }

    public static Specification<Topic> createdBetween(Instant start, Instant end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return null;
            if (start == null) return cb.lessThanOrEqualTo(root.get("createdAt"), end);
            if (end == null) return cb.greaterThanOrEqualTo(root.get("createdAt"), start);
            return cb.between(root.get("createdAt"), start, end);
        };
    }

    // ✅ STATUS FILTERING
    public static Specification<Topic> isSolved(boolean solved) {
        return (root, query, cb) -> cb.equal(root.get("isSolved"), solved);
    }

    public static Specification<Topic> hasTopicType(TopicType topicType) {
        return (root, query, cb) -> topicType == null ? null : cb.equal(root.get("topicType"), topicType);
    }

    // ✅ SCORE/POPULARITY FILTERING
    public static Specification<Topic> withMinimumScore(int minScore) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("score"), minScore);
    }

    public static Specification<Topic> withMinimumReplies(int minReplies) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("replyCount"), minReplies);
    }

    // ✅ AUTHOR FILTERING
    public static Specification<Topic> byAuthor(Integer userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("author").get("userId"), userId);
    }

    public static Specification<Topic> byAuthors(List<Integer> userIds) {
        return (root, query, cb) -> userIds == null || userIds.isEmpty() ? null : root.get("author").get("userId").in(userIds);
    }

    // ✅ COMMUNITY/MODULE FILTERING
    public static Specification<Topic> inCommunity(Integer communityId) {
        return (root, query, cb) -> communityId == null ? null : cb.equal(root.get("community").get("communityId"), communityId);
    }

    public static Specification<Topic> inModule(Integer moduleId) {
        return (root, query, cb) -> moduleId == null ? null : cb.equal(root.get("module").get("moduleId"), moduleId);
    }

    public static Specification<Topic> inCommunities(List<Integer> communityIds) {
        return (root, query, cb) -> communityIds == null || communityIds.isEmpty() ? null : root.get("community").get("communityId").in(communityIds);
    }

    public static Specification<Topic> inModules(List<Integer> moduleIds) {
        return (root, query, cb) -> moduleIds == null || moduleIds.isEmpty() ? null : root.get("module").get("moduleId").in(moduleIds);
    }

    // ✅ TAG FILTERING
    public static Specification<Topic> hasTag(String tagName) {
        return (root, query, cb) -> {
            if (tagName == null || tagName.trim().isEmpty()) return null;
            Join<Topic, Tag> tagsJoin = root.join("tags", JoinType.INNER);
            if (query.getResultType() != Long.class) {
                query.distinct(true);
            }
            return cb.equal(tagsJoin.get("name"), tagName);
        };
    }

    public static Specification<Topic> hasTags(List<String> tagNames) {
        return (root, query, cb) -> {
            if (tagNames == null || tagNames.isEmpty()) return null;
            Join<Topic, Tag> tagsJoin = root.join("tags", JoinType.INNER);
            if (query.getResultType() != Long.class) {
                query.distinct(true);
            }
            return tagsJoin.get("name").in(tagNames);
        };
    }

    // ✅ SEARCH FILTERING
    public static Specification<Topic> titleContains(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) return null;
            return cb.like(cb.lower(root.get("title")), "%" + searchTerm.toLowerCase() + "%");
        };
    }

    public static Specification<Topic> bodyContains(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) return null;
            return cb.like(cb.lower(root.get("body")), "%" + searchTerm.toLowerCase() + "%");
        };
    }

    public static Specification<Topic> titleOrBodyContains(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) return null;
            String pattern = "%" + searchTerm.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("body")), pattern)
            );
        };
    }

    // ✅ COMBINATION SPECIFICATIONS
    public static Specification<Topic> popularTopics(int minScore, int minReplies) {
        return Specification.where(withMinimumScore(minScore))
                           .and(withMinimumReplies(minReplies));
    }

    public static Specification<Topic> recentUnsolvedQuestions() {
        return Specification.where(hasTopicType(TopicType.QUESTION))
                           .and(isSolved(false))
                           .and(createdAfter(Instant.now().minusSeconds(7 * 24 * 60 * 60))); // Last 7 days
    }

    public static Specification<Topic> userActivity(Integer userId, Instant since) {
        return Specification.where(byAuthor(userId))
                           .and(createdAfter(since));
    }

    // ✅ UTILITY: Combine multiple specifications with AND
    public static Specification<Topic> combineAnd(Specification<Topic>... specs) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Specification<Topic> spec : specs) {
                if (spec != null) {
                    Predicate predicate = spec.toPredicate(root, query, cb);
                    if (predicate != null) {
                        predicates.add(predicate);
                    }
                }
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ✅ UTILITY: Combine multiple specifications with OR
    public static Specification<Topic> combineOr(Specification<Topic>... specs) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Specification<Topic> spec : specs) {
                if (spec != null) {
                    Predicate predicate = spec.toPredicate(root, query, cb);
                    if (predicate != null) {
                        predicates.add(predicate);
                    }
                }
            }
            return predicates.isEmpty() ? null : cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}