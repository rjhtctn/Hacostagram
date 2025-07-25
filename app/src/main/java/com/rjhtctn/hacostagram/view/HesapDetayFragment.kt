package com.rjhtctn.hacostagram.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.rjhtctn.hacostagram.R

class HesapDetayFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hesap_detay, container, false)
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

}