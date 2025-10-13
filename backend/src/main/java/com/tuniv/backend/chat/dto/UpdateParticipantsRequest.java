// UpdateParticipantsRequest.java
package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateParticipantsRequest {
    private List<Integer> userIds;
}