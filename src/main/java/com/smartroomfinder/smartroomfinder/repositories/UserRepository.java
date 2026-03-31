package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByEmail(String email);

    Optional<Users> findByGoogleId(String googleId);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM Users u WHERE u.role_id.roleName = 'ADMIN' AND u.isActive = true")
    List<Users> findAllAdmins();

    @Query("SELECT u FROM Users u WHERE u.role_id.roleName = :roleName")
    List<Users> findAllByRoleName(@Param("roleName") String roleName);
}
