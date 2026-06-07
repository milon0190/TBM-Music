package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    // --- Songs Queries ---
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY addedTimestamp DESC")
    fun getRecentlyAddedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 20")
    fun getRecentlyPlayedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT 15")
    fun getMostPlayedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): Song?

    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY title ASC")
    fun getSongsByGenre(genre: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY title ASC")
    fun getSongsByAlbum(album: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY title ASC")
    fun getSongsByArtist(artist: String): Flow<List<Song>>

    @Query("SELECT DISTINCT genre FROM songs ORDER BY genre ASC")
    fun getAllGenres(): Flow<List<String>>

    @Query("SELECT DISTINCT album FROM songs ORDER BY album ASC")
    fun getAllAlbums(): Flow<List<String>>

    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Update
    suspend fun updateSong(song: Song)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavoriteStatus(songId: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSong(songId: Long)

    @Query("SELECT * FROM songs s1 WHERE EXISTS (SELECT 1 FROM songs s2 WHERE s2.id != s1.id AND s2.title = s1.title AND s2.artist = s1.artist)")
    fun findDuplicateSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE lyrics = '' OR lyrics IS NULL ORDER BY title ASC")
    fun findSongsMissingLyrics(): Flow<List<Song>>


    // --- Playlists Queries ---
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deleteSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("""
        SELECT * FROM songs 
        INNER JOIN playlist_song_cross_ref ON songs.id = playlist_song_cross_ref.songId 
        WHERE playlist_song_cross_ref.playlistId = :playlistId 
        ORDER BY songs.title ASC
    """)
    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>>


    // --- Audio Profiles Queries ---
    @Query("SELECT * FROM audio_profiles")
    fun getAllAudioProfiles(): Flow<List<AudioProfile>>

    @Query("SELECT * FROM audio_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAudioProfile(): AudioProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioProfile(profile: AudioProfile): Long

    @Query("UPDATE audio_profiles SET isDefault = 0")
    suspend fun clearDefaultAudioProfile()

    @Query("UPDATE audio_profiles SET isDefault = 1 WHERE id = :profileId")
    suspend fun setDefaultAudioProfile(profileId: Long)

    @Delete
    suspend fun deleteAudioProfile(profile: AudioProfile)


    // --- Playback History Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: PlaybackHistoryEntry)

    @Query("DELETE FROM playback_history")
    suspend fun clearPlaybackHistory()
}
