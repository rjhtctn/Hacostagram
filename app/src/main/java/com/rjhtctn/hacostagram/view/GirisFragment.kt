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
        _binding = FragmentGirisBinding.inflate(inflater,container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.girisButton.setOnClickListener { girisYap(it) }
        if (auth.currentUser != null) {
            val action = GirisFragmentDirections.actionKullaniciFragmentToFeedFragment()
            view.findNavController().navigate(action)
        }
    }



    private fun girisYap (view: View) {
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.signInWithEmailAndPassword(email,password).addOnSuccessListener {
                val action = GirisFragmentDirections.actionKullaniciFragmentToFeedFragment()
                view.findNavController().navigate(action)
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(),exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }else {
            Toast.makeText(requireContext(),"E-POSTA VE PAROLA BOŞ BIRAKILAMAZ!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}