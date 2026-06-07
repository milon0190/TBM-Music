package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.annotation.OptIn
import com.example.data.Song
import com.example.data.MusicRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random
import kotlin.math.sin

class AudioPlayerManager(
    private val context: Context,
    private val repository: MusicRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val random = Random()

    // Playback States
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    // Smart Features & Audio Adjustments
    private val _playbackSpeed = MutableStateFlow(1.0f) // 0.5f - 3.0f
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f) // 0.5f - 2.0f
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _shuffleMode = MutableStateFlow(ShuffleMode.OFF)
    val shuffleMode: StateFlow<ShuffleMode> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // --- 64-Band Professional Equalizer & Custom Profiles ---
    private val _equalizerBands = MutableStateFlow(FloatArray(64) { 0f })
    val equalizerBands: StateFlow<FloatArray> = _equalizerBands.asStateFlow()

    private val _bassBoost = MutableStateFlow(15) // 0 - 100
    val bassBoost: StateFlow<Int> = _bassBoost.asStateFlow()

    private val _trebleBoost = MutableStateFlow(30) // 0 - 100
    val trebleBoost: StateFlow<Int> = _trebleBoost.asStateFlow()

    private val _vocalBoost = MutableStateFlow(25) // 0 - 100
    val vocalBoost: StateFlow<Int> = _vocalBoost.asStateFlow()

    private val _dynamicLoudness = MutableStateFlow(true)
    val dynamicLoudness: StateFlow<Boolean> = _dynamicLoudness.asStateFlow()

    private val _audioEnhancementMode = MutableStateFlow("Studio Mastering") // e.g. "Audiophile Reference", "Concert Hall", etc.
    val audioEnhancementMode: StateFlow<String> = _audioEnhancementMode.asStateFlow()

    // --- Intelligent Headphone Detection ---
    private val _headphoneConnected = MutableStateFlow(false)
    val headphoneConnected: StateFlow<Boolean> = _headphoneConnected.asStateFlow()

    private val _headphoneType = MutableStateFlow("Disconnected") // "Wired", "Bluetooth Base", "Wireless Studio Pro Earbuds"
    val headphoneType: StateFlow<String> = _headphoneType.asStateFlow()

    private val _autoPauseDisconnected = MutableStateFlow(true)
    val autoPauseDisconnected: StateFlow<Boolean> = _autoPauseDisconnected.asStateFlow()

    private val _autoResumeConnected = MutableStateFlow(true)
    val autoResumeConnected: StateFlow<Boolean> = _autoResumeConnected.asStateFlow()

    private val _hearingSafetyLimit = MutableStateFlow(true)
    val hearingSafetyLimit: StateFlow<Boolean> = _hearingSafetyLimit.asStateFlow()

    private val _hearingSafetyLevelDb = MutableStateFlow(72.5f) // simulated real-time dB
    val hearingSafetyLevelDb: StateFlow<Float> = _hearingSafetyLevelDb.asStateFlow()

    private val _soundCalibrationPassed = MutableStateFlow(false)
    val soundCalibrationPassed: StateFlow<Boolean> = _soundCalibrationPassed.asStateFlow()

    // --- Premium Bluetooth Optimization ---
    private val _bluetoothOptimizationEnabled = MutableStateFlow(true)
    val bluetoothOptimizationEnabled: StateFlow<Boolean> = _bluetoothOptimizationEnabled.asStateFlow()

    private val _lowLatencyMode = MutableStateFlow(false)
    val lowLatencyMode: StateFlow<Boolean> = _lowLatencyMode.asStateFlow()

    private val _hiResWirelessAudio = MutableStateFlow(true)
    val hiResWirelessAudio: StateFlow<Boolean> = _hiResWirelessAudio.asStateFlow()

    private val _smartCodec = MutableStateFlow("LDAC (24bit/96kHz - 990kbps)")
    val smartCodec: StateFlow<String> = _smartCodec.asStateFlow()

    private val _activeBluetoothDevice = MutableStateFlow("Sony WH-1000XM5")
    val activeBluetoothDevice: StateFlow<String> = _activeBluetoothDevice.asStateFlow()

    // --- Audiophile Elements & DSP ---
    private val _bitPerfectMode = MutableStateFlow(false)
    val bitPerfectMode: StateFlow<Boolean> = _bitPerfectMode.asStateFlow()

    private val _losslessPlaybackEngine = MutableStateFlow(true)
    val losslessPlaybackEngine: StateFlow<Boolean> = _losslessPlaybackEngine.asStateFlow()

    private val _audioProcessingBitDepth = MutableStateFlow(32) // 24 or 32 bit
    val audioProcessingBitDepth: StateFlow<Int> = _audioProcessingBitDepth.asStateFlow()

    // --- AI Smart Audio System ---
    private val _aiAudioTuningEnabled = MutableStateFlow(true)
    val aiAudioTuningEnabled: StateFlow<Boolean> = _aiAudioTuningEnabled.asStateFlow()

    // --- Advanced Visualizers & Beat State ---
    private val _currentVisualizerMode = MutableStateFlow("Circular Audio") // "3D Spectrum", "Circular Audio", "Waveform", "Particle Music", "Neon Reactive"
    val currentVisualizerMode: StateFlow<String> = _currentVisualizerMode.asStateFlow()

    private var _isBeatDetected = MutableStateFlow(false)
    val isBeatDetected: StateFlow<Boolean> = _isBeatDetected.asStateFlow()

    private val _virtualizer = MutableStateFlow(10) // 0 - 100
    val virtualizer: StateFlow<Int> = _virtualizer.asStateFlow()

    private val _surroundSound = MutableStateFlow(5) // 0 - 100
    val surroundSound: StateFlow<Int> = _surroundSound.asStateFlow()

    private val _reverbPreset = MutableStateFlow("Studio") // Studio, Club, Hall, Arena, Off
    val reverbPreset: StateFlow<String> = _reverbPreset.asStateFlow()

    private val _sound3D = MutableStateFlow(true)
    val sound3D: StateFlow<Boolean> = _sound3D.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(true)
    val gaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()

    private val _crossfadeEnabled = MutableStateFlow(false)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    // Sleep Timer
    private val _sleepTimerSec = MutableStateFlow(0) // 0 means inactive
    val sleepTimerSec: StateFlow<Int> = _sleepTimerSec.asStateFlow()

    // Real-time Visualizer Spectrum (64 bars of active frequencies)
    private val _visualizerAmplitudes = MutableStateFlow(FloatArray(64) { 0.1f })
    val visualizerAmplitudes: StateFlow<FloatArray> = _visualizerAmplitudes.asStateFlow()

    private var playbackJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var visualizerJob: Job? = null

    // Broadcast Receiver to detect physical headphone insertion and pullout
    private val headphoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", 0)
                    if (state > 0) {
                        // Plugged
                        _headphoneConnected.value = true
                        _headphoneType.value = "Wired"
                        if (_autoResumeConnected.value && !_isPlaying.value) {
                            togglePlayPause()
                        }
                        // Swap equalizer curve for Wired Headphone profile
                        applyHeadphoneOptimizedEQ("Wired")
                    } else {
                        // Unplugged
                        _headphoneConnected.value = false
                        _headphoneType.value = "Disconnected"
                        if (_autoPauseDisconnected.value && _isPlaying.value) {
                            togglePlayPause()
                        }
                    }
                }
            }
        }
    }

    init {
        // Start visualizer background feed
        startVisualizerLoop()

        // Safely register Broadcast Receiver
        try {
            val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
            context.registerReceiver(headphoneReceiver, filter)
        } catch (e: Exception) {
            // Safe fallback when run in standard unit-test contexts
        }
    }

    enum class ShuffleMode { OFF, SOCIAL_SHUFFLE, MOOD_SHUFFLE }
    enum class RepeatMode { OFF, REPEAT_ALL, REPEAT_ONE }

    fun setQueue(songs: List<Song>, startIndex: Int) {
        _queue.value = songs
        if (startIndex in songs.indices) {
            _queueIndex.value = startIndex
            playSong(songs[startIndex])
        }
    }

    fun playSong(song: Song) {
        playbackJob?.cancel()
        _currentSong.value = song
        _currentPositionMs.value = 0L
        _isPlaying.value = true

        // Record history and increment play count in DB
        scope.launch(Dispatchers.IO) {
            repository.recordHistory(song.id)
        }

        // Start time ticking job
        startPlaybackTick()
    }

    fun togglePlayPause() {
        if (_currentSong.value == null && _queue.value.isNotEmpty()) {
            _queueIndex.value = 0
            playSong(_queue.value[0])
            return
        }
        _isPlaying.value = !_isPlaying.value
        if (_isPlaying.value) {
            startPlaybackTick()
        } else {
            playbackJob?.cancel()
        }
    }

    fun nextTrack() {
        val songs = _queue.value
        if (songs.isEmpty()) return

        var nextIndex = _queueIndex.value + 1
        when (_shuffleMode.value) {
            ShuffleMode.OFF -> {
                if (nextIndex >= songs.size) {
                    nextIndex = if (_repeatMode.value == RepeatMode.REPEAT_ALL) 0 else -1
                }
            }
            ShuffleMode.SOCIAL_SHUFFLE -> {
                // Completely random shuffle
                nextIndex = random.nextInt(songs.size)
            }
            ShuffleMode.MOOD_SHUFFLE -> {
                // Smart shuffle - prioritize songs of the matching mood of current song or bpm
                val current = _currentSong.value
                val matchedMoodSongs = songs.filter { current != null && it.mood == current.mood && it.id != current.id }
                nextIndex = if (matchedMoodSongs.isNotEmpty() && random.nextDouble() < 0.70) {
                    val matchingSong = matchedMoodSongs[random.nextInt(matchedMoodSongs.size)]
                    songs.indexOf(matchingSong)
                } else {
                    random.nextInt(songs.size)
                }
            }
        }

        if (nextIndex in songs.indices) {
            _queueIndex.value = nextIndex
            playSong(songs[nextIndex])
        } else {
            _isPlaying.value = false
            playbackJob?.cancel()
        }
    }

    fun previousTrack() {
        val songs = _queue.value
        if (songs.isEmpty() || _queueIndex.value == -1) return

        var prevIndex = _queueIndex.value - 1
        if (prevIndex < 0) {
            prevIndex = if (_repeatMode.value == RepeatMode.REPEAT_ALL) songs.size - 1 else 0
        }

        if (prevIndex in songs.indices) {
            _queueIndex.value = prevIndex
            playSong(songs[prevIndex])
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = _currentSong.value?.durationMs ?: 0L
        _currentPositionMs.value = positionMs.coerceIn(0L, duration)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed.coerceIn(0.5f, 3.0f)
    }

    fun setPitch(pitch: Float) {
        _pitch.value = pitch.coerceIn(0.5f, 2.0f)
    }

    fun setShuffleMode(mode: ShuffleMode) {
        _shuffleMode.value = mode
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
    }

    fun setEqualizerBands(bands: FloatArray) {
        if (bands.size == 64) {
            _equalizerBands.value = bands
        } else if (bands.isNotEmpty()) {
            val newBands = FloatArray(64) { i ->
                val srcIdx = (i * bands.size / 64f).toInt().coerceIn(bands.indices)
                bands[srcIdx]
            }
            _equalizerBands.value = newBands
        }
    }

    fun applyHeadphoneOptimizedEQ(type: String) {
        // Wired vs Bluetooth vs Wireless Studio Earbuds optimized curve mapping
        val presetBands = FloatArray(64) { idx ->
            when (type.lowercase()) {
                "wired" -> {
                    // Crisp treble and controlled warm mids: generic audiophile target curves
                    if (idx < 10) 2f else if (idx in 20..40) -1.5f else if (idx > 50) 4f else 0.5f
                }
                "bluetooth base", "sony wh-1000xm5" -> {
                    // Deep subwoofer bass and airy top end: consumer target curves
                    if (idx < 12) 6f else if (idx in 12..32) -2f else if (idx > 48) 5f else 0f
                }
                "wireless studio pro earbuds" -> {
                    // Harman target curve simulation
                    if (idx < 8) 4.5f else if (idx in 15..35) 1.5f else if (idx > 52) 5.5f else 0.5f
                }
                else -> 0f
            }
        }
        setEqualizerBands(presetBands)
        _headphoneType.value = type
        _headphoneConnected.value = type != "Disconnected"
    }

    fun setBassBoost(level: Int) {
        _bassBoost.value = level.coerceIn(0, 100)
    }

    fun setTrebleBoost(level: Int) {
        _trebleBoost.value = level.coerceIn(0, 100)
    }

    fun setVocalBoost(level: Int) {
        _vocalBoost.value = level.coerceIn(0, 100)
    }

    fun setDynamicLoudness(enabled: Boolean) {
        _dynamicLoudness.value = enabled
    }

    fun setAudioEnhancementMode(mode: String) {
        _audioEnhancementMode.value = mode
        
        // Auto-configure optimal dsp parameters depending on enhancement presets!
        when (mode) {
            "Studio Mastering" -> {
                setBassBoost(15)
                setTrebleBoost(25)
                setVocalBoost(10)
                setVirtualizer(20)
                setSurroundSound(10)
                setReverbPreset("Studio")
            }
            "Audiophile Reference" -> {
                setBassBoost(0)
                setTrebleBoost(0)
                setVocalBoost(0)
                setVirtualizer(0)
                setSurroundSound(0)
                setReverbPreset("Off")
                setBitPerfectMode(true)
            }
            "Concert Hall" -> {
                setBassBoost(45)
                setTrebleBoost(15)
                setVocalBoost(20)
                setVirtualizer(65)
                setSurroundSound(85)
                setReverbPreset("Hall")
            }
            "Cinema Sound" -> {
                setBassBoost(55)
                setTrebleBoost(30)
                setVocalBoost(40)
                setVirtualizer(75)
                setSurroundSound(90)
                setReverbPreset("Arena")
            }
            "Gaming Sound" -> {
                setBassBoost(60)
                setTrebleBoost(45)
                setVocalBoost(15)
                setVirtualizer(80)
                setSurroundSound(70)
                setReverbPreset("Club")
            }
            "Podcast" -> {
                setBassBoost(5)
                setTrebleBoost(10)
                setVocalBoost(90)
                setVirtualizer(5)
                setSurroundSound(15)
                setReverbPreset("Studio")
            }
        }
    }

    fun setHeadphoneConnected(connected: Boolean) {
        _headphoneConnected.value = connected
        if (!connected) {
            _headphoneType.value = "Disconnected"
        }
    }

    fun setHeadphoneType(type: String) {
        _headphoneType.value = type
        _headphoneConnected.value = type != "Disconnected"
    }

    fun setAutoPauseDisconnected(enabled: Boolean) {
        _autoPauseDisconnected.value = enabled
    }

    fun setAutoResumeConnected(enabled: Boolean) {
        _autoResumeConnected.value = enabled
    }

    fun setHearingSafetyLimit(enabled: Boolean) {
        _hearingSafetyLimit.value = enabled
    }

    fun setHearingSafetyLevelDb(db: Float) {
        _hearingSafetyLevelDb.value = db
    }

    fun setSoundCalibrationPassed(passed: Boolean) {
        _soundCalibrationPassed.value = passed
    }

    fun setBluetoothOptimizationEnabled(enabled: Boolean) {
        _bluetoothOptimizationEnabled.value = enabled
    }

    fun setLowLatencyMode(enabled: Boolean) {
        _lowLatencyMode.value = enabled
    }

    fun setHiResWirelessAudio(enabled: Boolean) {
        _hiResWirelessAudio.value = enabled
    }

    fun setSmartCodec(codec: String) {
        _smartCodec.value = codec
    }

    fun setActiveBluetoothDevice(device: String) {
        _activeBluetoothDevice.value = device
    }

    fun setBitPerfectMode(enabled: Boolean) {
        _bitPerfectMode.value = enabled
    }

    fun setLosslessPlaybackEngine(enabled: Boolean) {
        _losslessPlaybackEngine.value = enabled
    }

    fun setAudioProcessingBitDepth(bitDepth: Int) {
        _audioProcessingBitDepth.value = bitDepth
    }

    fun setAiAudioTuningEnabled(enabled: Boolean) {
        _aiAudioTuningEnabled.value = enabled
    }

    fun setCurrentVisualizerMode(mode: String) {
        _currentVisualizerMode.value = mode
    }

    fun setVirtualizer(level: Int) {
        _virtualizer.value = level.coerceIn(0, 100)
    }

    fun setSurroundSound(level: Int) {
        _surroundSound.value = level.coerceIn(0, 100)
    }

    fun setReverbPreset(preset: String) {
        _reverbPreset.value = preset
    }

    fun setSound3D(enabled: Boolean) {
        _sound3D.value = enabled
    }

    fun setGaplessPlayback(enabled: Boolean) {
        _gaplessPlayback.value = enabled
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        _crossfadeEnabled.value = enabled
    }

    // Timer actions
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerSec.value = minutes * 60
        if (minutes > 0) {
            sleepTimerJob = scope.launch {
                while (_sleepTimerSec.value > 0) {
                    delay(1000)
                    _sleepTimerSec.value -= 1
                }
                // Sleep timer triggered, pause playback
                _isPlaying.value = false
                playbackJob?.cancel()
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerSec.value = 0
    }

    private fun startPlaybackTick() {
        playbackJob = scope.launch {
            while (_isPlaying.value) {
                delay((100 / _playbackSpeed.value).toLong())
                val currentSong = _currentSong.value ?: break
                val newPos = _currentPositionMs.value + 100
                if (newPos >= currentSong.durationMs) {
                    // Song completed
                    if (_repeatMode.value == RepeatMode.REPEAT_ONE) {
                        _currentPositionMs.value = 0
                    } else {
                        nextTrack()
                    }
                } else {
                    _currentPositionMs.value = newPos
                }
            }
        }
    }

    private fun startVisualizerLoop() {
        visualizerJob = scope.launch {
            var tick = 0f
            while (true) {
                delay(40) // ~25 FPS visualizer updates for extremely organic response
                if (_isPlaying.value) {
                    tick += 0.15f
                    val bassFactor = _bassBoost.value / 100.0f
                    val trebleFactor = _trebleBoost.value / 100.0f
                    val vocalFactor = _vocalBoost.value / 100.0f
                    val speedFactor = _playbackSpeed.value

                    val newAlphas = FloatArray(64) { index ->
                        // Generate overlapping sine waves modulated by frequency, bass booster, treble booster, and vocal booster
                        val baseFrequency = (index + 1) * 0.25f
                        var amplitude = (sin(tick * speedFactor + baseFrequency) * 0.4f + 0.5f).toFloat()

                        if (index < 12) {
                            // Bass bands responding to Bass Boost
                            amplitude += bassFactor * 0.5f
                        } else if (index in 12..28) {
                            // Mid/Vocal bands responding to Vocal Boost
                            amplitude += vocalFactor * 0.35f
                        } else if (index > 48) {
                            // Treble bands responding to Treble Boost
                            amplitude += trebleFactor * 0.4f
                        }

                        // Add small noise to represent micro-frequencies
                        amplitude += (random.nextFloat() * 0.08f - 0.04f)
                        amplitude.coerceIn(0.05f, 1.0f)
                    }
                    _visualizerAmplitudes.value = newAlphas

                    // Simulated real-time Hearing DB level fluctuation around average volume
                    if (_hearingSafetyLimit.value) {
                        val baseVol = bassFactor * 30 + trebleFactor * 20 + vocalFactor * 15 + 50
                        _hearingSafetyLevelDb.value = (baseVol + sin(tick) * 3f + random.nextFloat()).coerceIn(40f, 85f)
                    } else {
                        val baseVol = bassFactor * 40 + trebleFactor * 35 + vocalFactor * 20 + 55
                        _hearingSafetyLevelDb.value = (baseVol + sin(tick) * 5f + random.nextFloat() * 2f).coerceIn(40f, 105f)
                    }

                    // --- Real-Time Beat Detection ---
                    // Compute average energy of lowest 10 bass bands (0 to 9)
                    var bassEnergy = 0f
                    for (i in 0..9) {
                        bassEnergy += newAlphas[i]
                    }
                    bassEnergy /= 10f
                    _isBeatDetected.value = bassEnergy > 0.65f
                } else {
                    // Decay state when paused
                    val decreased = _visualizerAmplitudes.value.map { (it * 0.85f).coerceAtLeast(0.02f) }.toFloatArray()
                    _visualizerAmplitudes.value = decreased
                    _isBeatDetected.value = false
                }
            }
        }
    }
}
