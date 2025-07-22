package com.rjhtctn.hacostagram.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.rjhtctn.hacostagram.R
import com.rjhtctn.hacostagram.databinding.RecyclerRowProfileBinding
import com.rjhtctn.hacostagram.model.Posts
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class ProfilPostAdapter(private val postList : ArrayList<Posts>) : RecyclerView.Adapter<ProfilPostAdapter.PostHolder>() {
    class PostHolder(val binding : RecyclerRowProfileBinding) : RecyclerView.ViewHolder(binding.root)

    var onEditClickListener: ((Posts, Int) -> Unit)? = null
    var onDeleteClickListener: ((Posts, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val binding = RecyclerRowProfileBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PostHolder(binding)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) = with(holder.binding){
        val post = postList[position]
        profilFeedKullaniciAdi.text = post.kullaniciAdi
        recyclerCommentText.text = post.comment
        Picasso.get().load(post.imageUrl).into(recyclerImageView)
        feedPostTarih.text = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(post.time)
        if (position == 0) {
            border.visibility = View.GONE
        } else {
            border.visibility = View.VISIBLE
        }
        imageButton.setOnClickListener { view ->
            PopupMenu(holder.itemView.context, view).apply {
                menuInflater.inflate(R.menu.menu_post_options, menu)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_edit -> {
                            onEditClickListener?.invoke(post, position)
                            true
                        }
                        R.id.menu_delete -> {
                            onDeleteClickListener?.invoke(post, position)
                            true
                        }
                        else -> false
                    }
                }
            }.show()
        }
    }
}