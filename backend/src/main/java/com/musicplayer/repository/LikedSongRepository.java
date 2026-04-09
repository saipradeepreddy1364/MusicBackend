package com.musicplayer.repository;

import com.musicplayer.model.LikedSong;
import com.musicplayer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikedSongRepository extends JpaRepository<LikedSong, UUID> {

    /** All liked songs for a user, newest first */
    List<LikedSong> findByUserOrderByLikedAtDesc(User user);

    /** Check if a user already liked a specific song */
    boolean existsByUserAndSongId(User user, String songId);

    /** Find a specific like so we can delete it */
    Optional<LikedSong> findByUserAndSongId(User user, String songId);

    /** Delete a like by user + songId */
    void deleteByUserAndSongId(User user, String songId);
}