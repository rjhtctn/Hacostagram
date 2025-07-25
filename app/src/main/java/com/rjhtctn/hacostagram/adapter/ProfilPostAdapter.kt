package com.rjhtctn.hacostagram.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rjhtctn.hacostagram.R
import com.rjhtctn.hacostagram.databinding.RecyclerRowProfileBinding
import com.rjhtctn.hacostagram.model.Posts
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class ProfilPostAdapter :
    ListAdapter<Posts, ProfilPostAdapter.PostHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Posts>() {
            override fun areItemsTheSame(o: Posts, n: Posts) = o.id == n.id
            override fun areContentsTheSame(o: Posts, n: Posts) = o == n
        }
    }

    inner class PostHolder(val b: RecyclerRowProfileBinding) :
        RecyclerView.ViewHolder(b.root)

    var onEditClickListener: ((Posts, Int) -> Unit)? = null
    var onDeleteClickListener: ((Posts, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val b = RecyclerRowProfileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return PostHolder(b)
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) =
        with(holder.b) {

            val post = getItem(position)

            profilFeedKullaniciAdi.text = post.kullaniciAdi
            recyclerCommentText.text    = post.comment

            Picasso.get().load(post.imageUrl).into(recyclerImageView)

            feedPostTarih.text = SimpleDateFormat(
                "dd MMM yyyy HH:mm", Locale.getDefault()).format(post.time)

            border.visibility = if (position == 0) View.GONE else View.VISIBLE

            imageButton.setOnClickListener { v ->
                PopupMenu(holder.itemView.context, v).apply {
                    menuInflater.inflate(R.menu.menu_post_options, menu)
                    setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.menu_edit   -> { onEditClickListener?.invoke(post, position); true }
                            R.id.menu_delete -> { onDeleteClickListener?.invoke(post, position); true }
                            else             -> false
                        }
                    }
                }.show()
            }
        }
}