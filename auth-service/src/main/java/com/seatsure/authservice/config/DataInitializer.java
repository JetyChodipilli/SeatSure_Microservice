package com.seatsure.authservice.config;

import com.seatsure.authservice.entity.Role;
import com.seatsure.authservice.entity.User;
import com.seatsure.authservice.repository.RoleRepository;
import com.seatsure.authservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {

        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        createRoles();

        createDefaultAdmin();
    }

    private void createRoles() {

        if (roleRepository.findByName("ROLE_USER").isEmpty()) {

            Role userRole = new Role();
            userRole.setName("ROLE_USER");

            roleRepository.save(userRole);
        }

        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {

            Role adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");

            roleRepository.save(adminRole);
        }
    }

    private void createDefaultAdmin() {

        if (userRepository.existsByEmail("admin@seatsure.com")) {
            return;
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow();

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow();

        User admin = new User();

        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setEmail("admin@seatsure.com");
        admin.setPhone("9999999999");

        admin.setPassword(
                passwordEncoder.encode("Admin@123"));

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        roles.add(userRole);

        admin.setRoles(roles);

        userRepository.save(admin);

        System.out.println("====================================");
        System.out.println("Default Admin Created");
        System.out.println("Email    : admin@seatsure.com");
        System.out.println("Password : Admin@123");
        System.out.println("====================================");
    }
}