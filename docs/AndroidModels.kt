// ─────────────────────────────────────────────────────────────────────────────
// Android / Kotlin Data Models for Music Player APK
// Place these in: app/src/main/java/com/musicplayer/data/model/
// ─────────────────────────────────────────────────────────────────────────────

package com.musicplayer.data.model

import com.google.gson.annotations.SerializedName

// ── Generic API envelope ──────────────────────────────────────────────────────

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val error: String?,
    val timestamp: Long
)

// ── Song ──────────────────────────────────────────────────────────────────────

data class Song(
    val id: String,
    val name: String,
    val duration: Int,               // seconds
    val year: String?,
    val language: String?,
    val hasLyrics: Boolean,
    val playCount: Int?,
    val label: String?,
    val image: List<ImageQuality>,
    val downloadUrl: List<DownloadUrl>,
    val artists: Artists,
    val album: AlbumRef?
)

data class ImageQuality(
    val quality: String,             // "50x50", "150x150", "500x500"
    val url: String
) {
    /** Returns the highest quality URL from a list */
    companion object {
        fun List<ImageQuality>.highest(): String =
            lastOrNull()?.url ?: ""
    }
}

data class DownloadUrl(
    val quality: String,             // "96kbps", "160kbps", "320kbps"
    val url: String
) {
    companion object {
        fun List<DownloadUrl>.best(): String =
            lastOrNull()?.url ?: ""

        fun List<DownloadUrl>.forQuality(q: String): String =
            find { it.quality == q }?.url ?: lastOrNull()?.url ?: ""
    }
}

data class Artists(
    val primary: List<ArtistRef>,
    val featured: List<ArtistRef>,
    val all: List<ArtistRef>
) {
    fun primaryNames(): String =
        primary.joinToString(", ") { it.name }
}

data class ArtistRef(
    val id: String,
    val name: String,
    val image: List<ImageQuality>?,
    val role: String?
)

data class AlbumRef(
    val id: String?,
    val name: String?
)

// ── Album ─────────────────────────────────────────────────────────────────────

data class Album(
    val id: String,
    val name: String,
    val description: String?,
    val year: String?,
    val language: String?,
    val playCount: Int?,
    val image: List<ImageQuality>,
    val artists: Artists?,
    val songs: List<Song>?
)

// ── Artist ────────────────────────────────────────────────────────────────────

data class Artist(
    val id: String,
    val name: String,
    val image: List<ImageQuality>,
    val followerCount: Int?,
    val fanCount: Int?,
    val isVerified: Boolean?,
    val dominantLanguage: String?,
    val dominantType: String?,
    val bio: List<ArtistBio>?,
    val similarArtists: List<ArtistRef>?,
    val topSongs: List<Song>?,
    val topAlbums: List<Album>?
)

data class ArtistBio(
    val title: String,
    val text: String,
    val sequence: Int
)

// ── Playlist ──────────────────────────────────────────────────────────────────

data class Playlist(
    val id: String,
    val name: String,
    val description: String?,
    val image: List<ImageQuality>,
    val songCount: Int?,
    val songs: List<Song>?
)

// ── Search Results ────────────────────────────────────────────────────────────

data class SearchResult(
    val songs: SongSearchData?,
    val albums: AlbumSearchData?,
    val artists: ArtistSearchData?,
    val playlists: PlaylistSearchData?
)

data class SongSearchData(
    val total: Int,
    val start: Int,
    val results: List<Song>
)

data class AlbumSearchData(
    val total: Int,
    val start: Int,
    val results: List<Album>
)

data class ArtistSearchData(
    val total: Int,
    val start: Int,
    val results: List<Artist>
)

data class PlaylistSearchData(
    val total: Int,
    val start: Int,
    val results: List<Playlist>
)
