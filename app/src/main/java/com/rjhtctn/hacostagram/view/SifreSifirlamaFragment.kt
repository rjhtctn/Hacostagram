package com.rjhtctn.hacostagram.view

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rjhtctn.hacostagram.databinding.FragmentSifreSifirlamaBinding

class SifreSifirlamaFragment : Fragment() {

    private var _b: FragmentSifreSifirlamaBinding? = null
    private val b get() = _b!!

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()
        _b = FragmentSifreSifirlamaBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    view.findNavController()
                        .navigate(SifreSifirlamaFragmentDirections
                            .actionSifreSifirlamaFragmentToGirisFragment())
                }
            })

        b.sifreSifirlaButon.setOnClickListener {
            attemptPasswordReset()
        }
        b.backButon.setOnClickListener {
            view.findNavController().popBackStack()
        }
        b.sifirlamadanGiriseButon.setOnClickListener {
            view.findNavController().popBackStack()
        }
    }

    private fun attemptPasswordReset() {
        val username = b.sifreSifirlaKullaniciAdi.text.toString().trim()
        val emailInput = b.sifreSifirlaEmail.text.toString().trim()

        if (username.isEmpty()) {
            b.sifreSifirlaKullaniciAdi.error = "Kullanıcı adı boş olamaz"
            b.sifreSifirlaKullaniciAdi.requestFocus()
            return
        }
        if (emailInput.isEmpty()) {
            b.sifreSifirlaEmail.error = "E‑posta boş olamaz"
            b.sifreSifirlaEmail.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            b.sifreSifirlaEmail.error = "Geçerli bir e‑posta girin"
            b.sifreSifirlaButon.requestFocus()
            return
        }

        setButtonLoading(true)

        db.collection("usernames").document(username)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val reelEmail = doc.getString("email") ?: ""
                    if (reelEmail == emailInput) {
                        sendResetEmail(emailInput)
                    } else {
                        toast("Kullanıcı adı veya e‑posta yanlış!")
                        setButtonLoading(false)
                    }
                } else {
                    toast("Kullanıcı bulunamadı!")
                    setButtonLoading(false)
                }
            }
            .addOnFailureListener {
                toast("Kullanıcı adı veya e‑posta yanlış!")
                setButtonLoading(false)
            }
    }

    private fun sendResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                toast("Şifre sıfırlama bağlantısı e‑postanıza gönderildi.")
                view?.findNavController()?.popBackStack()
            }
            .addOnFailureListener {
                toast("E‑posta gönderilemedi")
                setButtonLoading(false)
            }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    private fun setButtonLoading(isLoading: Boolean) {
        b.sifreSifirlaButon.isEnabled = !isLoading
        b.sifreSifirlaButon.text = if (isLoading) "Gönderiliyor..." else "Şifreyi Sıfırla"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
