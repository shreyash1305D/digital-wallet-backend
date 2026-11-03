package com.techpulse.wallet.config;

import com.techpulse.wallet.entity.*;
import com.techpulse.wallet.entity.Role;
import com.techpulse.wallet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder,
                                @Value("${app.admin.email}") String email,
                                @Value("${app.admin.password}") String password) {
        return args -> {
            if (!users.existsByEmail(email))
                users.save(new AppUser(email, encoder.encode(password),
                        Role.ADMIN));
        };
    }
}
