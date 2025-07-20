package com.rjhtctn.hacostagram.view

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rjhtctn.hacostagram.R
import com.rjhtctn.hacostagram.databinding.FragmentKayitBinding

class KayitFragment : Fragment() {
    private var _binding: FragmentKayitBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var isim: String
    private lateinit var soyisim: String
    private lateinit var kullaniciAdi: String
    private lateinit var email: String
    private lateinit var uid: String

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            kayitButon.setOnClickListener { kayitOl() }
            kayitVisibility.setOnClickListener { sifreGoster() }
            kayittanGiriseButon.setOnClickListener {
                view.findNavController()
                    .navigate(KayitFragmentDirections.actionKayitFragmentToGirisFragment())
            }
            binding.kayitEmail.doAfterTextChanged { text ->
                if (text != null && !Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
                    binding.kayitEmail.error = "Geçerli e‑posta formatı"
                }
            }
        }
    }

    private fun sifreGoster() = with(binding) {
        val isHidden = kayitSifre1.transformationMethod is PasswordTransformationMethod

        listOf(kayitSifre1, kayitSifre2).forEach { field ->
            field.transformationMethod =
                if (isHidden) null
                else PasswordTransformationMethod.getInstance()
            field.setSelection(field.text.length)
        }

        kayitVisibility.setImageResource(
            if (isHidden)
                R.drawable.visibility
            else
                R.drawable.visibility_off
        )
    }

    private fun kayitOl() = with(binding){
        email = kayitEmail.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Geçerli bir e‑posta adresi girin!")
            kayitButon.isEnabled = true
            return@with
        }
        kayitButon.isEnabled = false
        isim = kayitIsim.text.toString().trim()
        soyisim = kayitSoyisim.text.toString().trim()
        kullaniciAdi = kayitKullaniciAdi.text.toString().trim()
        val sifre1 = kayitSifre1.text.toString()
        val sifre2 = kayitSifre2.text.toString()

        if (listOf(isim, soyisim, kullaniciAdi, email, sifre1, sifre2).any {it.isEmpty()}) {
            toast("Boş Alan Bırakmayın!")
            return@with
        }
        if (sifre1 != sifre2) {
            toast("Şifreler Uyuşmuyor!")
            return@with
        }
        Firebase.firestore.collection("users").whereEqualTo("kullaniciAdi", kullaniciAdi)
            .get().addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    toast("Bu kullanıcı adı zaten alınmış!")
                    kayitButon.isEnabled = true
                } else {
                    auth.createUserWithEmailAndPassword(email,sifre1).addOnCompleteListener{ task ->
                        if (!task.isSuccessful) {
                            toast(task.exception?.localizedMessage ?: "Auth hatası")
                            return@addOnCompleteListener
                        }
                        val uuid = task.result?.user?.uid
                        if(uuid == null) {
                            auth.currentUser?.delete()
                            toast("Kayıt başarısız, lütfen tekrar deneyin!")
                            return@addOnCompleteListener
                        }
                        this@KayitFragment.uid = uuid
                        firestoreKaydet()
                    }.addOnFailureListener { e ->
                        toast("Auth Hatası: ${e.localizedMessage}")
                        kayitButon.isEnabled = true
                    }
                }
            }.addOnFailureListener { e ->
                toast("Kullanıcı adı kontrolü başarısız: ${e.localizedMessage}")
                kayitButon.isEnabled = true
            }
    }
    private fun firestoreKaydet() {
        val user = hashMapOf(
            "isVerified" to "false",
            "isim"     to isim,
            "soyisim"  to soyisim,
            "kullaniciAdi"   to kullaniciAdi,
            "email" to email,
            "userId"    to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        com.google.firebase.Firebase.firestore.collection("users")
            .document(uid)
            .set(user)
            .addOnSuccessListener {
                toast("Kayıt Başarılı!")
                findNavController().navigate(KayitFragmentDirections.actionKayitFragmentToGirisFragment())
            }.addOnFailureListener { e ->
                auth.currentUser?.delete()
                toast("Firestore Hatası: ${e.localizedMessage}")
            }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}