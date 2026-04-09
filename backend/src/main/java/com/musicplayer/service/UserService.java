package com.musicplayer.service;

import com.musicplayer.model.LikedSong;
import com.musicplayer.model.Session;
import com.musicplayer.model.User;
import com.musicplayer.repository.LikedSongRepository;
import com.musicplayer.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final SessionRepository    sessionRepository;
    private final LikedSongRepository  likedSongRepository;

    public UserService(SessionRepository sessionRepository,
                       LikedSongRepository likedSongRepository) {
        this.sessionRepository   = sessionRepository;
        this.likedSongRepository = likedSongRepository;
    }

    // ── Resolve user from Bearer token ────────────────────────────────────────

    /**
     * Looks up the session by token and returns the owning User,
     * or empty if the token is missing / expired / not found.
     */
    public Optional<User> getUserFromToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return sessionRepository.findByToken(token)
                .filter(s -> !s.isExpired())
                .map(Session::getUser);
    }

    // ── Liked songs ───────────────────────────────────────────────────────────

    /** Returns all liked songs for the authenticated user. */
    public Map<String, Object> getLikedSongs(String token) {
        Optional<User> userOpt = getUserFromToken(token);
        if (userOpt.isEmpty()) return unauthorized();

        List<Map<String, Object>> songs = likedSongRepository
                .findByUserOrderByLikedAtDesc(userOpt.get())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return Map.of("success", true, "data", songs);
    }

    /** Likes a song for the authenticated user. Idempotent — safe to call twice. */
    @Transactional
    public Map<String, Object> likeSong(String token, String songId,
                                        String songTitle, String songImage) {
        Optional<User> userOpt = getUserFromToken(token);
        if (userOpt.isEmpty()) return unauthorized();
        if (songId == null || songId.isBlank()) return error("songId is required");

        User user = userOpt.get();

        // Idempotent — don't error if already liked
        if (likedSongRepository.existsByUserAndSongId(user, songId)) {
            return Map.of("success", true, "message", "Already liked");
        }

        LikedSong like = new LikedSong();
        like.setUser(user);
        like.setSongId(songId);
        like.setSongTitle(songTitle);
        like.setSongImage(songImage);
        likedSongRepository.save(like);

        log.info("likeSong | userId={} songId={}", user.getId(), songId);
        return Map.of("success", true, "message", "Song liked");
    }

    /** Removes a like for the authenticated user. */
    @Transactional
    public Map<String, Object> unlikeSong(String token, String songId) {
        Optional<User> userOpt = getUserFromToken(token);
        if (userOpt.isEmpty()) return unauthorized();
        if (songId == null || songId.isBlank()) return error("songId is required");

        User user = userOpt.get();
        likedSongRepository.deleteByUserAndSongId(user, songId);

        log.info("unlikeSong | userId={} songId={}", user.getId(), songId);
        return Map.of("success", true, "message", "Song unliked");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> toDto(LikedSong like) {
        return Map.of(
            "songId",     like.getSongId(),
            "songTitle",  like.getSongTitle()  != null ? like.getSongTitle()  : "",
            "songImage",  like.getSongImage()  != null ? like.getSongImage()  : "",
            "likedAt",    like.getLikedAt().toString()
        );
    }

    private Map<String, Object> unauthorized() {
        return Map.of("success", false, "message", "Unauthorized");
    }

    private Map<String, Object> error(String message) {
        return Map.of("success", false, "message", message);
    }
}