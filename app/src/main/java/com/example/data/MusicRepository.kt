package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MusicRepository(val dao: MusicDao) {

    val allSongs: Flow<List<Song>> = dao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = dao.getFavoriteSongs()
    val recentlyAddedSongs: Flow<List<Song>> = dao.getRecentlyAddedSongs()
    val recentlyPlayedSongs: Flow<List<Song>> = dao.getRecentlyPlayedSongs()
    val mostPlayedSongs: Flow<List<Song>> = dao.getMostPlayedSongs()
    val allPlaylists: Flow<List<Playlist>> = dao.getAllPlaylists()
    val allAudioProfiles: Flow<List<AudioProfile>> = dao.getAllAudioProfiles()
    val allGenres: Flow<List<String>> = dao.getAllGenres()
    val allAlbums: Flow<List<String>> = dao.getAllAlbums()
    val allArtists: Flow<List<String>> = dao.getAllArtists()
    val duplicateSongs: Flow<List<Song>> = dao.findDuplicateSongs()
    val missingLyricsSongs: Flow<List<Song>> = dao.findSongsMissingLyrics()

    suspend fun getSongById(id: Long): Song? = dao.getSongById(id)

    suspend fun insertSong(song: Song): Long = dao.insertSong(song)

    suspend fun updateSong(song: Song) = dao.updateSong(song)

    suspend fun updateFavoriteStatus(songId: Long, isFavorite: Boolean) = 
        dao.updateFavoriteStatus(songId, isFavorite)

    suspend fun deleteSong(songId: Long) = dao.deleteSong(songId)

    suspend fun incrementPlayCount(songId: Long) = dao.incrementPlayCount(songId)

    // Playlists
    suspend fun getPlaylistById(id: Long): Playlist? = dao.getPlaylistById(id)
    suspend fun insertPlaylist(playlist: Playlist): Long = dao.insertPlaylist(playlist)
    suspend fun deletePlaylist(playlist: Playlist) = dao.deletePlaylist(playlist)
    
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        dao.insertPlaylistCrossRef(PlaylistSongCrossRef(playlistId, songId))
    }
    
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = 
        dao.deleteSongFromPlaylist(playlistId, songId)
        
    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> = 
        dao.getSongsForPlaylist(playlistId)

    // Audio Profiles
    suspend fun getDefaultAudioProfile(): AudioProfile? = dao.getDefaultAudioProfile()
    suspend fun insertAudioProfile(profile: AudioProfile): Long = dao.insertAudioProfile(profile)
    suspend fun setDefaultAudioProfile(profileId: Long) {
        dao.clearDefaultAudioProfile()
        dao.setDefaultAudioProfile(profileId)
    }
    suspend fun deleteAudioProfile(profile: AudioProfile) = dao.deleteAudioProfile(profile)

    // History
    suspend fun recordHistory(songId: Long) {
        dao.insertHistoryEntry(PlaybackHistoryEntry(songId = songId))
        dao.incrementPlayCount(songId)
    }

    suspend fun clearHistory() = dao.clearPlaybackHistory()

    // Database seeding
    suspend fun seedDatabase() {
        // Seeding initial high-res cyberpunk songs if database is empty
        val currentSongs = dao.getAllSongs().first()
        if (currentSongs.isEmpty()) {
            val initialSongs = listOf(
                Song(
                    title = "Cyberpunk Odyssey",
                    artist = "Neon Shaper",
                    album = "Grid Explorer",
                    genre = "Synthwave",
                    durationMs = 234000L,
                    filePath = "asset:///music/cyberpunk_odyssey.flac",
                    isFavorite = true,
                    isHighRes = true,
                    highResFormat = "FLAC 96kHz / 24-bit",
                    mood = "Energetic",
                    bpm = 110,
                    fileSizeMb = 48.2,
                    lyrics = """
                        [00:00.00] (Synthwaves rising)
                        [00:15.00] Run through the neon grid
                        [00:22.00] Shadow dancers in the rain
                        [00:30.00] Digital minds with silicon hearts
                        [00:37.00] Running from the motherboard's pain
                        [00:45.00] (Retro-futuristic bass drop)
                        [01:05.00] We are the anomaly, the ghost in the wire
                        [01:12.00] Sparks flying up from synthetic fire
                        [01:20.00] Escaping the sector, crossing the line
                        [01:28.00] Outside of code, of space, and of time
                        [01:40.00] (Complex synthesizer solo)
                        [02:10.00] Into the dark of the system drive
                        [02:18.00] Out in the sparks we are still alive!
                        [02:28.00] (Grid fade out)
                    """.trimIndent()
                ),
                Song(
                    title = "Hyperdrive",
                    artist = "Vector Patrol",
                    album = "Speed of Light",
                    genre = "Electronic",
                    durationMs = 185000L,
                    filePath = "asset:///music/hyperdrive.wav",
                    isFavorite = false,
                    isHighRes = true,
                    highResFormat = "WAV 192kHz / 24-bit",
                    mood = "Uplifting",
                    bpm = 135,
                    fileSizeMb = 64.1,
                    lyrics = """
                        [00:00.00] (Accelerating engine sounds)
                        [00:10.00] Speed in my veins, screen in my eye
                        [00:17.00] Gridlines bending, watch them fly
                        [00:25.00] Shift into maximum warp, let it go
                        [00:32.00] Faster than frequencies, moving solo!
                        [00:40.00] Hyperdrive, take me over the curve
                        [00:48.00] High-octane circuits matching my nerve
                        [01:02.00] (Thrashing drums and heavy synth lines)
                        [01:20.00] Breaking through sound barriers tonight
                        [01:27.00] Cosmic dust glowing in retro blue light
                        [01:34.00] Shift the gears, turn up the gain
                        [01:42.00] Overclocking our systems to break free of pain!
                        [01:55.00] (Intense modular visual synthesizer outro)
                    """.trimIndent()
                ),
                Song(
                    title = "Infinite Horizon",
                    artist = "Solar Sail",
                    album = "Cosmic Breeze",
                    genre = "Ambient Space",
                    durationMs = 312000L,
                    filePath = "asset:///music/infinite_horizon.aac",
                    isFavorite = true,
                    isHighRes = false,
                    highResFormat = "AAC 320kbps",
                    mood = "Relaxed",
                    bpm = 85,
                    fileSizeMb = 7.2,
                    lyrics = """
                        [00:00.00] (Ambient space texture pads)
                        [00:35.00] Drifting along the Solar wind
                        [00:58.00] No markers, no lanes, no limits
                        [01:22.00] Stardust reflecting off chromium sails
                        [01:45.00] Floating far away from earthly tales
                        [02:10.00] Infinite Horizon, where silence meets the light
                        [02:35.00] Beautiful nebula, glowing in the night
                        [03:00.00] Under the stars, drifting out of sight...
                    """.trimIndent()
                ),
                Song(
                    title = "Retro Dreams",
                    artist = "Glitch Pixel",
                    album = "Arcade Memories",
                    genre = "8-Bit Chiptune",
                    durationMs = 160000L,
                    filePath = "asset:///music/retro_dreams.mp3",
                    isFavorite = false,
                    isHighRes = false,
                    highResFormat = "MP3 320kbps",
                    mood = "Uplifting",
                    bpm = 120,
                    fileSizeMb = 5.4,
                    lyrics = """
                        [00:00.00] (Nostalgic 8-bit intro sound)
                        [00:08.00] Blinking arcade lights in the corner room
                        [00:15.00] Tokens in hand, dispelling the gloom
                        [00:22.00] Choose your avatar, press start player one
                        [00:29.00] Retro dreams have only just begun!
                        [00:36.00] (Classic chiptune arpeggio)
                        [00:52.00] Pixelated stages, leaping through the sky
                        [00:59.00] High scores counting up, feeling so high
                        [01:06.00] Double jump over the digital pits
                        [01:13.00] Beating the boss with our virtual wits!
                        [01:25.00] (Exciting game music loop)
                        [01:50.00] Game Over? Never, insert coin to replay!
                    """.trimIndent()
                ),
                Song(
                    title = "Liquid Moonlight",
                    artist = "Satin Waves",
                    album = "Late Night Coffee",
                    genre = "Lofi Chill",
                    durationMs = 210000L,
                    filePath = "asset:///music/liquid_moonlight.flac",
                    isFavorite = true,
                    isHighRes = false,
                    highResFormat = "FLAC 44.1kHz / 16-bit",
                    mood = "Relaxed",
                    bpm = 78,
                    fileSizeMb = 22.0,
                    lyrics = """
                        [00:00.00] (Soft vinyl crackle and electric piano chords)
                        [00:12.00] Raindrops sliding down the window pane
                        [00:25.00] Dim light humming, soothing the brain
                        [00:38.00] Warm coffee steam in a cozy quiet room
                        [00:51.00] Sitting and watching the midnight flower bloom
                        [01:04.00] Liquid Moonlight, sweeping through the floor
                        [01:17.00] Peace is here, I don't ask for more
                        [01:30.00] (Muffled saxophone improvisation)
                        [01:55.00] Soft drums ticking, counting down the end
                        [02:05.00] A peaceful late night, my quiet old friend...
                    """.trimIndent()
                ),
                Song(
                    title = "Omega Protocol",
                    artist = "Dark Sector",
                    album = "Command Line",
                    genre = "Industrial",
                    durationMs = 245000L,
                    filePath = "asset:///music/omega_protocol.opus",
                    isFavorite = false,
                    isHighRes = true,
                    highResFormat = "OPUS 160kbps (Hi-Cap)",
                    mood = "Melancholic",
                    bpm = 125,
                    fileSizeMb = 4.8,
                    lyrics = """
                        [00:00.00] (Heavy metal rhythmic hum and mechanical gear sounds)
                        [00:15.00] Command prompt flashing white on dark green
                        [00:23.00] Codes sliding fast, the ghost in the machine
                        [00:31.00] Initiating Protocol Omega tonight
                        [00:39.00] All servers shutting down, losing the light
                        [00:48.00] (Industrial beat drops with distorted guitar feedback)
                        [01:12.00] Error four-zero-four, path is not found
                        [01:20.00] System is collapsing, falling to the ground
                        [01:28.00] Backups corrupted, connections are dead
                        [01:36.00] Only the static is repeating in my head...
                        [01:50.00] (Glitch sequence noise)
                    """.trimIndent()
                ),
                Song(
                    title = "Solar Wind",
                    artist = "Solar Sail",
                    album = "Cosmic Breeze",
                    genre = "Ambient Space",
                    durationMs = 220000L,
                    filePath = "asset:///music/solar_wind.flac",
                    isFavorite = false,
                    isHighRes = true,
                    highResFormat = "FLAC 96kHz / 24-bit",
                    mood = "Relaxed",
                    bpm = 80,
                    fileSizeMb = 38.5,
                    lyrics = """
                        [00:00.00] (Soft sound of celestial wind and metallic bells)
                        [00:20.00] Riding waves of radiation, sailing through the void
                        [00:45.00] Passing by a silent and forgotten asteroid
                        [01:10.00] Charged particles pushing are light years ahead
                        [01:35.00] Where no human footstep will ever tread
                        [02:00.00] Solar Wind, blow us safe into the deep...
                    """.trimIndent()
                )
            )
            dao.insertSongs(initialSongs)
        }

        val currentProfiles = dao.getAllAudioProfiles().first()
        if (currentProfiles.isEmpty()) {
            val defaultProfiles = listOf(
                AudioProfile(
                    name = "TBM Studio Master (Default)",
                    bands = "0.0,0.5,1.2,1.8,2.0,1.5,1.0,0.5,0.0,-0.5,-1.0,-1.5,-1.2,-0.8,0.0,0.8,1.5,2.0,2.5,2.2,1.8,1.2,0.5,0.0,-0.5,-1.0,-0.8,-0.2,0.5,1.0,1.5,0.0",
                    bassBoost = 15,
                    virtualizer = 10,
                    surroundSound = 5,
                    reverbPreset = "Studio",
                    sound3D = true,
                    sampleRateOptimized = true,
                    isDefault = true
                ),
                AudioProfile(
                    name = "Hyper Bass Booster",
                    bands = "12.0,11.5,10.0,8.5,7.0,5.0,3.0,1.5,0.0,-1.5,-3.0,-4.5,-5.0,-4.5,-3.0,-1.5,0.0,1.5,3.0,4.5,5.0,4.5,3.0,1.5,0.0,-1.5,-3.0,-4.5,-5.0,0.0,0.0,0.0",
                    bassBoost = 85,
                    virtualizer = 35,
                    surroundSound = 20,
                    reverbPreset = "Club",
                    sound3D = true,
                    sampleRateOptimized = true,
                    isDefault = false
                ),
                AudioProfile(
                    name = "Cosmic Hall 3D",
                    bands = "-2.0,-1.5,-1.0,0.0,0.5,1.0,1.5,2.0,2.5,3.0,3.5,4.0,4.5,5.0,5.5,5.0,4.5,4.0,3.5,3.0,2.5,2.0,1.5,1.0,0.5,0.0,-0.5,-1.0,-1.5,-2.0,-2.5,-3.0",
                    bassBoost = 35,
                    virtualizer = 80,
                    surroundSound = 90,
                    reverbPreset = "Hall",
                    sound3D = true,
                    sampleRateOptimized = true,
                    isDefault = false
                ),
                AudioProfile(
                    name = "Muted Vocal Clarity",
                    bands = "-6.0,-5.0,-4.0,-3.0,-1.5,0.0,1.5,3.0,4.0,5.0,6.0,8.0,9.0,8.0,6.0,4.0,2.0,0.0,-1.5,-3.0,-4.0,-4.5,-5.0,-5.5,-6.0,-6.0,-5.0,-4.0,-3.0,-2.0,-1.0,0.0",
                    bassBoost = 5,
                    virtualizer = 15,
                    surroundSound = 10,
                    reverbPreset = "Studio",
                    sound3D = false,
                    sampleRateOptimized = true,
                    isDefault = false
                )
            )
            for (profile in defaultProfiles) {
                dao.insertAudioProfile(profile)
            }
        }

        val currentPlaylists = dao.getAllPlaylists().first()
        if (currentPlaylists.isEmpty()) {
            val allSongsList = dao.getAllSongs().first()
            if (allSongsList.isNotEmpty()) {
                val p1Id = dao.insertPlaylist(Playlist(name = "Cyberpunk Outrun Mix", description = "High-energy tracks for virtual runs."))
                val p2Id = dao.insertPlaylist(Playlist(name = "Midnight Chillout", description = "Lofi and ambient space tunes to unwind."))

                // Add Cyberpunk Odyssey & Hyperdrive to Playlist 1
                val cpSong = allSongsList.find { it.title == "Cyberpunk Odyssey" }
                val hdSong = allSongsList.find { it.title == "Hyperdrive" }
                val opSong = allSongsList.find { it.title == "Omega Protocol" }
                if (cpSong != null) dao.insertPlaylistCrossRef(PlaylistSongCrossRef(p1Id, cpSong.id))
                if (hdSong != null) dao.insertPlaylistCrossRef(PlaylistSongCrossRef(p1Id, hdSong.id))
                if (opSong != null) dao.insertPlaylistCrossRef(PlaylistSongCrossRef(p1Id, opSong.id))

                // Add Infinite Horizon & Liquid Moonlight to Playlist 2
                val ihSong = allSongsList.find { it.title == "Infinite Horizon" }
                val lmSong = allSongsList.find { it.title == "Liquid Moonlight" }
                val swSong = allSongsList.find { it.title == "Solar Wind" }
                if (ihSong != null) dao.insertPlaylistCrossRef(PlaylistSongCrossRef(p2Id, ihSong.id))
                if (lmSong != null) dao.insertPlaylistCrossRef(PlaylistSongCrossRef(p2Id, lmSong.id))
                if (swSong != null) dao.insertPlaylistCrossRef(PlaylistSongCrossRef(p2Id, swSong.id))
            }
        }
    }
}
