package com.tuniv.backend.chat.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequest {
    
    @NotBlank(message = "Group title cannot be empty")
    @Size(max = 255, message = "Group title cannot exceed 255 characters")
    private String title;
    
    @NotNull(message = "Participant list is required")
    private List<Integer> participantIds;
    
    private Integer universityContextId;
}