package com.gamereleasetracker.config;

import com.gamereleasetracker.model.Role;
import com.gamereleasetracker.model.RoleType;
import com.gamereleasetracker.model.User; // Import User
import com.gamereleasetracker.repository.RoleRepository;
import com.gamereleasetracker.repository.UserRepository; // Import UserRepository
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder; // Import PasswordEncoder
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RoleRepository roleRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        seedRoles();
        seedAdminUser();
    }

    private void seedRoles() {
        // Check if roles are already in the database
        if (roleRepository.count() == 0) {
            // Create ROLE_USER
            Role userRole = new Role();
            userRole.setName(RoleType.ROLE_USER);
            roleRepository.save(userRole);

            // Create ROLE_ADMIN
            Role adminRole = new Role();
            adminRole.setName(RoleType.ROLE_ADMIN);
            roleRepository.save(adminRole);

            System.out.println("Roles have been seeded to the database.");
        } else {
            System.out.println("Roles already exist in the database.");
        }
    }

    private void seedAdminUser() {
        // Check if the admin user already exists
        if (userRepository.findByUsername("admin").isEmpty()) {

            // 1. Find the ROLE_ADMIN
            Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: ADMIN_ROLE not found."));

            // 2. Create the new admin user
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setEmail("admin@example.com"); // Use a real email if desired

            // 3. Hash the password
            // You can load this from application.properties for better security
            adminUser.setPasswordHash(passwordEncoder.encode("adm1nP@ssw0rd"));

            adminUser.setRole(adminRole);

            // Set Spring Security default fields
            adminUser.setEnabled(true);
            adminUser.setAccountNonExpired(true);
            adminUser.setCredentialsNonExpired(true);
            adminUser.setAccountNonLocked(true);

            // 4. Save the user
            userRepository.save(adminUser);
            System.out.println("Default admin user has been seeded.");

        } else {
            System.out.println("Admin user already exists in the database.");
        }
    }
}