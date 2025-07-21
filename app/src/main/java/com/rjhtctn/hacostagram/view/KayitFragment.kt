package com.rjhtctn.hacostagram.view

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import com.rjhtctn.hacostagram.R
import com.rjhtctn.hacostagram.databinding.FragmentKayitBinding
import java.util.regex.Pattern

class KayitFragment : Fragment() {
    private var _binding: FragmentKayitBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var isim: String
    private lateinit var soyisim: String
    private lateinit var kullaniciAdi: String
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKayitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?): Unit = with(binding) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    view.findNavController()
                        .navigate(KayitFragmentDirections.actionKayitFragmentToGirisFragment())
                }
            })

        kayitButon.setOnClickListener { kayitOl() }
        kayitVisibility.setOnClickListener { sifreGoster() }
        kayittanGiriseButon.setOnClickListener {
            view.findNavController()
                .navigate(KayitFragmentDirections.actionKayitFragmentToGirisFragment())
        }

        kayitEmail.doAfterTextChanged { text ->
            if (text != null && !Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
                kayitEmail.error = "Geçersiz e‑posta formatı"
            }
        }
    }
    private fun sifreGoster() = with(binding) {
        val isHidden = kayitSifre1.transformationMethod is PasswordTransformationMethod
        listOf(kayitSifre1, kayitSifre2).forEach { field ->
            field.transformationMethod =
                if (isHidden) null else PasswordTransformationMethod.getInstance()
            field.setSelection(field.text.length)
        }
        kayitVisibility.setImageResource(
            if (isHidden) R.drawable.ic_visibility
            else R.drawable.ic_visibility_off
        )
    }

    private fun kayitOl() = with(binding) {
        email = kayitEmail.text.toString().trim()
        isim = kayitIsim.text.toString().trim()
        soyisim = kayitSoyisim.text.toString().trim()
        kullaniciAdi = kayitKullaniciAdi.text.toString().trim()
        val sifre1 = kayitSifre1.text.toString()
        val sifre2 = kayitSifre2.text.toString()
        if (listOf(isim, soyisim, kullaniciAdi, email, sifre1, sifre2).any { it.isEmpty() }) {
            toast("Boş Alan Bırakmayın!")
            return@with
        }

        val idRegex: Pattern = Pattern.compile("^[a-zA-Z0-9._-]{3,30}$")
        if (!idRegex.matcher(kullaniciAdi).matches()) {
            toast("Kullanıcı adı sadece harf, sayı, . _ - içerebilir (3‑30 karakter)!")
            return@with
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Geçerli bir e‑posta adresi girin!")
            return@with
        }

        if (sifre1 != sifre2) {
            toast("Şifreler Uyuşmuyor!")
            return@with
        }

        setButtonLoading(true)

        val db = FirebaseFirestore.getInstance()
        val nameRef: DocumentReference = db.collection("usernames").document(kullaniciAdi)

        nameRef.get(Source.SERVER).addOnSuccessListener { snap ->
            if (snap.exists()) {
                toast("Bu kullanıcı adı alınmış!")
                setButtonLoading(false)
                return@addOnSuccessListener
            }
            auth.createUserWithEmailAndPassword(email, sifre1)
                .addOnSuccessListener { cred ->
                    val user = cred.user!!
                    val uid = user.uid
                    db.runTransaction { tx ->
                        if (tx.get(nameRef).exists()) {
                            throw FirebaseFirestoreException(
                                "Kullanıcı adı dolu",
                                FirebaseFirestoreException.Code.ABORTED
                            )
                        }

                        tx.set(nameRef, mapOf("uid" to uid, "email" to email))

                        tx.set(
                            db.collection("users").document(uid),
                            mapOf(
                                "isim" to isim,
                                "soyisim" to soyisim,
                                "kullaniciAdi" to kullaniciAdi,
                                "email" to email,
                                "userId" to uid,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "emailVerified" to false
                            )
                        )
                    }.addOnSuccessListener {
                        user.sendEmailVerification().addOnSuccessListener {
                            Firebase.auth.signOut()
                            toast("Doğrulama E-Postası Gönderildi!")
                            findNavController().navigate(
                                KayitFragmentDirections.actionKayitFragmentToGirisFragment()
                            )
                        }.addOnFailureListener { mailErr ->
                            user.delete()
                            toast("E‑posta gönderilemedi: ${mailErr.localizedMessage}")
                            setButtonLoading(false)
                        }
                    }
                        .addOnFailureListener { e ->
                            user.delete()
                            toast("Kayıt hatası: ${e.localizedMessage}")
                            setButtonLoading(false)
                        }
                }
                .addOnFailureListener { e ->
                    toast("Auth Hatası: ${e.localizedMessage}")
                    setButtonLoading(false)
                }
        }.addOnFailureListener { e ->
            toast("Bağlantı Hatası: ${e.localizedMessage}")
            setButtonLoading(false)
        }
    }
    private fun setButtonLoading(isLoading: Boolean) {
        requireActivity().runOnUiThread {
            binding.kayitButon.isEnabled = !isLoading
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
