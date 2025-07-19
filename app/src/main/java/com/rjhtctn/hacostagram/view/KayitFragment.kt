package com.rjhtctn.hacostagram.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.rjhtctn.hacostagram.databinding.FragmentKayitBinding

class KayitFragment : Fragment() {

    private var _binding: FragmentKayitBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKayitBinding.inflate(inflater,container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.kayitButon.setOnClickListener { kayitOl(it) }
    }

    private fun kayitOl(view: View){
        val email = binding.kayitEmail.text.toString()
        val password = binding.kayitSifre1.text.toString()
        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    val action = GirisFragmentDirections.actionKullaniciFragmentToFeedFragment()
                    view.findNavController().navigate(action)
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(),exception.localizedMessage,Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(),"E-POSTA VE PAROLA BOŞ BIRAKILAMAZ!", Toast.LENGTH_LONG).show()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}