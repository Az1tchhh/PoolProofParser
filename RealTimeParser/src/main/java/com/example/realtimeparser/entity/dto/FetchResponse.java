package com.example.realtimeparser.entity.dto;

import com.example.realtimeparser.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FetchResponse {
    private List<Message> messages;
}
