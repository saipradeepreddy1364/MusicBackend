# Music Player API Reference

Base URL: `http://localhost:8080/api`

All responses follow this envelope:
```json
{
  "success": true,
  "data": { ... },
  "message": "optional",
  "error": "only on failure",
  "timestamp": 1712345678901
}
```

---

## 🔍 Search

### Global Search
```
GET /search?q={query}&page=1&limit=20
```
Returns songs, albums, artists, playlists all in one response.

### Search Songs
```
GET /search/songs?q={query}&page=1&limit=20
```

### Search Albums
```
GET /search/albums?q={query}&page=1&limit=10
```

### Search Artists
```
GET /search/artists?q={query}&page=1&limit=10
```

### Search Playlists
```
GET /search/playlists?q={query}&page=1&limit=10
```

---

## 🎵 Songs

### Get Song by ID
```
GET /songs/{id}
```
Returns full details: name, artists, album, duration, download URLs (96kbps, 160kbps, 320kbps), artwork images.

**Example response:**
```json
{
  "success": true,
  "data": {
    "id": "5WXAlMNt",
    "name": "Kesariya",
    "duration": 269,
    "year": "2022",
    "downloadUrl": [
      { "quality": "96kbps",  "url": "https://..." },
      { "quality": "160kbps", "url": "https://..." },
      { "quality": "320kbps", "url": "https://..." }
    ],
    "image": [
      { "quality": "50x50",   "url": "https://..." },
      { "quality": "150x150", "url": "https://..." },
      { "quality": "500x500", "url": "https://..." }
    ],
    "artists": { "primary": [...], "featured": [...] },
    "album": { "id": "...", "name": "Brahmastra" }
  }
}
```

### Get Song Suggestions
```
GET /songs/{id}/suggestions?limit=10
```

---

## 💿 Albums

### Get Album by ID
```
GET /albums?id={albumId}
```

### Get Album by Link
```
GET /albums?link=https://www.jiosaavn.com/album/brahmastra/...
```

---

## 🎤 Artists

### Get Artist Profile
```
GET /artists/{id}
```

### Get Artist's Songs
```
GET /artists/{id}/songs?page=1&sortBy=popularity&sortOrder=desc
```
`sortBy` options: `popularity`, `latest`, `alphabetical`

### Get Artist's Albums
```
GET /artists/{id}/albums?page=1
```

---

## 📋 Playlists

### Get Playlist by ID
```
GET /playlists?id={playlistId}&page=1&limit=20
```

### Get Playlist by Link
```
GET /playlists?link=https://www.jiosaavn.com/featured/...&page=1&limit=20
```

---

## 📈 Charts

### Get Trending Charts
```
GET /charts
```

---

## 🏥 Health

### Backend Health
```
GET /health
```

### JioSaavn Proxy Check
```
GET /health/jiosaavn
```

---

## 📱 Android Integration Example (Retrofit)

```kotlin
// ApiService.kt
interface MusicApiService {

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<SongsSearchResult>

    @GET("songs/{id}")
    suspend fun getSong(@Path("id") id: String): ApiResponse<Song>

    @GET("songs/{id}/suggestions")
    suspend fun getSuggestions(
        @Path("id") id: String,
        @Query("limit") limit: Int = 10
    ): ApiResponse<List<Song>>

    @GET("charts")
    suspend fun getCharts(): ApiResponse<Any>
}

// Retrofit setup
val retrofit = Retrofit.Builder()
    .baseUrl("http://YOUR_SERVER_IP:8080/api/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val api = retrofit.create(MusicApiService::class.java)
```

---

## ⚠️ Notes

- The JioSaavn API is **unofficial** — use responsibly and respect rate limits.
- Download URLs from JioSaavn are **time-limited** (expire after ~1 hour).
- The backend caches responses for **5 minutes** to reduce load on the proxy.
- For production, add authentication (JWT) to protect your endpoints.
