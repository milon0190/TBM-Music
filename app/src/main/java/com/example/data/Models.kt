package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationMs: Long,
    val filePath: String,
    val isFavorite: Boolean = false,
    val isHighRes: Boolean = false,
    val highResFormat: String = "44.1kHz / 16-bit", // e.g. "FLAC 96kHz/24-bit", "DSD 11.2MHz"
    val mood: String = "Energetic", // Energetic, Relaxed, Melancholic, Uplifting, Cinematic
    val lyrics: String = "",
    val playCount: Int = 0,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val lastPlayedTimestamp: Long = 0L,
    val bpm: Int = 120,
    val fileSizeMb: Double = 8.5
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isSmart: Boolean = false
)

@Entity(tableName = "playlist_song_cross_ref", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long
)

@Entity(tableName = "audio_profiles")
data class AudioProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bands: String, // Comma separated float values for 32 equalizer bands
    val bassBoost: Int = 0, // 0-100
    val virtualizer: Int = 0, // 0-100
    val surroundSound: Int = 0, // 0-100
    val reverbPreset: String = "Off", // Studio, Club, Hall, Arena, Off
    val sound3D: Boolean = false,
    val sampleRateOptimized: Boolean = true,
    val isDefault: Boolean = false
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val timestamp: Long = System.currentTimeMillis()
)
