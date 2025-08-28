package com.tuniv.backend.qa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.Attachment;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Integer> {
        List<Attachment> findAllByPostTypeAndPostIdIn(String postType, List<Integer> postIds);

}