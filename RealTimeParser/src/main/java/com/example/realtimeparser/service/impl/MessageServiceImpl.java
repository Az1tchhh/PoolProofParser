package com.example.realtimeparser.service.impl;

import com.example.realtimeparser.config.HttpUtils;
import com.example.realtimeparser.entity.Message;
import com.example.realtimeparser.entity.dto.ErrorMessage;
import com.example.realtimeparser.entity.dto.FetchResponse;
import com.example.realtimeparser.entity.dto.SuccessMessage;
import com.example.realtimeparser.repository.MessageRepository;
import com.example.realtimeparser.service.MessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    @Value("${fetcherUrl}")
    private String fectherUrl;
    public MessageServiceImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    @Async
    public CompletableFuture<ResponseEntity<?>> fetchChannel() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ResponseEntity<List<Message>> response;
                try {
                    response = HttpUtils.sendGetRequest(fectherUrl);
                    if(response.getStatusCode().is2xxSuccessful()){
                        Long allRows = messageRepository.countAllMessages();
                        if(allRows == 0){
                            messageRepository.saveAll(Objects.requireNonNull(response.getBody()));
                            SuccessMessage successMessage = new SuccessMessage("Messages were saved into database");
                            return new ResponseEntity<>(successMessage, HttpStatus.OK);
                        }
                        else if(Objects.requireNonNull(response.getBody()).size() == allRows){
                            SuccessMessage successMessage = new SuccessMessage("No new messages yet");
                            return new ResponseEntity<>(successMessage, HttpStatus.OK);
                        }
                        else {
                            Message message = response.getBody().get((int) (allRows-1));
                            messageRepository.save(message);
                            SuccessMessage successMessage = new SuccessMessage("New message save into database");
                            return new ResponseEntity<>(successMessage, HttpStatus.OK);
                        }
                    }
                    else{
                        ErrorMessage errorMessageResponse = new ErrorMessage("Could bot request python server");
                        return new ResponseEntity<>(errorMessageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                } catch (Exception e) {
                    ErrorMessage errorMessageResponse = new ErrorMessage("Connection between services corrupted");
                    return new ResponseEntity<>(errorMessageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } catch (Exception e) {
                ErrorMessage errorMessage = new ErrorMessage("Operation failed");
                return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }

    @Override
    @Async
    public CompletableFuture<ResponseEntity<?>> findAllMessages() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Message> messages = messageRepository.findAll();
                return new ResponseEntity<>(messages, HttpStatus.OK);
            }catch (Exception e){
                ErrorMessage errorMessage = new ErrorMessage("Operation failed");
                return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }

    @Override
    @Async
    public CompletableFuture<ResponseEntity<?>> findLastNMessages(Integer n) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Message> lastNMessages = messageRepository.findBottomN(n);
                return new ResponseEntity<>(lastNMessages, HttpStatus.OK);
            } catch (Exception e) {
                ErrorMessage errorMessage = new ErrorMessage("Error fetching last N messages");
                return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }

    @Override
    @Async
    public CompletableFuture<ResponseEntity<?>> findByDate(LocalDate date){
        return CompletableFuture.supplyAsync(() -> {
            try {
                LocalDate startDate = date.atStartOfDay().toLocalDate();
                LocalDate endDate = startDate.plusDays(1);
                List<Message> messages = messageRepository.findByDateBetween(startDate.atStartOfDay(), endDate.atStartOfDay());
                if (messages.isEmpty()) {
                    ErrorMessage errorMessage = new ErrorMessage("No messages found for the given date");
                    return new ResponseEntity<>(errorMessage, HttpStatus.NOT_FOUND);
                }
                return new ResponseEntity<>(messages, HttpStatus.OK);
            } catch (Exception e) {
                ErrorMessage errorMessage = new ErrorMessage("Error fetching messages by date");
                return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }

    @Override
    @Scheduled(fixedDelay = 60000) // Run every minute
    public void fetchChannelPeriodically() {
        CompletableFuture<ResponseEntity<?>> future = fetchChannel();
        future.thenAccept(responseEntity -> {
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                System.out.println("Fetch and save operation successful: " + responseEntity.getBody());
            } else {
                System.err.println("Error during fetch and save operation: " + responseEntity.getBody());
            }
        });
    }
}
