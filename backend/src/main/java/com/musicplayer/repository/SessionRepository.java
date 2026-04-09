package com.musicplayer.repository;

import com.musicplayer.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByToken(String token);

    @Modifying
    @Transactional
    void deleteByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now")
    void deleteExpiredSessions(Instant now);
}