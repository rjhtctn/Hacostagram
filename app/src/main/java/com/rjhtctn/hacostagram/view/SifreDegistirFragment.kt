package com.rjhtctn.hacostagram.view

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.rjhtctn.hacostagram.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.rjhtctn.hacostagram.databinding.FragmentSifreDegistirBinding

class SifreDegistirFragment : Fragment() {
    private var _binding: FragmentSifreDegistirBinding? = null
    private val binding get()  = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSifreDegistirBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sifreDegistirButon.setOnClickListener { sifreDegistir() }
        binding.sifreDegistirVisibility1.setOnClickListener { sifreGoster1() }
        binding.sifreDegistirVisibility2.setOnClickListener { sifreGoster2() }
        binding.backButon.setOnClickListener { findNavController().navigateUp() }
    }

    private fun sifreDegistir() {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: return toast("Oturum açılmamış!")
        val oldPassword = binding.sifreDegistirMevcutSifre.text.toString()
        val newPassword1 = binding.sifreDegistirYeniSifre1.text.toString()
        val newPassword2 = binding.sifreDegistirYeniSifre2.text.toString()
        setButtonLoading(true)
        if (newPassword1 == newPassword2) {
            user.reauthenticate(EmailAuthProvider.getCredential(email, oldPassword))
                .addOnSuccessListener {
                    user.updatePassword(newPassword1)
                        .addOnSuccessListener {
                            toast("Şifre değiştirildi!")
                            findNavController().navigate(R.id.action_sifreDegistirFragment_to_profilFragment,null,
                                NavOptions.Builder().setPopUpTo(R.id.feedFragment,false).build())
                        }.addOnFailureListener { e ->
                            toast("Şifre güncellenirken hata: ${e.localizedMessage}")
                            setButtonLoading(false)
                        }
                }.addOnFailureListener { toast("Mevcut şifre yanlış!")
                    setButtonLoading(false)
                }
        } else {
            toast("Yeni şifreler uyuşmuyor!")
            setButtonLoading(false)
        }

    }

    private fun sifreGoster1() = with(binding) {
        val isHidden = sifreDegistirMevcutSifre.transformationMethod is PasswordTransformationMethod
        listOf(sifreDegistirMevcutSifre).forEach { field ->
            field.transformationMethod =
                if (isHidden) null else PasswordTransformationMethod.getInstance()
            field.setSelection(field.text.length)
        }
        sifreDegistirVisibility1.setImageResource(
            if (isHidden) R.drawable.ic_visibility
            else R.drawable.ic_visibility_off
        )
    }

    private fun sifreGoster2() = with(binding) {
        val isHidden = sifreDegistirYeniSifre1.transformationMethod is PasswordTransformationMethod
        listOf(sifreDegistirYeniSifre1, sifreDegistirYeniSifre2).forEach { field ->
            field.transformationMethod =
                if (isHidden) null else PasswordTransformationMethod.getInstance()
            field.setSelection(field.text.length)
        }
        sifreDegistirVisibility2.setImageResource(
            if (isHidden) R.drawable.ic_visibility
            else R.drawable.ic_visibility_off
        )
    }

    private fun setButtonLoading(isLoading: Boolean) {
        requireActivity().runOnUiThread {
            binding.sifreDegistirButon.isEnabled = !isLoading
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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