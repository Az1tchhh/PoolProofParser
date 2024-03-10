package com.example.realtimeparser.service;

import com.example.realtimeparser.entity.Message;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MessageService {
    CompletableFuture<ResponseEntity<?>> fetchChannel();
    CompletableFuture<ResponseEntity<?>> findAllMessages();
    void fetchChannelPeriodically();
    CompletableFuture<ResponseEntity<?>> findByDate(LocalDate date);
    CompletableFuture<ResponseEntity<?>> findLastNMessages(Integer n);
}
