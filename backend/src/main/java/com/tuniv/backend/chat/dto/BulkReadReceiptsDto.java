package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class BulkReadReceiptsDto {
    private List<ReadReceiptDto> readReceipts;
    private Integer conversationId;

    public BulkReadReceiptsDto(List<ReadReceiptDto> readReceipts, Integer conversationId) {
        this.readReceipts = readReceipts;
        this.conversationId = conversationId;
    }
}