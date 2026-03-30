package com.smartroomfinder.smartroomfinder.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id"}),
        indexes = @Index(name = "idx_reaction_message", columnList = "message_id")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "emoji", nullable = false, length = 10)
    private String emoji;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}