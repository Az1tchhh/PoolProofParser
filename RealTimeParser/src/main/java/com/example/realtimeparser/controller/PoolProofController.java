package com.example.realtimeparser.controller;

import com.example.realtimeparser.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class PoolProofController {
    private final MessageService messageService;

    public PoolProofController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/fetch_channel")
    public ResponseEntity<?> fetchChannel() throws ExecutionException, InterruptedException {
        CompletableFuture<ResponseEntity<?>> futureResponse = messageService.fetchChannel();
        return futureResponse.get();
    }
    @GetMapping("/messages/all")
    public ResponseEntity<?> findAllMessages() throws ExecutionException, InterruptedException {
        CompletableFuture<ResponseEntity<?>> futureResponse = messageService.findAllMessages();
        return futureResponse.get();
    }
    @GetMapping("/messages")
    public ResponseEntity<?> findNMessages(@RequestParam Integer n) throws ExecutionException, InterruptedException {
        CompletableFuture<ResponseEntity<?>> futureResponse = messageService.findLastNMessages(n);
        return futureResponse.get();
    }
    @GetMapping("/messages/by_date")
    public ResponseEntity<?> findMessagesByDay(@RequestParam LocalDate date) throws ExecutionException, InterruptedException {
        CompletableFuture<ResponseEntity<?>> futureResponse = messageService.findByDate(date);
        return futureResponse.get();
    }
}
