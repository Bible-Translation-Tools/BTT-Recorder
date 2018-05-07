package org.wycliffeassociates.translationrecorder.login

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.door43.login.TermsOfUseActivity
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.login.fragments.FragmentCreateProfile
import org.wycliffeassociates.translationrecorder.login.fragments.FragmentReviewProfile
import org.wycliffeassociates.translationrecorder.login.interfaces.OnProfileCreatedListener
import org.wycliffeassociates.translationrecorder.login.interfaces.OnRedoListener
import java.io.File

/**
 * Created by sarabiaj on 3/9/2018.
 */

class LoginActivity : AppCompatActivity(), OnProfileCreatedListener, OnRedoListener {
    private lateinit var mFragmentCreateProfile: FragmentCreateProfile
    private var mFragmentReviewProfile: FragmentReviewProfile? = null
    private lateinit var profilesDirectory: File

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        if (savedInstanceState == null) {
            startActivityForResult(Intent(this, TermsOfUseActivity::class.java), 42)
            profilesDirectory = File(externalCacheDir, "profiles")
            initializeFragments()
            addFragments()
        }
    }

    private fun initializeFragments() {
        mFragmentCreateProfile = FragmentCreateProfile.newInstance(
                profilesDirectory,
                this
        )
        mFragmentCreateProfile.retainInstance = true
    }

    private fun addFragments() {
        fragmentManager.beginTransaction()
                .add(R.id.fragment_container, mFragmentCreateProfile)
                .commit()
    }

    override fun onProfileCreated(audio: File, hash: String) {
        mFragmentReviewProfile = FragmentReviewProfile.newInstance(audio, hash, this)
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, mFragmentReviewProfile)
                .commit()
    }

    override fun onRedo() {
        mFragmentCreateProfile = FragmentCreateProfile.newInstance(
                profilesDirectory,
                this
        )
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, mFragmentCreateProfile)
                .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode != TermsOfUseActivity.RESULT_OK) {
            finish()
        }
    }

}
