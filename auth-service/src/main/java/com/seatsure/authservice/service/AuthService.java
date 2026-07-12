package com.seatsure.authservice.service;

import com.seatsure.authservice.dto.RegisterRequest;
import com.seatsure.authservice.entity.Role;
import com.seatsure.authservice.entity.User;
import com.seatsure.authservice.exception.EmailAlreadyExistsException;
import com.seatsure.authservice.exception.ResourceNotFoundException;
import com.seatsure.authservice.repository.RoleRepository;
import com.seatsure.authservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(RegisterRequest request){
        //check Duplicate email
        if(userRepository.existsByEmail(request.getEmail()))
            throw new EmailAlreadyExistsException("Email already registered" + request.getEmail());

            // 2. Get ROLE_USER
        Role role = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() ->
                            new ResourceNotFoundException("ROLE_USER not found"));

            // 3. Create User entity
            User user = new User();

            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());

            // Encrypt password
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            user.setPhone(request.getPhone());

            // 4. Assign role
            Set<Role> roles = new HashSet<>();
            roles.add(role);

            user.setRoles(roles);

            // 5. Save user
            userRepository.save(user);

            return "User registered successfully.";
        }
}
