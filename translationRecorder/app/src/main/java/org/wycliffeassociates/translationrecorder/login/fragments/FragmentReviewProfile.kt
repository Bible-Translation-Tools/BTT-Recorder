package org.wycliffeassociates.translationrecorder.login.fragments


import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.pixplicity.sharp.Sharp
import dagger.hilt.android.AndroidEntryPoint
import jdenticon.Jdenticon
import org.wycliffeassociates.translationrecorder.*
import org.wycliffeassociates.translationrecorder.AudioVisualization.WavVisualizer
import org.wycliffeassociates.translationrecorder.Playback.Editing.CutOp
import org.wycliffeassociates.translationrecorder.Playback.overlays.WaveformLayer
import org.wycliffeassociates.translationrecorder.Playback.player.WavPlayer
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.FragmentReviewProfileBinding
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import org.wycliffeassociates.translationrecorder.project.components.User
import org.wycliffeassociates.translationrecorder.wav.WavFile
import java.io.File
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FragmentReviewProfile : Fragment(), WaveformLayer.WaveformDrawDelegator {

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var prefs: IPreferenceRepository

    interface OnReviewProfileListener {
        fun onRedo()
    }

    private var _binding: FragmentReviewProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var wav: WavFile
    private lateinit var audio: File
    private lateinit var hash: String
    private lateinit var waveformLayer: WaveformLayer
    private lateinit var wavVis: WavVisualizer
    private lateinit var player: WavPlayer

    private var onReviewProfileListener: OnReviewProfileListener? = null

    private var layoutInitialized = false

    private val trackBufferSize = AudioTrack.getMinBufferSize(
        AudioInfo.SAMPLERATE,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val audioFormat = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(AudioInfo.SAMPLERATE)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    private val audioTrack = AudioTrack(
        audioAttributes,
        audioFormat,
        trackBufferSize,
        AudioTrack.MODE_STREAM,
        AudioManager.AUDIO_SESSION_ID_GENERATE
    )

    companion object {
        fun newInstance(wav: WavFile, audio: File, hash: String): FragmentReviewProfile {
            val fragment = FragmentReviewProfile()
            fragment.wav = wav
            fragment.audio = audio
            fragment.hash = hash
            return fragment
        }
    }

    override fun onDrawWaveform(canvas: Canvas?, paint: Paint?) {
        if(layoutInitialized) {
            paint?.let {
                canvas?.drawLines(
                    wavVis.getMinimap(
                        canvas.height,
                        canvas.width,
                        player.absoluteDurationInFrames
                    ),
                    paint
                )
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewProfileBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            renderIdenticon(hash, iconHash)
            btnRedo.setOnClickListener {
                onReviewProfileListener?.apply {
                    audio.delete()
                    audio.createNewFile()
                    onRedo()
                }
            }
            btnYes.setOnClickListener {
                val profilesDir = directoryProvider.profilesDir
                if (profilesDir.exists().not()) {
                    profilesDir.mkdirs()
                }
                val newAudio = File(profilesDir.absolutePath + "/" + audio.name)
                audio.renameTo(newAudio)
                audio = newAudio
                val user = User(audio, hash)
                db.addUser(user)
                prefs.setDefaultPref(Settings.KEY_PROFILE, user.id)
                val mainActivityIntent = Intent(activity, MainMenu::class.java)
                requireActivity().startActivity(mainActivityIntent)
                requireActivity().finish()
            }
            waveformLayer = WaveformLayer.newInstance(activity, this@FragmentReviewProfile)
            waveformFrame.addView(waveformLayer)
            lateinit var layoutListener: ViewTreeObserver.OnGlobalLayoutListener
            layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                initializeRenderer()
                layoutInitialized = true
                waveformLayer.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
            }
            waveformLayer.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
            btnPlay.setOnClickListener {
                showPauseButton()
                player.play()
            }
            btnPause.setOnClickListener {
                showPlayButton()
                player.pause()
            }
            //audioTrack = (requireActivity().application as TranslationRecorderApp).audioTrack
            //trackBufferSize = (requireActivity().application as TranslationRecorderApp).trackBufferSize
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onReviewProfileListener = context as OnReviewProfileListener
    }

    private fun renderIdenticon(hash: String, view: ImageView) {
        val svg = Jdenticon.toSvg(hash, 512, 0f)
        Sharp.loadString(svg).into(view)
    }

    private fun initializeRenderer() {
        showPlayButton()
        wav.overwriteHeaderData()
        val wavFileLoader = WavFileLoader(wav, directoryProvider)
        val numThreads = 4
        val uncompressed = wavFileLoader.mapAndGetAudioBuffer()
        wavVis = WavVisualizer(
                uncompressed,
                null,
                numThreads,
                binding.waveformFrame.width,
                binding.waveformFrame.height,
                binding.waveformFrame.width,
                CutOp()
        )
        player = WavPlayer(audioTrack, trackBufferSize, uncompressed, CutOp(), LinkedList())
        player.setOnCompleteListener(
            object : WavPlayer.OnCompleteListener {
                override fun onComplete() {
                    showPlayButton()
                }
            }
        )
    }

    private fun showPauseButton() {
        Utils.swapViews(
            arrayOf(binding.btnPause),
            arrayOf(binding.btnPlay)
        )
    }

    private fun showPlayButton() {
        Utils.swapViews(
            arrayOf(binding.btnPlay),
            arrayOf(binding.btnPause)
        )
    }
}
