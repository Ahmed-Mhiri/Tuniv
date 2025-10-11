package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class UpdateParticipantsRequest {
    private List<Integer> userIds;
}