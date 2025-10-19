// BulkMessageDeletionDto.java
package com.tuniv.backend.chat.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkMessageDeletionDto {
    private List<Integer> messageIds;
    private Integer conversationId;
    private Instant deletedAt;
}