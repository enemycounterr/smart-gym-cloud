package com.sprint.auth.service;


import com.sprint.auth.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);

    public TokenCleanupService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanExpiredRefreshTokens() {
        log.info("Started to clear expired Refresh tokens.");

        try {
            refreshTokenRepository.deleteAllExpiredSince(LocalDateTime.now());
            log.info("Expired refresh tokens cleanup completed successfully.");
        } catch (Exception e) {
            log.error("Error during refresh tokens cleanup: {}", e.getMessage());
        }
    }
}
