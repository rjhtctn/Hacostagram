package com.rjhtctn.hacostagram.view

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import com.rjhtctn.hacostagram.R
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
        binding.girisVisibility.setOnClickListener { sifreGoster() }
        binding.girisButton.setOnClickListener { girisYap(it) }
        binding.giristenKayitaButon.setOnClickListener { gecisYap(it) }
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().moveTaskToBack(true)
                }
            })
        binding.giristenSifirlamaButon.setOnClickListener {
            findNavController().navigate(GirisFragmentDirections.actionGirisFragmentToSifreSifirlamaFragment())
        }
    }

    private fun sifreGoster() = with(binding) {
        val isHidden = passwordEditText.transformationMethod is PasswordTransformationMethod
        listOf(passwordEditText).forEach { field ->
            field.transformationMethod =
                if (isHidden) null else PasswordTransformationMethod.getInstance()
            field.setSelection(field.text.length)
        }
        girisVisibility.setImageResource(
            if (isHidden) R.drawable.ic_visibility
            else R.drawable.ic_visibility_off
        )
    }

    private fun gecisYap(view: View) {
        val action = GirisFragmentDirections.actionGirisFragmentToKayitFragment()
        view.findNavController().navigate(action)
    }

    private fun girisYap(view: View) {
        val identifier = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()

        if (identifier.isEmpty() || password.isEmpty()) {
            toast("E‑POSTA / KULLANICI ADI VE PAROLA BOŞ BIRAKILAMAZ!")
            return
        }

        setButtonLoading(true)

        val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$", RegexOption.IGNORE_CASE)
        if (emailRegex.matches(identifier)) {
            signInWithEmail(identifier, password, view)
            return
        }

        val db = FirebaseFirestore.getInstance()
        val nameRef = db.collection("usernames").document(identifier)

        nameRef.get(Source.SERVER)
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    val email = snap.getString("email")
                    if (email != null) {
                        signInWithEmail(email, password, view)
                    } else {
                        toast("KULLANICI ADI VEYA ŞİFRE YANLIŞ")
                        setButtonLoading(false)
                    }
                } else {
                    toast("KULLANICI ADI VEYA ŞİFRE YANLIŞ")
                    setButtonLoading(false)

                }
            }.addOnFailureListener { e -> toast(e.localizedMessage ?: "Hata!")
                setButtonLoading(false)
            }
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
                            .collection("usersPrivate")
                            .document(cred.user!!.uid)
                            .update("emailVerified", true)
                    } else {
                        cred.user!!.sendEmailVerification()
                        auth.signOut()
                        setButtonLoading(false)
                        toast("E‑posta doğrulanmamış! Mailinizi onaylayın.")
                    }
                }.addOnFailureListener { toast( "KULLANICI ADI VEYA ŞİFRE YANLIŞ"); setButtonLoading(false) }
            }.addOnFailureListener { e ->
                toast(e.localizedMessage ?: "Giriş hatası!")
                setButtonLoading(false)
            }
    }

    private fun setButtonLoading(isLoading: Boolean) {
        requireActivity().runOnUiThread {
            binding.girisButton.isEnabled = !isLoading
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}