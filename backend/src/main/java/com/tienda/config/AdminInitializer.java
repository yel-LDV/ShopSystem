package com.tienda.config;

import com.tienda.entity.AdminUser;
import com.tienda.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail("admin@tienda.com").isPresent()) {
            return;
        }

        AdminUser admin = new AdminUser();
        admin.setEmail("admin@tienda.com");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setFullName("Administrador");
        admin.setRole("ROLE_ADMIN");
        admin.setEnabled(true);
        userRepository.save(admin);
    }
}
