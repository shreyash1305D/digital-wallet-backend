package com.techpulse.wallet.service;

import com.techpulse.wallet.dto.*;
import com.techpulse.wallet.entity.*;
import com.techpulse.wallet.exception.BadRequestException;
import com.techpulse.wallet.repository.*;
import com.techpulse.wallet.security.JwtService;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final WalletRepository wallets;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthService(UserRepository users, WalletRepository wallets, PasswordEncoder encoder, AuthenticationManager authManager, JwtService jwt) {
        this.users = users;
        this.wallets = wallets;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new BadRequestException("Email is already registered");
        AppUser user = users.save(new AppUser(email, encoder.encode(request.password()), Role.USER));
        wallets.save(new Wallet(user));
    }

    public AuthResponse login(LoginRequest request) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        AppUser user = users.findByEmail(request.email().trim().toLowerCase()).orElseThrow(() -> new BadRequestException("Invalid credentials"));
        return new AuthResponse(jwt.generateToken(user.getEmail(), user.getRole().name()), "Bearer", user.getEmail(), user.getRole().name());
    }
}
