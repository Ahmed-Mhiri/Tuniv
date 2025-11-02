package com.tuniv.backend.chat.dto.request;

import java.util.List;

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
public class UpdateParticipantsRequest {
    
    @NotNull(message = "User IDs list is required")
    @Size(min = 1, message = "At least one user ID must be provided")
    private List<Integer> userIds;
}