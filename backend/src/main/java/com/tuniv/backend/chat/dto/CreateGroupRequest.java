// CreateGroupRequest.java
package com.tuniv.backend.chat.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequest {
    private String title;
    private List<Integer> participantIds;
    private Integer universityContextId; // Optional
}