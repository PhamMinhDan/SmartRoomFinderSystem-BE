package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
        SELECT DISTINCT m FROM ChatMessage m
        JOIN FETCH m.sender
        JOIN FETCH m.receiver
        LEFT JOIN FETCH m.attachments
        WHERE m.isDeleted = false
          AND (
            (m.sender.userId = :user1 AND m.receiver.userId = :user2)
            OR
            (m.sender.userId = :user2 AND m.receiver.userId = :user1)
          )
        ORDER BY m.createdAt ASC
    """)
    List<ChatMessage> getConversationWithAttachments(@Param("user1") UUID user1,
                                                     @Param("user2") UUID user2);

    @Query("""
        SELECT m FROM ChatMessage m
        JOIN FETCH m.sender
        JOIN FETCH m.receiver
        WHERE m.isDeleted = false
          AND (
            (m.sender.userId = :user1 AND m.receiver.userId = :user2)
            OR
            (m.sender.userId = :user2 AND m.receiver.userId = :user1)
          )
        ORDER BY m.createdAt ASC
    """)
    List<ChatMessage> getConversation(@Param("user1") UUID user1, @Param("user2") UUID user2);


    @Query("""
        SELECT m FROM ChatMessage m
        JOIN FETCH m.sender s
        JOIN FETCH m.receiver r
        LEFT JOIN FETCH m.attachments
        WHERE m.isDeleted = false
          AND m.messageId IN (
            SELECT MAX(cm.messageId)
            FROM ChatMessage cm
            WHERE cm.isDeleted = false
              AND (cm.sender.userId = :userId OR cm.receiver.userId = :userId)
            GROUP BY
              CASE
                WHEN cm.sender.userId = :userId THEN cm.receiver.userId
                ELSE cm.sender.userId
              END
          )
        ORDER BY m.createdAt DESC
    """)
    List<ChatMessage> findLatestConversations(@Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(m) FROM ChatMessage m
        WHERE m.sender.userId = :senderId
          AND m.receiver.userId = :receiverId
          AND m.isRead = false
          AND m.isDeleted = false
    """)
    long countUnread(@Param("senderId") UUID senderId, @Param("receiverId") UUID receiverId);

    @Modifying
    @Transactional
    @Query("""
        UPDATE ChatMessage m
        SET m.isRead = true, m.readAt = :readAt
        WHERE m.sender.userId = :senderId
          AND m.receiver.userId = :receiverId
          AND m.isRead = false
    """)
    int markAsRead(@Param("senderId") UUID senderId,
                   @Param("receiverId") UUID receiverId,
                   @Param("readAt") LocalDateTime readAt);

    @Query("""
        SELECT COUNT(m) FROM ChatMessage m
        WHERE (
            (m.sender.userId = :user1 AND m.receiver.userId = :user2)
            OR
            (m.sender.userId = :user2 AND m.receiver.userId = :user1)
        )
    """)
    long countConversation(@Param("user1") UUID user1, @Param("user2") UUID user2);

    @Query("""
        SELECT MAX(m.createdAt) FROM ChatMessage m
        WHERE (
            (m.sender.userId = :user1 AND m.receiver.userId = :user2)
            OR
            (m.sender.userId = :user2 AND m.receiver.userId = :user1)
        )
    """)
    LocalDateTime getLastMessageTime(@Param("user1") UUID user1, @Param("user2") UUID user2);
}