package com.musicplayer.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores a song that a specific user has liked.
 * songId is the external JioSaavn song ID (string).
 * The unique constraint on (user, songId) prevents duplicate likes.
 */
@Entity
@Table(
    name = "liked_songs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "song_id"})
)
public class LikedSong {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "song_id", nullable = false, length = 255)
    private String songId;

    @Column(name = "song_title", length = 500)
    private String songTitle;

    @Column(name = "song_image", length = 1000)
    private String songImage;

    @Column(name = "liked_at", updatable = false)
    private Instant likedAt = Instant.now();

    // ── Getters & Setters ─────────────────────────────────────────────────

    public UUID getId()                        { return id; }
    public void setId(UUID id)                 { this.id = id; }

    public User getUser()                      { return user; }
    public void setUser(User user)             { this.user = user; }

    public String getSongId()                  { return songId; }
    public void setSongId(String songId)       { this.songId = songId; }

    public String getSongTitle()               { return songTitle; }
    public void setSongTitle(String songTitle) { this.songTitle = songTitle; }

    public String getSongImage()               { return songImage; }
    public void setSongImage(String songImage) { this.songImage = songImage; }

    public Instant getLikedAt()                { return likedAt; }
    public void setLikedAt(Instant likedAt)    { this.likedAt = likedAt; }
}