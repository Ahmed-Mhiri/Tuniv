// BulkReadReceiptsDto.java
package com.tuniv.backend.chat.dto.event;

import java.util.List;

import com.tuniv.backend.chat.dto.common.ReadReceiptDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkReadReceiptsDto {
    private List<ReadReceiptDto> readReceipts;
    private Integer conversationId;
}