package com.sprint.auth.service;


import com.sprint.auth.dto.request.*;
import com.sprint.auth.dto.response.AuthResponse;
import com.sprint.auth.exception.ResourceNotFoundException;
import com.sprint.auth.exception.UserAlreadyExistException;
import com.sprint.auth.model.RefreshToken;
import com.sprint.auth.model.Role;
import com.sprint.auth.model.User;
import com.sprint.auth.repository.RefreshTokenRepository;
import com.sprint.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    public AuthService(UserRepository userRepository, JwtService jwtService, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (this.userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistException("Client with name: " + request.username() + " already exists");
        }

        if (request.email() != null && userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistException("User with email: " + request.email() + " already exists");
        }

        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                Role.valueOf(request.role().toUpperCase())
        );
        user.setEmail(request.email());

//        user.setUsername(request.username());
//
//        user.setPassword(passwordEncoder.encode(request.password()));
//        user.setRole(Role.valueOf(request.role().toUpperCase()));

        User savedUser = this.userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {

        Authentication authenticate = this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        User user = (User) authenticate.getPrincipal();

        refreshTokenRepository.deleteAllByUserId(user.getId());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse processOAuth2PostLogin(String email, String githubLogin) {
        Optional<User> optionalUser = this.userRepository.findByUsername(githubLogin);

        if (optionalUser.isEmpty()) {
            optionalUser = this.userRepository.findByEmail(email);
        }

        User user = optionalUser.orElseGet(() -> {
            User newUser = new User(
                    githubLogin,
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    Role.GUARD
            );
            newUser.setEmail(email);
            return this.userRepository.save(newUser);
        });

        this.refreshTokenRepository.deleteAllByUserId(user.getId());

        return buildAuthResponse(user);
    }

    @Transactional
    public void updateEmail(String username, UpdateEmailRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistException("Email " + request.email() + " is already taken");
        }
        //dirty checking
        user.setEmail(request.email());
    }

    @Transactional
    public void updatePassword(String username, UpdatePasswordRequest request) {
        User user = this.userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Wrong current password");
        }

        this.refreshTokenRepository.revokeAllByUserId(user.getId());

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        this.userRepository.save(user);

    }

    @Transactional
    public void updateRole(Long userId, UpdateRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        this.refreshTokenRepository.revokeAllByUserId(userId);
        Role newRole = Role.valueOf(request.role().toUpperCase());
        user.setRole(newRole);
        this.userRepository.save(user);

    }

    @Transactional(noRollbackFor = AccessDeniedException.class)
    public AuthResponse refresh(RefreshRequest refreshRequest) {
        RefreshToken oldRefreshToken = this.refreshTokenRepository.findByToken(refreshRequest.refreshToken())
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        if (oldRefreshToken.isRevoked()) {
            this.refreshTokenRepository.revokeAllByUserId(oldRefreshToken.getUser().getId());
            throw new AccessDeniedException("Security alert: Token reuse detected. All sessions terminated.");
        }

        if (oldRefreshToken.isExpired()) {
            throw new AccessDeniedException("Refresh token is expired. Please sign in again.");
        }

        oldRefreshToken.setRevoked(true);
        this.refreshTokenRepository.save(oldRefreshToken);

        User user = oldRefreshToken.getUser();
        String accessToken = this.jwtService.generateToken(user);

        RefreshToken newRefreshToken = new RefreshToken(
                UUID.randomUUID().toString(),
                user,
                oldRefreshToken.getExpiresAt()
        );
        this.refreshTokenRepository.save(newRefreshToken);

        return new AuthResponse(accessToken, newRefreshToken.getToken());

    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional
    public void deleteUser(Long userId, User currentUser) {
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (userToDelete.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        refreshTokenRepository.deleteAllByUserId(userId);

        userRepository.delete(userToDelete);
    }


    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);

        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID().toString(),
                user,
                LocalDateTime.now().plusDays(refreshExpirationDays)
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }
}
