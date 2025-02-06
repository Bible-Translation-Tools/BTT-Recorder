package org.wycliffeassociates.translationrecorder.login

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.UserListItemBinding
import org.wycliffeassociates.translationrecorder.project.components.User



/**
 * Created by Dilip Maharjan on 05/01/2018
 */
class UserAdapter(
    private val users: List<Pair<User, Drawable>>,
    private val onItemClick: (User, Int) -> Unit
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    private var playing = false

    fun setUsers(users: List<Pair<User, Drawable>>) {
        this.users as MutableList
        this.users.clear()
        this.users.addAll(users)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return users.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = UserListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        if (position == 0) {
            holder.newUserTxt.visibility = View.VISIBLE
            holder.identicon.setImageResource(R.drawable.ic_person_add_black_48dp)
            holder.playIcon.visibility = View.INVISIBLE
            holder.identicon.rootView.setOnClickListener {
                it.context.startActivity(Intent(it.context, LoginActivity::class.java))
                (it.context as Activity).finish()
            }
        } else {
            holder.identicon.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            holder.identicon.setImageDrawable(user.second)
            holder.identicon.setOnClickListener {
                onItemClick.invoke(user.first, position)
            }
            holder.playIcon.setOnClickListener {
                if (!playing) {
                    playing = true
                    val player = MediaPlayer()
                    player.setDataSource(users[position].first.audio.toString())
                    player.prepare()
                    player.setOnCompletionListener {
                        player.release()
                        playing = false
                        holder.playIcon.isActivated = false
                    }
                    player.start()
                }
            }
        }
    }

    class ViewHolder(binding: UserListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val identicon = binding.identicon
        val playIcon = binding.playIcon
        val newUserTxt = binding.newUserTxt
    }

}