package com.rjhtctn.hacostagram.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rjhtctn.hacostagram.R
import com.rjhtctn.hacostagram.databinding.RecyclerRowFeedBinding
import com.rjhtctn.hacostagram.model.Posts
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class PostAdapter(private val postList : ArrayList<Posts>) : RecyclerView.Adapter<PostAdapter.PostHolder>() {
    class PostHolder(val binding : RecyclerRowFeedBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val binding = RecyclerRowFeedBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PostHolder(binding)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) = with(holder.binding){
        val post = postList[position]
        post.profilePhotoUrl
            .takeIf { !it.isNullOrBlank() }
            ?.let { url ->
                Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(profilResmi)
            } ?: profilResmi.setImageResource(R.drawable.ic_profile)
        feedKullaniciAdi.text = post.kullaniciAdi
        recyclerCommentText.text = post.comment
        Picasso.get().load(post.imageUrl).into(recyclerImageView)
        feedPostTarih.text = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(post.time)
        if (position == 0) {
            border.visibility = View.GONE
        } else {
            border.visibility = View.VISIBLE
        }
    }
}