package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.Notification;
import com.smartroomfinder.smartroomfinder.entities.Users;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(Users user);

    long countByUserAndIsReadFalse(Users user);
    @Modifying
    @Transactional
    @Query("""
    UPDATE Notification n
    SET n.isRead = true
    WHERE n.user.userId = :userId
""")
    int markAllAsRead(UUID userId);
}
