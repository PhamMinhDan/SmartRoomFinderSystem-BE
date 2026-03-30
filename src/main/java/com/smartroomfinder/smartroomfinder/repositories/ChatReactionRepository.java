package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.ChatReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatReactionRepository extends JpaRepository<ChatReaction, Long> {

    Optional<ChatReaction> findByMessage_MessageIdAndUser_UserId(Long messageId, UUID userId);

    List<ChatReaction> findAllByMessage_MessageId(Long messageId);

    @Query("""
        SELECT r FROM ChatReaction r
        WHERE r.message.messageId IN :messageIds
    """)
    List<ChatReaction> findAllByMessageIds(@Param("messageIds") List<Long> messageIds);
}