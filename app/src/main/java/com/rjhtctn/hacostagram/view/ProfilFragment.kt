package com.rjhtctn.hacostagram.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
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
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rjhtctn.hacostagram.BuildConfig
import com.rjhtctn.hacostagram.R
import com.rjhtctn.hacostagram.adapter.ProfilPostAdapter
import com.rjhtctn.hacostagram.databinding.FragmentProfilBinding
import com.rjhtctn.hacostagram.model.Posts
import com.squareup.picasso.Picasso
import java.util.Date

class ProfilFragment : Fragment() {
    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var photoPicker: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var safPicker: ActivityResultLauncher<Array<String>>
    private var secilenGorsel: Uri? = null
    private lateinit var auth: FirebaseAuth
    private val firestore = Firebase.firestore
    private val postList: ArrayList<Posts> = arrayListOf()
    private var adapter: ProfilPostAdapter? = null
    private var userDataListener: ListenerRegistration? = null
    private var postsListener: ListenerRegistration? = null
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        registerLaunchers()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.profilMenuButon.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                        binding.drawerLayout.closeDrawer(GravityCompat.END)
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            })

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.END)

            when (menuItem.itemId) {
                R.id.menu_detay -> {

                }
                R.id.menu_cikis -> {
                    MaterialAlertDialogBuilder(requireContext()).setTitle("Çıkış Yapmak İstiyor Musunuz?").setPositiveButton("Evet") { _ , _ ->
                        auth.signOut()
                        val parentNavController = requireActivity().findNavController(R.id.fragmentContainerView)
                        val logoutOptions = NavOptions.Builder().setPopUpTo(R.id.home_nav_graph,true)
                            .setPopUpTo(R.id.nav_graph, true)
                            .build()
                        parentNavController.navigate(R.id.girisFragment, null, logoutOptions)
                    }.setNegativeButton("İptal", null).show()
                }
                R.id.menu_sifre_degistir -> {
                    try {
                        findNavController().navigate(R.id.action_profilFragment_to_sifreDegistirFragment)
                    } catch (e: Exception) {
                        toast("Navigasyon hatası: ${e.localizedMessage}")
                    }
                }
                R.id.menu_hesap_sil -> {
                    try {
                        findNavController().navigate(R.id.action_profilFragment_to_kayitSilFragment)
                    } catch (e: Exception) {
                        toast("Navigasyon hatası: ${e.localizedMessage}")
                    }
                }
            }
            true
        }

        adapter = ProfilPostAdapter(postList).apply {
            onEditClickListener = { post, pos ->
                guncelleYorum(post, pos)
            }
            onDeleteClickListener = { post, pos ->
                silPost(post)
            }
        }

        binding.recyclerViewPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPosts.adapter = this@ProfilFragment.adapter
        binding.profilResmi.setOnClickListener { gorselSec(it) }
        yukleKullaniciBilgileri()
        yukleGonderiler()
    }

    private fun gorselSec(view: View) {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), perm)
            == PackageManager.PERMISSION_GRANTED) {
            openSelector()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), perm)) {
                Snackbar.make(view,
                    "Fotoğraf Seçmek İçin İzin Vermeniz Gerekiyor.",
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("Tamam") { permissionLauncher.launch(perm) }
                    .show()
            } else {
                permissionLauncher.launch(perm)
            }
        }
    }

    private fun openSelector() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            else -> {
                safPicker.launch(arrayOf("image/*"))
            }
        }
    }

    private fun handleSelected(uri: Uri) {
        secilenGorsel = uri
        val stream = requireContext().contentResolver.openInputStream(secilenGorsel!!)
            ?: run {toast("Dosya açılamadı"); return}
        val header = ByteArray(3)
        val read = stream.read(header)
        stream.close()
        if (read < 3) {
            toast("Dosya geçerli bir resim değil")
            return
        }
        if (header[0] != 0xFF.toByte() ||
            header[1] != 0xD8.toByte() ||
            header[2] != 0xFF.toByte()) {
            toast("Seçtiğiniz dosya gerçek bir JPEG değil")
            return
        }
        val mime = requireContext().contentResolver.getType(secilenGorsel!!)
        if (mime == null || !mime.startsWith("image/")) {
            toast("Lütfen gerçek bir resim dosyası seçin")
            return
        }
        MaterialAlertDialogBuilder(requireContext()).setTitle("Profil fotoğrafı yüklemek istiyor musunuz?").
            setPositiveButton("Evet") { _,_ ->
                MediaManager.get()
                    .upload(secilenGorsel)
                    .unsigned(BuildConfig.CLOUD_PRESET)
                    .option("resource_type", "image")
                    .callback(object : UploadCallback {
                        override fun onStart(reqId: String?) {}
                        override fun onProgress(reqId: String?, total: Long, sent: Long) {}
                        override fun onSuccess(reqId: String?, data: MutableMap<Any?, Any?>?) {
                            val secure = data?.get("secure_url") as String
                            firestoreKaydet(secure)
                        }
                        override fun onError(reqId: String?, err: ErrorInfo?) {
                            Toast.makeText(requireContext(),
                                "Yükleme hatası: ${err?.description}", Toast.LENGTH_LONG).show()
                        }
                        override fun onReschedule(reqId: String?, err: ErrorInfo?) {}
                    })
                    .dispatch()
            }.setNegativeButton("İptal" , null).show()
    }
    private fun firestoreKaydet(url: String) {
        val db = com.google.firebase.Firebase.firestore
        db.collection("usersPublic").document(username).
        update("profilePhoto", url).addOnSuccessListener {
            toast("Profil fotoğrafı yüklendi!")
        }.addOnFailureListener { e ->
            toast("Hata: ${e.localizedMessage}")
        }
    }

    private fun registerLaunchers() {

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) openSelector() else
                Toast.makeText(requireContext(),
                    "İzin verilmedi.", Toast.LENGTH_LONG).show()
        }

        photoPicker = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri -> uri?.let { handleSelected(it) } }

        safPicker = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri -> uri?.let { handleSelected(it) } }
    }

    private fun guncelleYorum(post: Posts, pos: Int) {
        if (!isAdded || _binding == null) return

        val eskiYorum = postList[pos].comment
        val input = EditText(requireContext()).apply { setText(eskiYorum) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yorumu Düzenle")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val yeni = input.text.toString()
                firestore.collection("posts").document(post.id)
                    .update("comment", yeni)
                    .addOnSuccessListener {
                        if (isAdded && _binding != null) {
                            toast("Yorum güncellendi")
                        }
                    }
                    .addOnFailureListener { e ->
                        toast(e.localizedMessage ?: "Hata")
                    }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun silPost(post: Posts) {
        if (!isAdded || _binding == null) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Gönderiyi Sil")
            .setMessage("Bu gönderi kalıcı olarak silinecek. Emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                firestore.collection("posts").document(post.id)
                    .delete()
                    .addOnSuccessListener {
                        toast("Gönderi silindi")
                        yukleGonderiler()
                    }
                    .addOnFailureListener { e ->
                        toast(e.localizedMessage ?: "Hata")
                    }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun yukleKullaniciBilgileri() {
        val uid = auth.currentUser?.uid ?: return
        userDataListener?.remove()

        firestore.collection("usersPrivate")
            .document(uid)
            .get(Source.CACHE)
            .addOnSuccessListener { doc ->
                val newUsername = doc.getString("kullaniciAdi").orEmpty()
                if (newUsername.isBlank()) {
                    toast("Kullanıcı adı bulunamadı")
                    return@addOnSuccessListener
                }
                username = newUsername

                userDataListener = firestore.collection("usersPublic")
                    .document(username)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            toast("Profil bilgileri yüklenirken hata: ${error.localizedMessage}")
                            return@addSnapshotListener
                        }
                        if (snapshot?.exists() == true && isAdded && _binding != null) {
                            val isim       = snapshot.getString("isim").orEmpty()
                            val soyisim    = snapshot.getString("soyisim").orEmpty()
                            val profilFoto = snapshot.getString("profilePhoto").orEmpty()

                            binding.profilKullaniciAdi.text  = username
                            binding.profilIsimSoyisim.text   = listOf(isim, soyisim)
                                .filter(String::isNotBlank)
                                .joinToString(" ")
                            if (profilFoto.isNotBlank()) {
                                Picasso.get()
                                    .load(profilFoto)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .into(binding.profilResmi)
                            } else {
                                binding.profilResmi.setImageResource(R.drawable.ic_profile)
                            }
                        }
                    }
            }
            .addOnFailureListener { e ->
                toast("Kullanıcı bilgisi alınamadı: ${e.localizedMessage}")
            }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun yukleGonderiler() {
        val uid = auth.currentUser?.uid ?: return
        postsListener?.remove()

        postsListener = firestore.collection("posts")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (!isAdded || _binding == null) {
                    postsListener?.remove()
                    return@addSnapshotListener
                }

                if (error != null) {
                    toast(error.localizedMessage ?: "Gönderiler yüklenirken hata")
                } else {
                    postList.clear()
                    if (value != null && !value.isEmpty) {
                        val documents = value.documents
                        for (document in documents) {
                            val id = document.id
                            val comment = document.getString("comment") ?: ""
                            val userName = document.getString("kullaniciAdi") ?: ""
                            val imageUrl = document.getString("imageUrl") ?: ""
                            val time = document.getTimestamp("createdAt")?.toDate() ?: Date()
                            val post = Posts(id, userName, comment, imageUrl, time)
                            postList.add(post)
                        }

                        if (isAdded && _binding != null) {
                            adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()

        userDataListener?.remove()
        postsListener?.remove()

        adapter = null

        _binding = null
    }
}