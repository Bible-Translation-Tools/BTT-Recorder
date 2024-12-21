package org.wycliffeassociates.translationrecorder.login.fragments


import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
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
import org.wycliffeassociates.translationrecorder.login.interfaces.OnRedoListener
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

    private var _binding: FragmentReviewProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var wav: WavFile
    private lateinit var audio: File
    private lateinit var hash: String
    private lateinit var onRedoListener: OnRedoListener
    private lateinit var waveformLayer: WaveformLayer
    private lateinit var wavVis: WavVisualizer
    private lateinit var player: WavPlayer
    private var layoutInitialized = false
    private lateinit var audioTrack: AudioTrack
    private var trackBufferSize: Int = 0

    companion object {
        fun newInstance(wav: WavFile, audio: File, hash: String, redo: OnRedoListener): FragmentReviewProfile {
            val fragment = FragmentReviewProfile()
            fragment.wav = wav
            fragment.audio = audio
            fragment.hash = hash
            fragment.onRedoListener = redo
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
                onRedoListener.let {
                    audio.delete()
                    audio.createNewFile()
                    onRedoListener.onRedo()
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
            audioTrack = (requireActivity().application as TranslationRecorderApp).audioTrack
            trackBufferSize = (requireActivity().application as TranslationRecorderApp).trackBufferSize
        }
    }

    private fun renderIdenticon(hash: String, view: ImageView) {
        val svg = Jdenticon.toSvg(hash, 512, 0f)
        Sharp.loadString(svg).into(view)
    }

    private fun initializeRenderer() {
        showPlayButton()
        wav.overwriteHeaderData()
        val wavFileLoader = WavFileLoader(wav, activity)
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
        player.setOnCompleteListener {
            showPlayButton()
        }
    }

    private fun showPauseButton() {
        Utils.swapViews(arrayOf(binding.btnPause), arrayOf(binding.btnPlay))
    }

    private fun showPlayButton() {
        Utils.swapViews(arrayOf(binding.btnPlay), arrayOf(binding.btnPause))
    }
}
