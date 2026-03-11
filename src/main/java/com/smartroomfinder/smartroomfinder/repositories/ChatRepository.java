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

    /**
     * Lấy toàn bộ lịch sử chat giữa 2 user, sắp xếp tăng dần theo thời gian.
     */
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

    /**
     * Lấy tin nhắn cuối cùng của mỗi cuộc trò chuyện mà user tham gia.
     * Dùng subquery để lấy MAX(createdAt) per partner.
     */
    @Query("""
        SELECT m FROM ChatMessage m
        JOIN FETCH m.sender s
        JOIN FETCH m.receiver r
        WHERE m.isDeleted = false
          AND m.createdAt IN (
            SELECT MAX(cm.createdAt)
            FROM ChatMessage cm
            WHERE cm.isDeleted = false
              AND (cm.sender.userId = :userId OR cm.receiver.userId = :userId)
            GROUP BY
              CASE
                WHEN cm.sender.userId = :userId THEN cm.receiver.userId
                ELSE cm.sender.userId
              END
          )
          AND (m.sender.userId = :userId OR m.receiver.userId = :userId)
        ORDER BY m.createdAt DESC
    """)
    List<ChatMessage> findLatestConversations(@Param("userId") UUID userId);

    /**
     * Đếm số tin nhắn chưa đọc từ một sender cụ thể gửi cho receiver.
     */
    @Query("""
        SELECT COUNT(m) FROM ChatMessage m
        WHERE m.sender.userId = :senderId
          AND m.receiver.userId = :receiverId
          AND m.isRead = false
          AND m.isDeleted = false
    """)
    long countUnread(@Param("senderId") UUID senderId, @Param("receiverId") UUID receiverId);

    /**
     * Đánh dấu tất cả tin nhắn từ sender gửi cho receiver là đã đọc.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE ChatMessage m
        SET m.isRead = true, m.readAt = :readAt
        WHERE m.sender.userId = :senderId
          AND m.receiver.userId = :receiverId
          AND m.isRead = false
    """)
    int markAsRead(
            @Param("senderId") UUID senderId,
            @Param("receiverId") UUID receiverId,
            @Param("readAt") LocalDateTime readAt
    );
}