package com.rjhtctn.hacostagram.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.rjhtctn.hacostagram.R
import com.rjhtctn.hacostagram.adapter.PostAdapter
import com.rjhtctn.hacostagram.databinding.FragmentFeedBinding
import com.rjhtctn.hacostagram.model.Posts

class FeedFragment : Fragment(), PopupMenu.OnMenuItemClickListener {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var popup: PopupMenu
    private lateinit var auth: FirebaseAuth

    private val postList : ArrayList<Posts> = arrayListOf()

    private var adapter : PostAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater,container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.floatingActionButton.setOnClickListener { menuButton() }
        popup = PopupMenu(requireContext(),binding.floatingActionButton)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.menu,popup.menu)
        popup.setOnMenuItemClickListener(this)
        firestoreVeriAl()
        adapter = PostAdapter(postList)
        binding.feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.feedRecyclerView.adapter = adapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun firestoreVeriAl() {
        Firebase.firestore.collection("posts").orderBy("createdAt" ,Query.Direction.DESCENDING).addSnapshotListener{ value , error ->
            if (error != null) {
                Toast.makeText(requireContext(),error.localizedMessage,Toast.LENGTH_LONG).show()
            }
            else {
                if (value != null) {
                    if (!value.isEmpty) {
                        postList.clear()
                        val documents = value.documents
                        for (document in documents) {
                            val comment = document.get("comment") as String
                            val email = document.get("email") as String
                            val imageUrl = document.get("imageUrl") as String
                            val post = Posts(email, comment, imageUrl)
                            postList.add(post)
                        }
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun menuButton() {
        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}