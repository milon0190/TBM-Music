package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiManager
import com.example.audio.AudioPlayerManager
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MusicDatabase.getDatabase(application)
    private val dao = db.musicDao()
    val repository = MusicRepository(dao)

    // Main Audio Service state
    val playerManager = AudioPlayerManager(application, repository, viewModelScope)

    // Flow UI properties from Repository
    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAddedSongs: StateFlow<List<Song>> = repository.recentlyAddedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedSongs: StateFlow<List<Song>> = repository.recentlyPlayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedSongs: StateFlow<List<Song>> = repository.mostPlayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAudioProfiles: StateFlow<List<AudioProfile>> = repository.allAudioProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGenres: StateFlow<List<String>> = repository.allGenres
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlbums: StateFlow<List<String>> = repository.allAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allArtists: StateFlow<List<String>> = repository.allArtists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val duplicateSongs: StateFlow<List<Song>> = repository.duplicateSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missingLyricsSongs: StateFlow<List<Song>> = repository.missingLyricsSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live navigation & selected elements properties
    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    val currentPlaylistSongs: StateFlow<List<Song>> = _selectedPlaylistId
        .flatMapLatest { playlistId ->
            if (playlistId != null) repository.getSongsForPlaylist(playlistId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylistId
        .flatMapLatest { id ->
            if (id != null) {
                flow {
                    emit(repository.getPlaylistById(id))
                }
            } else flowOf<Playlist?>(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Smart Tag Editing Dialog
    private val _songToEdit = MutableStateFlow<Song?>(null)
    val songToEdit: StateFlow<Song?> = _songToEdit.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchedSongs: StateFlow<List<Song>> = combine(allSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) songs
        else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
            }
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI smart feature states
    private val _aiRecommendationQuery = MutableStateFlow("")
    val aiRecommendationQuery: StateFlow<String> = _aiRecommendationQuery.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiRecommendationNotes = MutableStateFlow("")
    val aiRecommendationNotes: StateFlow<String> = _aiRecommendationNotes.asStateFlow()

    private val _aiRecommendedSongIds = MutableStateFlow<List<Long>>(emptyList())
    val aiRecommendedSongIds: StateFlow<List<Long>> = _aiRecommendedSongIds.asStateFlow()

    private val _aiSongAnalysis = MutableStateFlow<Map<String, String>?>(null)
    val aiSongAnalysis: StateFlow<Map<String, String>?> = _aiSongAnalysis.asStateFlow()

    // Scanner state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        // Seeding database
        viewModelScope.launch {
            repository.seedDatabase()
        }
    }

    enum class Screen { DASHBOARD, LIBRARY, SOUND_EFFECTS, NOW_PLAYING, SMART_ASSISTANT }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectPlaylist(id: Long?) {
        _selectedPlaylistId.value = id
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateAiRecommendationQuery(query: String) {
        _aiRecommendationQuery.value = query
    }

    fun startAIAssistantPlay() {
        viewModelScope.launch {
            val recIds = _aiRecommendedSongIds.value
            val matchingSongs = allSongs.value.filter { recIds.contains(it.id) }
            if (matchingSongs.isNotEmpty()) {
                playerManager.setQueue(matchingSongs, 0)
                _currentScreen.value = Screen.NOW_PLAYING
            }
        }
    }

    // AI Features
    fun generateAIPlaylist() {
        if (_aiRecommendationQuery.value.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiRecommendationNotes.value = ""
            _aiRecommendedSongIds.value = emptyList()

            try {
                val songs = allSongs.value
                val (ids, notes) = GeminiManager.getSmartRecommendations(_aiRecommendationQuery.value, songs)
                _aiRecommendedSongIds.value = ids
                _aiRecommendationNotes.value = notes
            } catch (e: Exception) {
                _aiRecommendationNotes.value = "Error: Concurrency exception triggered by AI Engine ${e.localizedMessage}."
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun analyzeSongAI(song: Song) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val json = GeminiManager.getSongAIAssistedAnalysis(song.title, song.artist)
                val map = mapOf(
                    "mood" to json.optString("mood", "Aesthetic Cyberwave"),
                    "genre" to json.optString("genre", "Industrial Outrun"),
                    "bpm" to json.optInt("bpm", 124).toString(),
                    "description" to json.optString("description", "A highly precise visual mood tag calculated by our smart tag algorithm.")
                )
                _aiSongAnalysis.value = map
            } catch (e: Exception) {
                _aiSongAnalysis.value = mapOf(
                    "error" to "Failed to reach AI Core. Offline rules active."
                )
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearAISongAnalysis() {
        _aiSongAnalysis.value = null
    }

    // Media Scanning
    fun scanDeviceMediaCatalog() {
        viewModelScope.launch {
            _isScanning.value = true
            // Simulate advanced lightning-fast 120fps background directory & content scanning
            delay(1500)

            // Inject three newly discovered premium local audio tracks as part of file mapping
            val newlyDiscoveredSongs = listOf(
                Song(
                    title = "Neon Drift",
                    artist = "Hyperdrive Cadet",
                    album = "Outrun the Clock",
                    genre = "Synthwave",
                    durationMs = 195000L,
                    filePath = "/storage/emulated/0/Music/neon_drift.wav",
                    isHighRes = true,
                    highResFormat = "WAV 96kHz / 24-bit",
                    mood = "Energetic",
                    bpm = 124,
                    fileSizeMb = 53.4,
                    lyrics = "[00:00.00] (Neon flashing on wet windshields) [00:15.00] Sideways drifting into outer grids!"
                ),
                Song(
                    title = "Ethereal Whispers",
                    artist = "Aura Synthetica",
                    album = "Nebula Whispers",
                    genre = "Ambient Space",
                    durationMs = 280000L,
                    filePath = "/storage/emulated/0/Music/ethereal_whispers.ogg",
                    isHighRes = false,
                    highResFormat = "OGG 44.1kHz (VBR)",
                    mood = "Relaxed",
                    bpm = 70,
                    fileSizeMb = 6.8,
                    lyrics = "[00:00.00] (Echoing space bells and soft wind pads) [00:50.00] Floating inside the nebula drift..."
                ),
                Song(
                    title = "Midnight Glitch",
                    artist = "Neon Shaper",
                    album = "Grid Explorer",
                    genre = "Synthwave",
                    durationMs = 150000L,
                    filePath = "/storage/emulated/0/Music/midnight_glitch.mp3",
                    isHighRes = false,
                    highResFormat = "MP3 320kbps",
                    mood = "Energetic",
                    bpm = 112,
                    fileSizeMb = 4.8,
                    lyrics = "[00:00.00] (Gritty modular glitch synth riff) [00:12.00] Lost in code, a glitch in the dark room."
                )
            )

            // Filter out existing ones
            val currentTracks = allSongs.value
            val songsToInsert = newlyDiscoveredSongs.filter { dis ->
                currentTracks.none { it.title == dis.title && it.artist == dis.artist }
            }

            if (songsToInsert.isNotEmpty()) {
                repository.dao.insertSongs(songsToInsert)
            }
            _isScanning.value = false
        }
    }

    // Music control actions from database
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(song.id, !song.isFavorite)
        }
    }

    fun playSongDirect(song: Song, contextSongsList: List<Song>) {
        val index = contextSongsList.indexOfFirst { it.id == song.id }
        if (index != -1) {
            playerManager.setQueue(contextSongsList, index)
        } else {
            playerManager.setQueue(listOf(song), 0)
        }
        _currentScreen.value = Screen.NOW_PLAYING
    }

    // Playlist CRUD
    fun createPlaylist(name: String, description: String) {
        viewModelScope.launch {
            repository.insertPlaylist(Playlist(name = name, description = description))
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            if (_selectedPlaylistId.value == playlist.id) {
                _selectedPlaylistId.value = null
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    // Equalizer Profiles CRUD
    fun createAudioProfile(name: String, bandsList: FloatArray, bassBoost: Int, virt: Int, surround: Int, reverb: String, sound3D: Boolean) {
        viewModelScope.launch {
            val bandsStr = bandsList.joinToString(",")
            val profile = AudioProfile(
                name = name,
                bands = bandsStr,
                bassBoost = bassBoost,
                virtualizer = virt,
                surroundSound = surround,
                reverbPreset = reverb,
                sound3D = sound3D,
                isDefault = false
            )
            val insertedId = repository.insertAudioProfile(profile)
            applyAudioProfileDirectly(profile.copy(id = insertedId))
        }
    }

    fun selectAudioProfile(profile: AudioProfile) {
        viewModelScope.launch {
            repository.setDefaultAudioProfile(profile.id)
            applyAudioProfileDirectly(profile)
        }
    }

    fun deleteAudioProfile(profile: AudioProfile) {
        viewModelScope.launch {
            repository.deleteAudioProfile(profile)
        }
    }

    private fun applyAudioProfileDirectly(profile: AudioProfile) {
        val floatBands = profile.bands.split(",").map { it.toFloatOrNull() ?: 0f }.toFloatArray()
        playerManager.setEqualizerBands(floatBands)
        playerManager.setBassBoost(profile.bassBoost)
        playerManager.setVirtualizer(profile.virtualizer)
        playerManager.setSurroundSound(profile.surroundSound)
        playerManager.setReverbPreset(profile.reverbPreset)
        playerManager.setSound3D(profile.sound3D)
    }

    // Tag editing actions
    fun openTagEditorFor(song: Song) {
        _songToEdit.value = song
    }

    fun saveSongTags(songId: Long, updatedTitle: String, updatedArtist: String, updatedAlbum: String, updatedGenre: String, updatedLyrics: String) {
        viewModelScope.launch {
            val song = repository.getSongById(songId)
            if (song != null) {
                val updated = song.copy(
                    title = updatedTitle,
                    artist = updatedArtist,
                    album = updatedAlbum,
                    genre = updatedGenre,
                    lyrics = updatedLyrics
                )
                repository.updateSong(updated)
                // If currently playing, update details
                if (playerManager.currentSong.value?.id == songId) {
                    playerManager.playSong(updated)
                }
            }
            _songToEdit.value = null
        }
    }

    fun cancelTagEditing() {
        _songToEdit.value = null
    }

    fun deleteSongFromLibrary(songId: Long) {
        viewModelScope.launch {
            repository.deleteSong(songId)
            // If deleted song is currently playing, jump off
            if (playerManager.currentSong.value?.id == songId) {
                playerManager.nextTrack()
            }
        }
    }
}
