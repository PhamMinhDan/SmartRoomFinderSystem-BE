package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Roles, Integer> {
    Optional<Roles> findByRoleName(String roleName);
    boolean existsByRoleName(String roleName);
}
