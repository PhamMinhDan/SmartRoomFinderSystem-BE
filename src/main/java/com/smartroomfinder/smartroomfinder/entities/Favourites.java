package com.smartroomfinder.smartroomfinder.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "favourites",
        indexes = {
                @Index(name = "idx_fav_user_id", columnList = "user_id"),
                @Index(name = "idx_fav_room_id", columnList = "room_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_fav_user_room", columnNames = {"user_id", "room_id"})
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Favourites {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fav_id")
    private Long favId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Rooms room;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}