package com.smartroomfinder.smartroomfinder.seed;

import com.smartroomfinder.smartroomfinder.entities.Roles;
import com.smartroomfinder.smartroomfinder.repositories.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleSeeder {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void init() {

        addRoleIfNotExists("ADMIN", "Quản trị hệ thống");
        addRoleIfNotExists("LANDLORD", "Chủ trọ / người đăng phòng");
        addRoleIfNotExists("RENTER", "Người thuê trọ");

        System.out.println(">>> ROLE SEEDER: Initialized sample roles");
    }

    private void addRoleIfNotExists(String roleName, String description) {
        roleRepository.findByRoleName(roleName).ifPresentOrElse(
                r -> {},
                () -> {
                    Roles role = Roles.builder()
                            .roleName(roleName)
                            .description(description)
                            .build();

                    roleRepository.save(role);
                }
        );
    }
}
