    package com.rjhtctn.hacostagram.adapter

    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import androidx.recyclerview.widget.DiffUtil
    import androidx.recyclerview.widget.ListAdapter
    import androidx.recyclerview.widget.RecyclerView
    import com.rjhtctn.hacostagram.R
    import com.rjhtctn.hacostagram.databinding.RecyclerRowFeedBinding
    import com.rjhtctn.hacostagram.model.Posts
    import com.squareup.picasso.Picasso
    import java.text.SimpleDateFormat
    import java.util.Locale

    class PostAdapter :
        ListAdapter<Posts, PostAdapter.PostHolder>(DIFF) {
        var onUserClickListener: ((String) -> Unit)? = null

        companion object {
            const val PAYLOAD_PP_SILINDI = "PAYLOAD_PP_SILINDI"
            private val DIFF = object : DiffUtil.ItemCallback<Posts>() {
                override fun areItemsTheSame(old: Posts, new: Posts) =
                    old.imageUrl == new.imageUrl && old.time == new.time

                override fun areContentsTheSame(old: Posts, new: Posts) = old == new
            }
        }

        inner class PostHolder(val binding: RecyclerRowFeedBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
            val b = RecyclerRowFeedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
            return PostHolder(b)
        }

        override fun onBindViewHolder(
            holder: PostHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.isEmpty()) {
                onBindViewHolder(holder, position)
            } else {
                payloads.forEach { payload ->
                    if (payload == PAYLOAD_PP_SILINDI) {
                        holder.binding.profilResmi
                            .setImageResource(R.drawable.ic_profile_org)
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: PostHolder, position: Int) {
            val post = getItem(position)
            with(holder.binding) {
                post.profilePhotoUrl
                    ?.takeIf { it.isNotBlank() }
                    ?.let { url ->
                        Picasso.get()
                            .load(url)
                            .noFade()
                            .placeholder(R.drawable.ic_profile_org)
                            .error(R.drawable.ic_profile_org)
                            .into(profilResmi)
                    } ?: profilResmi.setImageResource(R.drawable.ic_profile_org)

                feedKullaniciAdi.text   = post.kullaniciAdi
                recyclerCommentText.text = post.comment
                feedKullanici.setOnClickListener {
                    onUserClickListener?.invoke(post.kullaniciAdi)
                }
                Picasso.get().load(post.imageUrl).noFade().into(recyclerImageView)
                feedPostTarih.text =
                    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                        .format(post.time)
                border.visibility = if (position == 0) View.GONE else View.VISIBLE
            }
        }
    }