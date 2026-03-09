package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.IdentityVerification;
import com.smartroomfinder.smartroomfinder.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, Long> {

    Optional<IdentityVerification> findTopByUserOrderByCreatedAtDesc(Users user);

    boolean existsByUserAndStatus(Users user, String status);

    Optional<IdentityVerification> findByUserAndStatus(Users user, String status);

    long countByStatus(String status);
}