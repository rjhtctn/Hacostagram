package com.rjhtctn.hacostagram.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rjhtctn.hacostagram.BuildConfig
import com.rjhtctn.hacostagram.R
import com.rjhtctn.hacostagram.adapter.ProfilPostAdapter
import com.rjhtctn.hacostagram.databinding.FragmentProfilBinding
import com.rjhtctn.hacostagram.model.Posts
import com.rjhtctn.hacostagram.util.FeedEventsBus
import com.rjhtctn.hacostagram.viewmodel.ProfilViewModel
import com.squareup.picasso.Picasso

class ProfilFragment : Fragment() {

    private var _b: FragmentProfilBinding? = null
    private val b get() = _b!!
    private val vm: ProfilViewModel by viewModels()

    private val auth = FirebaseAuth.getInstance()
    private val db   = Firebase.firestore

    private val adapter = ProfilPostAdapter()

    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var photoPicker: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var safPicker:   ActivityResultLauncher<Array<String>>
    private var secilenGorsel: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); registerLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, s: Bundle?
    ): View {
        _b = FragmentProfilBinding.inflate(inflater, container, false)
        return b.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        b.profilMenuButon.setOnClickListener { b.drawerLayout.openDrawer(GravityCompat.END) }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (b.drawerLayout.isDrawerOpen(GravityCompat.END))
                        b.drawerLayout.closeDrawer(GravityCompat.END)
                    else { isEnabled = false; requireActivity().onBackPressed() }
                }
            })

        b.navigationView.setNavigationItemSelectedListener { item ->
            b.drawerLayout.closeDrawer(GravityCompat.END)
            when (item.itemId) {

                R.id.menu_detay ->
                    runCatching {
                        findNavController()
                            .navigate(R.id.action_profilFragment_to_hesapDetayFragment)
                    }.onFailure { toast("Navigasyon hatası: ${it.message}") }

                R.id.menu_sifre_degistir ->
                    runCatching {
                        findNavController()
                            .navigate(R.id.action_profilFragment_to_sifreDegistirFragment)
                    }.onFailure { toast("Navigasyon hatası: ${it.message}") }

                R.id.menu_hesap_sil ->
                    runCatching {
                        findNavController()
                            .navigate(R.id.action_profilFragment_to_kayitSilFragment)
                    }.onFailure { toast("Navigasyon hatası: ${it.message}") }

                R.id.menu_cikis -> {
                    auth.signOut()
                    requireActivity()
                        .findNavController(R.id.fragmentContainerView)
                        .navigate(
                            R.id.action_global_girisFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build()
                        )
                }
            }
            true
        }

        b.recyclerViewPosts.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerViewPosts.adapter = adapter
        adapter.onEditClickListener   = ::guncelleYorum
        adapter.onDeleteClickListener = { post, _ -> silPost(post) }

        vm.postsLive.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list) {
                adapter.notifyDataSetChanged()
            }
        }
        vm.userLive.observe(viewLifecycleOwner)  { uiGuncelle(it) }

        vm.init(auth.currentUser?.uid)

        b.profilResmi.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("İşlem Seçin")
                .setPositiveButton("Güncelle") { _, _ -> gorselSec() }
                .setNegativeButton("Sil") { _, _ ->
                    val kullanici = b.profilKullaniciAdi.text.toString()
                    db.collection("usersPublic").document(kullanici)
                        .update("profilePhoto", "")
                        .addOnSuccessListener {
                            toast("Profil fotoğrafı silindi.")
                            FeedEventsBus.publish(
                                FeedEventsBus.Event.ProfilePhotoChanged(kullanici, "")
                            )
                        }
                }
                .setNeutralButton("İptal", null)
                .show()
        }
    }
    private fun uiGuncelle(u: ProfilViewModel.UserPublic) {
        b.profilKullaniciAdi.text = vm.cachedUsername
        b.profilIsimSoyisim.text  = "${u.isim} ${u.soyisim}".trim()
        if (u.photoUrl.isNotBlank())
            Picasso.get().load(u.photoUrl)
                .noFade()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(b.profilResmi)
        else b.profilResmi.setImageResource(R.drawable.ic_profile)
    }

    private fun guncelleYorum(post: Posts, pos: Int) {
        val input = EditText(requireContext()).apply { setText(post.comment) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yorumu Düzenle")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                db.collection("posts").document(post.id)
                    .update("comment", input.text.toString())
                    .addOnSuccessListener {
                        toast("Yorum güncellendi")
                        FeedEventsBus.publish(
                            FeedEventsBus.Event.CommentUpdated(post.id)
                        )
                    }
                    .addOnFailureListener { toast(it.message ?: "Hata") }
            }
            .setNegativeButton("İptal", null).show()
    }

    private fun silPost(post: Posts) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Gönderiyi Sil")
            .setMessage("Bu gönderi kalıcı olarak silinecek. Emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                db.collection("posts").document(post.id).delete()
                    .addOnSuccessListener {
                        toast("Gönderi silindi")
                        FeedEventsBus.publish(
                            FeedEventsBus.Event.PostDeleted(post.id)
                        )
                    }
                    .addOnFailureListener { toast(it.message ?: "Hata") }
            }
            .setNegativeButton("İptal", null).show()
    }

    private fun gorselSec() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), perm)
            == PackageManager.PERMISSION_GRANTED) {
            openSelector()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), perm))
                toast("Fotoğraf seçmek için izin vermelisiniz.")
            permissionLauncher.launch(perm)
        }
    }

    private fun openSelector() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        else safPicker.launch(arrayOf("image/*"))
    }

    private fun handleSelected(uri: Uri) {
        secilenGorsel = uri
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Profil fotoğrafı yüklensin mi?")
            .setPositiveButton("Evet") { _, _ -> uploadProfilFoto(uri) }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun uploadProfilFoto(uri: Uri) {
        MediaManager.get().upload(uri)
            .unsigned(BuildConfig.CLOUD_PRESET)
            .option("resource_type", "image")
            .callback(object : UploadCallback {
                override fun onStart(reqId: String?) {}
                override fun onProgress(reqId: String?, total: Long, sent: Long) {}
                override fun onSuccess(reqId: String?, data: MutableMap<Any?, Any?>?) {
                    val secure = data?.get("secure_url") as? String ?: return
                    db.collection("usersPublic")
                        .document(vm.cachedUsername!!)
                        .update("profilePhoto", secure)
                        .addOnSuccessListener {
                            toast("Profil fotoğrafı yüklendi!")
                            FeedEventsBus.publish(
                                FeedEventsBus.Event.ProfilePhotoChanged(vm.cachedUsername!!, secure)
                            )
                        }
                }
                override fun onError(reqId: String?, err: ErrorInfo?) {
                    toast("Yükleme hatası: ${err?.description}")
                }
                override fun onReschedule(reqId: String?, err: ErrorInfo?) {}
            }).dispatch()
    }

    private fun registerLaunchers() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { if (it) openSelector() else toast("İzin verilmedi.") }

        photoPicker = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri -> uri?.let { handleSelected(it) } }

        safPicker = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri -> uri?.let { handleSelected(it) } }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView(); _b = null
    }
}
