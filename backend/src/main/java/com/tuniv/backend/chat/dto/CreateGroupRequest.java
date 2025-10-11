package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class CreateGroupRequest {
    private String title;
    private List<Integer> participantIds;
    private Integer universityContextId; // Optional
}