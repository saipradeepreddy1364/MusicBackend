// ─────────────────────────────────────────────────────────────────────────────
// Android Retrofit Setup — Complete Integration Layer
// Place in: app/src/main/java/com/musicplayer/data/
// ─────────────────────────────────────────────────────────────────────────────

package com.musicplayer.data

import com.musicplayer.data.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ── 1. Retrofit Interface ─────────────────────────────────────────────────────

interface MusicApiService {

    // Search
    @GET("search")
    suspend fun searchAll(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<SearchResult>

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<SongSearchData>

    @GET("search/albums")
    suspend fun searchAlbums(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): ApiResponse<AlbumSearchData>

    @GET("search/artists")
    suspend fun searchArtists(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): ApiResponse<ArtistSearchData>

    // Songs
    @GET("songs/{id}")
    suspend fun getSong(@Path("id") id: String): ApiResponse<Song>

    @GET("songs/{id}/suggestions")
    suspend fun getSuggestions(
        @Path("id") id: String,
        @Query("limit") limit: Int = 10
    ): ApiResponse<List<Song>>

    // Albums
    @GET("albums")
    suspend fun getAlbumById(@Query("id") id: String): ApiResponse<Album>

    @GET("albums")
    suspend fun getAlbumByLink(@Query("link") link: String): ApiResponse<Album>

    // Artists
    @GET("artists/{id}")
    suspend fun getArtist(@Path("id") id: String): ApiResponse<Artist>

    @GET("artists/{id}/songs")
    suspend fun getArtistSongs(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("sortBy") sortBy: String = "popularity",
        @Query("sortOrder") sortOrder: String = "desc"
    ): ApiResponse<SongSearchData>

    @GET("artists/{id}/albums")
    suspend fun getArtistAlbums(
        @Path("id") id: String,
        @Query("page") page: Int = 1
    ): ApiResponse<AlbumSearchData>

    // Playlists
    @GET("playlists")
    suspend fun getPlaylistById(
        @Query("id") id: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<Playlist>

    // Charts
    @GET("charts")
    suspend fun getCharts(): ApiResponse<Any>

    // Health
    @GET("health")
    suspend fun ping(): ApiResponse<Any>
}

// ── 2. Retrofit Factory ───────────────────────────────────────────────────────

object RetrofitClient {

    // ⚠️ Change to your server IP or domain when testing on a real device
    private const val BASE_URL = "http://10.0.2.2:8080/api/"   // Android emulator → localhost

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val instance: MusicApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicApiService::class.java)
    }
}

// ── 3. Repository (wraps network calls with Result) ──────────────────────────

sealed class Resource<T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String, val data: T? = null) : Resource<T>()
    class Loading<T> : Resource<T>()
}

class MusicRepository(
    private val api: MusicApiService = RetrofitClient.instance
) {

    suspend fun searchSongs(query: String, page: Int = 1): Resource<SongSearchData> = safeCall {
        val response = api.searchSongs(query, page)
        if (response.success && response.data != null) response.data
        else throw Exception(response.error ?: "Unknown error")
    }

    suspend fun getSong(id: String): Resource<Song> = safeCall {
        val response = api.getSong(id)
        if (response.success && response.data != null) response.data
        else throw Exception(response.error ?: "Unknown error")
    }

    suspend fun getSuggestions(songId: String): Resource<List<Song>> = safeCall {
        val response = api.getSuggestions(songId)
        if (response.success && response.data != null) response.data
        else throw Exception(response.error ?: "Unknown error")
    }

    suspend fun getAlbum(id: String): Resource<Album> = safeCall {
        val response = api.getAlbumById(id)
        if (response.success && response.data != null) response.data
        else throw Exception(response.error ?: "Unknown error")
    }

    suspend fun getArtist(id: String): Resource<Artist> = safeCall {
        val response = api.getArtist(id)
        if (response.success && response.data != null) response.data
        else throw Exception(response.error ?: "Unknown error")
    }

    suspend fun getPlaylist(id: String, page: Int = 1): Resource<Playlist> = safeCall {
        val response = api.getPlaylistById(id, page)
        if (response.success && response.data != null) response.data
        else throw Exception(response.error ?: "Unknown error")
    }

    suspend fun getCharts(): Resource<Any> = safeCall {
        val response = api.getCharts()
        if (response.success && response.data != null) response.data
        else throw Exception(response.error ?: "Unknown error")
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private suspend fun <T> safeCall(call: suspend () -> T): Resource<T> {
        return try {
            Resource.Success(call())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}

// ── 4. ViewModel Example ──────────────────────────────────────────────────────

// In your Fragment/Activity:
//
// class SearchViewModel : ViewModel() {
//     private val repo = MusicRepository()
//
//     private val _songs = MutableStateFlow<Resource<SongSearchData>>(Resource.Loading())
//     val songs: StateFlow<Resource<SongSearchData>> = _songs
//
//     fun search(query: String) {
//         viewModelScope.launch {
//             _songs.value = Resource.Loading()
//             _songs.value = repo.searchSongs(query)
//         }
//     }
// }
//
// // In Fragment:
// lifecycleScope.launch {
//     viewModel.songs.collect { resource ->
//         when (resource) {
//             is Resource.Loading -> showLoader()
//             is Resource.Success -> showSongs(resource.data.results)
//             is Resource.Error   -> showError(resource.message)
//         }
//     }
// }
