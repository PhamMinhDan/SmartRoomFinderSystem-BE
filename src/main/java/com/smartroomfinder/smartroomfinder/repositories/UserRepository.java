package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByEmail(String email);

    Optional<Users> findByUsername(String username);

    Optional<Users> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByGoogleId(String googleId);

    Optional<Users> findByFacebookId(String facebookId);

    boolean existsByFacebookId(String facebookId);
}
