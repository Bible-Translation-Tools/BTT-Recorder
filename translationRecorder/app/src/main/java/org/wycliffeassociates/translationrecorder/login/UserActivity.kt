package org.wycliffeassociates.translationrecorder.login

import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pixplicity.sharp.Sharp
import dagger.hilt.android.AndroidEntryPoint
import jdenticon.Jdenticon
import org.wycliffeassociates.translationrecorder.DocumentationActivity
import org.wycliffeassociates.translationrecorder.MainMenu
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.ActivityUserBinding
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import org.wycliffeassociates.translationrecorder.project.components.User
import javax.inject.Inject

/**
 * Created by sarabiaj on 3/9/2018.
 */
@AndroidEntryPoint
class UserActivity : AppCompatActivity() {

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var prefs: IPreferenceRepository

    private lateinit var binding: ActivityUserBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false)
        }

        val orientation = resources.configuration.orientation
        var layoutManager = GridLayoutManager(this, 4) as RecyclerView.LayoutManager
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            layoutManager = GridLayoutManager(this, 3)
        }

        with(binding) {
            recycler.layoutManager = layoutManager
            recycler.itemAnimator = DefaultItemAnimator()
            val adapter = UserAdapter(userList(), ::onItemClick)
            recycler.adapter = adapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.user_page_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_help -> {
                val help = Intent(this, DocumentationActivity::class.java)
                startActivity(help)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun userList(): List<Pair<User, Drawable>> {
        val userList = arrayListOf<Pair<User, Drawable>>()
        val newEmptyUser = User()
        userList.add(Pair(newEmptyUser, resources.getDrawable(R.drawable.ic_person_add_black_48dp)))
        val users = db.allUsers
        for (user in users) {
            val identicon = generateIdenticon(user.hash!!)
            userList.add(Pair(user, identicon))
        }
        return userList
    }

    private fun generateIdenticon(hash: String): Drawable {
        val svg = Jdenticon.toSvg(hash, 512, 0f)
        return Sharp.loadString(svg).drawable
    }

    private fun onItemClick(user: User, position: Int) {
        Toast.makeText(this, "Identicon $position", Toast.LENGTH_LONG).show()
        prefs.setDefaultPref(SettingsActivity.KEY_PROFILE, user.id)
        val mainActivityIntent = Intent(this, MainMenu::class.java)
        startActivity(mainActivityIntent)
        finish()
    }

}

