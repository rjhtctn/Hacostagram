package com.rjhtctn.hacostagram.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import com.rjhtctn.hacostagram.databinding.FragmentGirisBinding

class GirisFragment : Fragment() {
    private var _binding: FragmentGirisBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGirisBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth.currentUser?.reload()
        auth.currentUser?.let { user ->
            if (user.isEmailVerified) {
                val action = GirisFragmentDirections.actionGirisFragmentToHomeFragment()
                view.findNavController().navigate(action)
                return
            }
        }
        binding.girisButton.setOnClickListener { girisYap(it) }
        binding.giristenKayitaButon.setOnClickListener { gecisYap(it) }
    }

    private fun gecisYap(view: View) {
        val action = GirisFragmentDirections.actionGirisFragmentToKayitFragment()
        view.findNavController().navigate(action)
    }

    private fun girisYap(view: View) {
        val identifier = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()

        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "E‑POSTA / KULLANICI ADI VE PAROLA BOŞ BIRAKILAMAZ!", Toast.LENGTH_LONG
            ).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val nameRef = db.collection("usernames").document(identifier)

        nameRef.get(Source.SERVER)
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    val email = snap.getString("email") ?: run {
                        toast("E‑posta kayıtlı değil!"); return@addOnSuccessListener
                    }
                    signInWithEmail(email, password, view)
                } else {
                    val emailRegex =
                        Regex("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$", RegexOption.IGNORE_CASE)
                    if (emailRegex.matches(identifier)) {
                        signInWithEmail(identifier, password, view)
                    } else {
                        toast("Kullanıcı adı bulunamadı ve geçerli e‑posta değil!")
                    }
                }
            }
            .addOnFailureListener { e -> toast(e.localizedMessage ?: "Hata!") }
    }
    private fun signInWithEmail(email: String, password: String, view: View) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { cred ->
                cred.user!!.reload().addOnSuccessListener {
                    if (cred.user!!.isEmailVerified) {
                        GirisFragmentDirections
                            .actionGirisFragmentToHomeFragment()
                            .also { view.findNavController().navigate(it) }

                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(cred.user!!.uid)
                            .update("emailVerified", true)
                    } else {
                        cred.user!!.sendEmailVerification()
                        auth.signOut()
                        toast("E‑posta doğrulanmamış! Mailinizi onaylayın.")
                    }
                }
            }
            .addOnFailureListener { e -> toast(e.localizedMessage ?: "Giriş hatası!") }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}