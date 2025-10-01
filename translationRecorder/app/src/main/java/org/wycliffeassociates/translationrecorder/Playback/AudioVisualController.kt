package org.wycliffeassociates.translationrecorder.Playback

import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import org.wycliffeassociates.translationrecorder.Playback.Editing.CutOp
import org.wycliffeassociates.translationrecorder.Playback.interfaces.AudioStateCallback
import org.wycliffeassociates.translationrecorder.Playback.interfaces.MediaControlReceiver
import org.wycliffeassociates.translationrecorder.Playback.player.WavPlayer
import org.wycliffeassociates.translationrecorder.WavFileLoader
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.wav.WavCue
import org.wycliffeassociates.translationrecorder.wav.WavFile
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * Created by sarabiaj on 10/27/2016.
 */
class AudioVisualController(
    audioTrack: AudioTrack,
    trackBufferSize: Int,
    wav: WavFile,
    directoryProvider: IDirectoryProvider,
    private var mCallback: AudioStateCallback,
) : MediaControlReceiver {

    private lateinit var mPlayer: WavPlayer
    lateinit var wavLoader: WavFileLoader

    var cutOp: CutOp = CutOp()
    private val mHandler: Handler
    private val mCues: MutableList<WavCue> = arrayListOf()

    init {
        initPlayer(audioTrack, trackBufferSize, wav, directoryProvider)
        mHandler = Handler(Looper.getMainLooper())
        mPlayer.setOnCompleteListener(object : WavPlayer.OnCompleteListener {
            override fun onComplete() {
                mCallback.onPlayerPaused()
            }
        })
    }

    fun setCueList(cueList: List<WavCue>) {
        mCues.clear()
        mCues.addAll(cueList)
        mPlayer.setCueList(cueList)
    }

    @Throws(IOException::class)
    private fun initPlayer(
        audioTrack: AudioTrack,
        trackBufferSize: Int,
        wav: WavFile,
        directoryProvider: IDirectoryProvider
    ) {
        wavLoader = WavFileLoader(wav, directoryProvider).apply {
            setOnVisualizationFileCreatedListener { mappedVisualizationFile ->
                mCallback.onVisualizationLoaded(
                    mappedVisualizationFile
                )
            }
        }
        mCues.clear()
        mCues.addAll(wav.metadata.cuePoints)
        mCues.sortBy { it.location }

        mPlayer = WavPlayer(
            audioTrack,
            trackBufferSize,
            wavLoader.mapAndGetAudioBuffer(),
            cutOp,
            mCues
        )
    }

    @Throws(IllegalStateException::class)
    override fun play() {
        mPlayer.play()
    }

    override fun pause() {
        mPlayer.pause()
    }

    @Throws(IllegalStateException::class)
    override fun seekNext() {
        mPlayer.seekNext()
    }

    @Throws(IllegalStateException::class)
    override fun seekPrevious() {
        mPlayer.seekPrevious()
    }

    fun seekTo(frame: Int) {
        mPlayer.seekToAbsolute(frame)
    }

    @Throws(IllegalStateException::class)
    override fun getAbsoluteLocationMs(): Int {
        return mPlayer.absoluteLocationMs
    }

    @get:Throws(IllegalStateException::class)
    val absoluteLocationInFrames: Int
        get() = mPlayer.absoluteLocationInFrames

    @get:Throws(IllegalStateException::class)
    val relativeLocationMs: Int
        get() = mPlayer.relativeLocationMs

    @get:Throws(IllegalStateException::class)
    val relativeLocationInFrames: Int
        get() = mPlayer.relativeLocationInFrames

    @Throws(IllegalStateException::class)
    override fun getRelativeDurationMs(): Int {
        return mPlayer.relativeDurationMs
    }

    @get:Throws(IllegalStateException::class)
    val absoluteDurationInFrames: Int
        get() = mPlayer.absoluteDurationInFrames

    @get:Throws(IllegalStateException::class)
    val relativeDurationInFrames: Int
        get() = mPlayer.relativeDurationInFrames

    val isPlaying: Boolean
        get() = mPlayer.isPlaying

    fun cut() {
        cutOp.cut(mPlayer.loopStart, mPlayer.loopEnd)
        mPlayer.clearLoopPoints()
    }

    @Throws(IllegalStateException::class)
    fun dropStartMarker() {
        mPlayer.loopStart = mPlayer.absoluteLocationInFrames
    }

    @Throws(IllegalStateException::class)
    fun dropEndMarker() {
        mPlayer.loopEnd = mPlayer.absoluteLocationInFrames
    }

    fun dropVerseMarker(label: String?, location: Int) {
        mCues.add(WavCue(label, location))
    }

    fun clearLoopPoints() {
        mPlayer.clearLoopPoints()
    }

    fun undo() {
        if (cutOp.hasCut()) {
            cutOp.undo()
        }
    }

    val loopStart: Int
        get() = mPlayer.loopStart

    val loopEnd: Int
        get() = mPlayer.loopEnd

    @Throws(IllegalStateException::class)
    fun scrollAudio(distX: Float) {
        var seekTo = max(
            min(
                ((distX * 230) + mPlayer.absoluteLocationInFrames).toInt().toDouble(),
                mPlayer.absoluteDurationInFrames.toDouble()
            ), 0.0
        ).toInt()
        if (distX > 0) {
            val skip = cutOp.skip(seekTo)
            if (skip != -1) {
                seekTo = skip + 1
            }
        } else {
            val skip = cutOp.skipReverse(seekTo)
            if (skip != Int.MAX_VALUE) {
                seekTo = skip - 1
            }
        }
        mPlayer.seekToAbsolute(seekTo)
        mCallback.onLocationUpdated()
    }

    @Throws(IllegalStateException::class)
    fun setStartMarker(relativeLocation: Int) {
        mPlayer.loopStart = max(
            cutOp.relativeLocToAbsolute(relativeLocation, false).toDouble(), 0.0
        ).toInt()
    }

    @Throws(IllegalStateException::class)
    fun setEndMarker(relativeLocation: Int) {
        mPlayer.loopEnd = min(
            cutOp.relativeLocToAbsolute(relativeLocation, false).toDouble(),
            mPlayer.absoluteDurationInFrames.toDouble()
        ).toInt()
    }
}