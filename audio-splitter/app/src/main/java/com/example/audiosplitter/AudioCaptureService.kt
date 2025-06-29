
package com.example.audiosplitter

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import kotlinx.coroutines.*

class AudioCaptureService : Service() {
    companion object {
        fun start(ctx: Context, intent: Intent) {
            ctx.startService(Intent(ctx, AudioCaptureService::class.java).apply {
                putExtra("captureIntent", intent)
            })
        }
    }

    private lateinit var mediaProjection: MediaProjection
    private lateinit var recorder: AudioRecord
    private lateinit var track: AudioTrack
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val capIntent = intent?.getParcelableExtra<Intent>("captureIntent")!!
        val mpManager = getSystemService(MediaProjectionManager::class.java)
        mediaProjection = mpManager.getMediaProjection(Activity.RESULT_OK, capIntent)

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(44100)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()

        val minBuf = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO,
                              AudioFormat.ENCODING_PCM_16BIT)
        recorder = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBuf)
            .setAudioPlaybackCaptureConfig(config)
            .build()
        recorder.startRecording()

        track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(44100)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build(),
            minBuf,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.play()

        scope.launch {
            val buf = ByteArray(minBuf)
            while (true) {
                val r = recorder.read(buf, 0, buf.size)
                if (r > 0) track.write(buf, 0, r)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        recorder.stop(); recorder.release()
        track.stop(); track.release()
        mediaProjection.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
