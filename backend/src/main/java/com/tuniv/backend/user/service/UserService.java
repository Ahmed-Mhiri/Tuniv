package com.tuniv.backend.user.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.community.dto.CommunitySummaryDto;
import com.tuniv.backend.community.mapper.CommunityMapper;
import com.tuniv.backend.community.repository.CommunityMembershipRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.follow.model.FollowableType;
import com.tuniv.backend.follow.repository.FollowRepository;
import com.tuniv.backend.qa.dto.ReplyResponseDto;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.dto.VoteInfo;
import com.tuniv.backend.qa.mapper.TopicMapper;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.qa.repository.ReplyRepository;
import com.tuniv.backend.qa.repository.TopicRepository;
import com.tuniv.backend.qa.repository.VoteRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.dto.LeaderboardUserDto;
import com.tuniv.backend.user.dto.UserProfileDto;
import com.tuniv.backend.user.dto.UserProfileUpdateRequest;
import com.tuniv.backend.user.mapper.UserMapper;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final ReplyRepository replyRepository;
    private final FollowRepository followRepository;
    private final VoteRepository voteRepository;
    private final CommunityMembershipRepository communityMembershipRepository;
    private final CommunityMapper communityMapper;

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfileById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        long topicsCount = topicRepository.countByAuthor_UserId(userId);
        long answersCount = replyRepository.countAnswersByUser(userId);
        long followersCount = followRepository.countByTargetTypeAndTargetId(FollowableType.USER, userId);

        return UserMapper.toUserProfileDto(user, topicsCount, answersCount, followersCount);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile(UserDetailsImpl currentUser) {
        Integer userId = currentUser.getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        long topicsCount = topicRepository.countByAuthor_UserId(userId);
        long answersCount = replyRepository.countAnswersByUser(userId);
        long followersCount = followRepository.countByTargetTypeAndTargetId(FollowableType.USER, userId);
        
        return UserMapper.toUserProfileDto(user, topicsCount, answersCount, followersCount);
    }

    @Transactional
    public UserProfileDto updateCurrentUserProfile(UserDetailsImpl currentUser, UserProfileUpdateRequest updateRequest) {
        User userToUpdate = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));

        if (updateRequest.bio() != null) {
            userToUpdate.setBio(updateRequest.bio());
        }
        if (updateRequest.major() != null) {
            userToUpdate.setMajor(updateRequest.major());
        }
        if (updateRequest.profilePhotoUrl() != null) {
            userToUpdate.setProfilePhotoUrl(updateRequest.profilePhotoUrl());
        }

        User updatedUser = userRepository.save(userToUpdate);

        long topicsCount = topicRepository.countByAuthor_UserId(updatedUser.getUserId());
        long answersCount = replyRepository.countAnswersByUser(updatedUser.getUserId());
        long followersCount = followRepository.countByTargetTypeAndTargetId(FollowableType.USER, updatedUser.getUserId());

        return UserMapper.toUserProfileDto(updatedUser, topicsCount, answersCount, followersCount);
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryDto> getUserCommunities(UserDetailsImpl currentUser) {
        List<CommunityMembership> memberships = communityMembershipRepository.findByUser_UserId(currentUser.getId());
        
        return memberships.stream()
                .map(membership -> communityMapper.toSummaryDto(membership.getCommunity()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardUserDto> getLeaderboardUsers() {
        return userRepository.findTop5ByOrderByReputationScoreDesc().stream()
                .map(UserMapper::toLeaderboardUserDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TopicSummaryDto> getUserTopics(Integer userId, UserDetailsImpl currentUser) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        
        List<Topic> topics = topicRepository.findByAuthor_UserIdOrderByCreatedAtDesc(userId);
        Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        
        List<Integer> topicIds = topics.stream().map(Topic::getId).collect(Collectors.toList());
        
        // Create final copy for lambda usage
        final Map<Integer, Integer> currentUserVotes;
        if (currentUserId != null && !topicIds.isEmpty()) {
            List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUserId, topicIds);
            currentUserVotes = votes.stream()
                    .collect(Collectors.toMap(VoteInfo::postId, VoteInfo::value));
        } else {
            currentUserVotes = Collections.emptyMap();
        }

        return topics.stream()
                .map(topic -> {
                    String voteType = getVoteType(currentUserVotes.get(topic.getId()));
                    Integer containerId = topic.getModule() != null ? topic.getModule().getModuleId() : 
                                        topic.getCommunity() != null ? topic.getCommunity().getCommunityId() : null;
                    String containerName = topic.getModule() != null ? topic.getModule().getName() : 
                                         topic.getCommunity() != null ? topic.getCommunity().getName() : null;
                    
                    return TopicMapper.toTopicSummaryDto(topic, voteType, containerId, containerName);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getUserAnswers(Integer userId, UserDetailsImpl currentUser) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        
        List<Reply> answers = replyRepository.findAnswersByUser(userId);
        return convertRepliesToDtos(answers, currentUser);
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getUserComments(Integer userId, UserDetailsImpl currentUser) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId); // Fixed duplicate 'new'
        }
        
        List<Reply> comments = replyRepository.findCommentsByUser(userId);
        return convertRepliesToDtos(comments, currentUser);
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getAllUserReplies(Integer userId, UserDetailsImpl currentUser) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        
        List<Reply> allReplies = replyRepository.findByAuthor_UserIdOrderByCreatedAtDesc(userId);
        return convertRepliesToDtos(allReplies, currentUser);
    }

    private List<ReplyResponseDto> convertRepliesToDtos(List<Reply> replies, UserDetailsImpl currentUser) {
        final Integer currentUserId = (currentUser != null) ? currentUser.getId() : null;
        
        List<Integer> replyIds = replies.stream().map(Reply::getId).collect(Collectors.toList());
        
        // Create final copy for lambda usage
        final Map<Integer, Integer> currentUserVotes;
        if (currentUserId != null && !replyIds.isEmpty()) {
            List<VoteInfo> votes = voteRepository.findAllVotesForUserByPostIds(currentUserId, replyIds);
            currentUserVotes = votes.stream()
                    .collect(Collectors.toMap(VoteInfo::postId, VoteInfo::value));
        } else {
            currentUserVotes = Collections.emptyMap();
        }

        return replies.stream()
                .map(reply -> TopicMapper.toReplyResponseDto(reply, currentUser, currentUserVotes))
                .collect(Collectors.toList());
    }

    private String getVoteType(Integer voteValue) {
        if (voteValue == null) return null;
        return voteValue == 1 ? "UPVOTE" : voteValue == -1 ? "DOWNVOTE" : null;
    }
}