package com.rjhtctn.hacostagram.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rjhtctn.hacostagram.databinding.FeedRecyclerRowBinding
import com.rjhtctn.hacostagram.model.Posts
import com.squareup.picasso.Picasso

class PostAdapter(private val postList : ArrayList<Posts>) : RecyclerView.Adapter<PostAdapter.PostHolder>() {

    class PostHolder(val binding : FeedRecyclerRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val binding = FeedRecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PostHolder(binding)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        holder.binding.recyclerEmailText.text = postList[position].email
        holder.binding.recyclerCommentText.text = postList[position].comment
        Picasso.get().load(postList[position].imageUrl).into(holder.binding.recyclerImageView)
    }
}