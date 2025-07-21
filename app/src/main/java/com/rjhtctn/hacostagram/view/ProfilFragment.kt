package com.rjhtctn.hacostagram.view

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rjhtctn.hacostagram.R
import com.rjhtctn.hacostagram.adapter.ProfilPostAdapter
import com.rjhtctn.hacostagram.databinding.FragmentProfilBinding
import com.rjhtctn.hacostagram.model.Posts
import java.util.Date

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val firestore = Firebase.firestore

    private val postList : ArrayList<Posts> = arrayListOf()
    private var adapter: ProfilPostAdapter? = null
    private lateinit var rootNav: NavController
    private lateinit var logoutOpts: NavOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        rootNav = requireActivity().findNavController(R.id.fragmentContainerView)

        logoutOpts = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()

        binding.profilMenuButon.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                        binding.drawerLayout.closeDrawer(GravityCompat.END)
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            })

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_detay -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.END)
                }
                R.id.menu_cikis -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.END)
                    auth.signOut()
                    rootNav.navigate(R.id.girisFragment, null, logoutOpts)
                }
                R.id.menu_sifre_degistir -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.END)
                    rootNav.navigate(R.id.sifreDegistirFragment, null)
                }
                R.id.menu_hesap_sil -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.END)
                    rootNav.navigate(R.id.kayitSilFragment, null)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        adapter = ProfilPostAdapter(postList).apply {
            onEditClickListener = { post, pos ->
                guncelleYorum(post,pos)
            }
            onDeleteClickListener = { post, pos ->
                silPost(post)
            }
        }

        binding.recyclerViewPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPosts.adapter = this@ProfilFragment.adapter

        yukleKullaniciBilgileri()
        yukleGonderiler()
    }

    private fun guncelleYorum(post: Posts, pos: Int) {
        val eskiYorum = postList[pos].comment

        val input = EditText(requireContext()).apply { setText(eskiYorum) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yorumu Düzenle")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val yeni = input.text.toString()
                firestore.collection("posts").document(post.id)
                    .update("comment", yeni)
                    .addOnSuccessListener {
                        postList[pos] = postList[pos].copy(comment = yeni)
                        adapter?.notifyItemChanged(pos)
                        toast("Yorum güncellendi")
                    }
                    .addOnFailureListener { e -> toast(e.localizedMessage ?: "Hata") }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun silPost(post: Posts) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Gönderiyi Sil")
            .setMessage("Bu gönderi kalıcı olarak silinecek. Emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                firestore.collection("posts").document(post.id)
                    .delete()
                    .addOnSuccessListener {
                        toast("Gönderi silindi")
                    }
                    .addOnFailureListener { e -> toast(e.localizedMessage ?: "Hata") }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun yukleKullaniciBilgileri() {
        val uid = auth.currentUser?.uid
        if (uid == null) return

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val username = doc.getString("kullaniciAdi") ?: ""
                    val isim = doc.getString("isim") ?: ""
                    val soyisim = doc.getString("soyisim") ?: ""

                    binding.profilKullaniciAdi.text = username
                    binding.profilIsimSoyisim.text = listOf(isim, soyisim)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                }
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun yukleGonderiler() {
        val uid = auth.currentUser?.uid ?: return
        Firebase.firestore.collection("posts")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING).addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(requireContext(),error.localizedMessage,Toast.LENGTH_LONG).show()
                } else {
                    postList.clear()
                    if (value != null) {
                        if (!value.isEmpty) {
                            postList.clear()
                            val documents = value.documents
                            for (document in documents) {
                                val id = document.id
                                val comment = document.getString("comment") ?: ""
                                val userName = document.getString("kullaniciAdi") ?: ""
                                val imageUrl = document.getString("imageUrl") ?: ""
                                val time = document.getTimestamp("createdAt")?.toDate() ?: Date()
                                val post = Posts(id,userName, comment, imageUrl, time)
                                postList.add(post)
                            }
                            adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }

    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}