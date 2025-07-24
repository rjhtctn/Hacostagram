package com.rjhtctn.hacostagram.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rjhtctn.hacostagram.adapter.PostAdapter
import com.rjhtctn.hacostagram.databinding.FragmentFeedBinding
import com.rjhtctn.hacostagram.util.FeedEventsBus
import com.rjhtctn.hacostagram.viewmodel.feedViewModel

class FeedFragment : Fragment() {

    private var _b: FragmentFeedBinding? = null
    private val b get() = _b!!

    private val vm: feedViewModel by viewModels()
    private val adapter = PostAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, s: Bundle?
    ): View {
        _b = FragmentFeedBinding.inflate(inflater, container, false)
        return b.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        b.feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        b.feedRecyclerView.adapter = adapter

        vm.postsLive.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list) {
                adapter.notifyDataSetChanged()
            }
        }

        vm.startListening()
        FeedEventsBus.live.observe(viewLifecycleOwner) { event ->
            if (event is FeedEventsBus.Event.ProfilePhotoChanged) {
                val deletedUsername = event.username
                adapter.currentList.forEachIndexed { index, post ->
                    if (post.kullaniciAdi == deletedUsername) {
                        adapter.notifyItemChanged(
                            index,
                            PostAdapter.PAYLOAD_PP_SILINDI
                        )
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
