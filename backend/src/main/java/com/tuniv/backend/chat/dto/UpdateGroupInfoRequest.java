// UpdateGroupInfoRequest.java
package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupInfoRequest {
    private String title;
    private String description;
    private String iconUrl;
}