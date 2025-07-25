package com.rjhtctn.hacostagram.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rjhtctn.hacostagram.R
import com.rjhtctn.hacostagram.databinding.FragmentHesapDetayBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HesapDetayFragment : Fragment() {

    private var _binding: FragmentHesapDetayBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private var isim: String = ""
    private var soyisim: String = ""
    private var username: String = ""
    private var email: String = ""
    private var postSayi: String = "0"
    private var time: Date = Date()
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHesapDetayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (auth.currentUser == null) {
                    Toast.makeText(requireContext(), "Şifreniz değişti, tekrar giriş yapmalısınız.", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.nav_graph)
                }
            } else {
                Toast.makeText(requireContext(), "Oturum geçersiz, tekrar giriş yapın.", Toast.LENGTH_LONG).show()
                findNavController().navigate(R.id.nav_graph)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            findNavController().navigate(R.id.nav_graph)
            return
        }

        uid = currentUser.uid
        loadUserData()
        binding.backButon.setOnClickListener { findNavController().navigateUp() }
    }

    private fun loadUserData() {
        Firebase.firestore.collection("usersPrivate").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    username = doc.getString("kullaniciAdi") ?: ""
                    email = doc.getString("email") ?: ""
                    time = doc.getTimestamp("createdAt")?.toDate() ?: Date()

                    loadPublicUserData()
                    loadPostCount()
                } else {
                    Toast.makeText(requireContext(), "Kullanıcı bilgileri bulunamadı", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Veri yükleme hatası: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPublicUserData() {
        if (username.isNotEmpty()) {
            Firebase.firestore.collection("usersPublic").document(username)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        isim = doc.getString("isim") ?: ""
                        soyisim = doc.getString("soyisim") ?: ""
                    }
                    updateUI()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Profil bilgileri yüklenemedi: ${exception.message}", Toast.LENGTH_SHORT).show()
                    updateUI()
                }
        }
    }

    private fun loadPostCount() {
        if (username.isNotEmpty()) {
            Firebase.firestore.collection("posts")
                .whereEqualTo("kullaniciAdi", username)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener { snapshot ->
                    postSayi = snapshot.count.toString()
                    updateUI()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Post sayısı yüklenemedi: ${exception.message}", Toast.LENGTH_SHORT).show()
                    postSayi = "0"
                    updateUI()
                }
        }
    }

    private fun updateUI() {
        if (_binding != null) {
            binding.detayIsim.text = isim
            binding.detaySoyisim.text = soyisim
            binding.detayKullaniciAdi.text = username
            binding.detayEmail.text = email
            binding.detayPostSayisi.text = postSayi

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.detayCreatedAt.text = dateFormat.format(time)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}