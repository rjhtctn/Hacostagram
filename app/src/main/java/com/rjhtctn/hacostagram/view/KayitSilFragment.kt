package com.rjhtctn.hacostagram.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rjhtctn.hacostagram.databinding.FragmentKayitSilBinding
import com.rjhtctn.hacostagram.util.FeedEventsBus
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class KayitSilFragment : Fragment() {

    private var _b: FragmentKayitSilBinding? = null
    private val b get() = _b!!

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, s: Bundle?
    ): View {
        _b = FragmentKayitSilBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        b.backButon.setOnClickListener { findNavController().navigateUp() }

        b.kayitSilButon.setOnClickListener { confirmAndDelete() }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            }
        )
    }

    private fun confirmAndDelete() {
        val email = b.kayitSilEmail.text.toString().trim()
        val pass1 = b.kayitSilSifre.text.toString()
        val pass2 = b.kayitSilSifre2.text.toString()
        val usern = b.kayitSilKullaniciAdi.text.toString().trim()

        when {
            email.isBlank() || pass1.isBlank() || pass2.isBlank() || usern.isBlank() ->
                return toast("Lütfen tüm alanları doldurun.")
            pass1 != pass2 ->
                return toast("Şifreler eşleşmiyor.")

        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hesabınızı kalıcı olarak silmek üzeresiniz!")
            .setMessage("Bu işlem geri alınamaz. Devam etmek istiyor musunuz?")
            .setPositiveButton("Evet") { _, _ -> performDeletion(email, pass1, usern) }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun performDeletion(email: String, pwd: String, username: String) {

        val user = auth.currentUser ?: return toast("Oturum kapalı.")
        val cred = EmailAuthProvider.getCredential(email, pwd)

        setButtonLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) { user.reauthenticate(cred).await() }

                val posts = withContext(Dispatchers.IO) {
                    db.collection("posts")
                        .whereEqualTo("kullaniciAdi", username)
                        .get().await()
                }
                posts.forEach { doc ->
                    FeedEventsBus.publish(FeedEventsBus.Event.PostDeleted(doc.id))
                    doc.reference.delete()
                }

                val batch = db.batch()
                batch.delete(db.collection("usersPublic").document(username))
                batch.delete(db.collection("usersPrivate").document(user.uid))
                withContext(Dispatchers.IO) { batch.commit().await() }

                withContext(Dispatchers.IO) { user.delete().await() }

                toast("Hesap ve tüm veriler silindi!")

                auth.signOut()

            } catch (e: Exception) {
                toast("Silme hatası: ${e.localizedMessage}")
            } finally {
                setButtonLoading(false)
            }
        }
    }

    private fun setButtonLoading(isLoading: Boolean) {
        requireActivity().runOnUiThread {
            b.kayitSilButon.isEnabled = !isLoading
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView(); _b = null
    }
}
