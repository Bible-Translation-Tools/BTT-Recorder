package org.wycliffeassociates.translationrecorder.login

import android.content.Intent
import android.os.Bundle
import com.door43.login.TermsOfUseActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.ActivityLoginBinding
import org.wycliffeassociates.translationrecorder.login.fragments.FragmentCreateProfile
import org.wycliffeassociates.translationrecorder.login.fragments.FragmentCreateProfile.OnProfileCreatedListener
import org.wycliffeassociates.translationrecorder.login.fragments.FragmentReviewProfile
import org.wycliffeassociates.translationrecorder.login.fragments.FragmentReviewProfile.OnReviewProfileListener
import org.wycliffeassociates.translationrecorder.permissions.PermissionActivity
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.wav.WavFile
import java.io.File
import javax.inject.Inject

/**
 * Created by sarabiaj on 3/9/2018.
 */
@AndroidEntryPoint
class LoginActivity : PermissionActivity(), OnProfileCreatedListener, OnReviewProfileListener {
    override fun onPermissionsAccepted() {}

    @Inject lateinit var directoryProvider: IDirectoryProvider

    private lateinit var fragmentCreateProfile: FragmentCreateProfile
    private var fragmentReviewProfile: FragmentReviewProfile? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            startActivityForResult(Intent(this, TermsOfUseActivity::class.java), 42)
            initializeFragments()
            addFragments()
        }
    }

    private fun initializeFragments() {
        fragmentCreateProfile = FragmentCreateProfile.newInstance(directoryProvider)
    }

    private fun addFragments() {
        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragmentCreateProfile)
                .commit()
    }

    override fun onProfileCreated(wav: WavFile, audio: File, hash: String) {
        fragmentReviewProfile = FragmentReviewProfile.newInstance(wav, audio, hash)
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragmentReviewProfile!!)
                .commit()
    }

    override fun onRedo() {
        fragmentCreateProfile = FragmentCreateProfile.newInstance(directoryProvider)
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragmentCreateProfile)
                .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode != TermsOfUseActivity.RESULT_OK) {
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, UserActivity::class.java))
        finish()
    }
}
