package com.example.realtimeparser.repository;

import com.example.realtimeparser.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    public Message findByDate(LocalDateTime localDateTime);

    @Query("SELECT COUNT(m) FROM Message m")
    Long countAllMessages();

    @Query(value = "SELECT * FROM messages ORDER BY date DESC LIMIT :n", nativeQuery = true)
    List<Message> findBottomN(Integer n);

    List<Message> findByDateBetween(LocalDateTime date, LocalDateTime date2);
}
