package com.rjhtctn.hacostagram.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.rjhtctn.hacostagram.R
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

        adapter.onUserClickListener = { username ->
            findNavController().navigate(R.id.profilFragment, bundleOf("targetUsername" to username))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    override fun onResume() {
        super.onResume()
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (auth.currentUser == null) {
                    Toast.makeText(requireContext(), "Şifreniz değişti, tekrar giriş yapmalısınız.", Toast.LENGTH_LONG).show()
                    requireActivity()
                        .findNavController(R.id.fragmentContainerView)
                        .navigate(
                            R.id.action_global_girisFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build()
                        )                }
            } else {
                Toast.makeText(requireContext(), "Oturum geçersiz, tekrar giriş yapın.", Toast.LENGTH_LONG).show()
                requireActivity()
                    .findNavController(R.id.fragmentContainerView)
                    .navigate(
                        R.id.action_global_girisFragment,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build()
                    )            }
        }
    }
}