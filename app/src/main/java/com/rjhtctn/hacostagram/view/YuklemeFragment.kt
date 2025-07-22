package com.rjhtctn.hacostagram.view

import android.Manifest
import android.content.pm.PackageManager
import com.google.firebase.firestore.Source
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import com.rjhtctn.hacostagram.BuildConfig
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.rjhtctn.hacostagram.databinding.FragmentYuklemeBinding
class YuklemeFragment : Fragment() {
    private var _binding: FragmentYuklemeBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var photoPicker: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var safPicker: ActivityResultLauncher<Array<String>>
    private var secilenGorsel: Uri? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        registerLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYuklemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.uploadButton.setOnClickListener { yukleButton() }
        binding.uploadImageView.setOnClickListener { gorselSec(it) }
        binding.uploadCommentEditText.filters = arrayOf(
            InputFilter { source, _, _, dest, dstart, dend ->
                val result = dest
                    .replaceRange(dstart, dend, source.toString())
                if (result.isEmpty()) return@InputFilter null

                val words = result.split("\\s+".toRegex())
                if (words.any { it.length > 30 }) {
                    toast("Lütfen kelime başına en fazla 30 karakter girin.")
                    return@InputFilter ""   // bu karakteri ekleme
                }
                if (source.any { it.isISOControl() && it != '\n' }) {
                    toast("Geçersiz karakter kullandınız.")
                    return@InputFilter ""
                }
                null
            })
        binding.uploadCommentEditText.setHorizontallyScrolling(false)
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
        binding.uploadImageView.imageTintList = null
        binding.uploadImageView.setImageURI(uri)
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
    private fun yukleButton() {
        setButtonLoading(true)
        if (secilenGorsel == null) {
            Toast.makeText(requireContext(), "Önce fotoğraf seçin", Toast.LENGTH_SHORT).show()
            setButtonLoading(false)
            return
        }

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
                    setButtonLoading(false)
                }
                override fun onReschedule(reqId: String?, err: ErrorInfo?) {}
            })
            .dispatch()
    }

    private fun firestoreKaydet(url: String) {
        val uid = auth.currentUser?.uid ?: run {
            toast("Oturum Kapandı!")
            setButtonLoading(false)
            return
        }
        val db = Firebase.firestore
        db.collection("usersPrivate").document(uid).get(Source.SERVER)
            .addOnSuccessListener { snap ->
                val postsRef = Firebase.firestore.collection("posts").document()
                val newId = postsRef.id
                val username = snap.getString("kullaniciAdi") ?: ""
                val post = hashMapOf(
                    "imageUrl"  to url.replace("upload/", "upload/q_auto,f_auto/"),
                    "comment"   to binding.uploadCommentEditText.text.toString(),
                    "kullaniciAdi" to username,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "id" to newId
                )


                postsRef.set(post)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Paylaşıldı!", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }.addOnFailureListener { e ->
                        toast("Firestore hata: ${e.localizedMessage}")
                        setButtonLoading(false)
                    }
            }.addOnFailureListener { e ->
                toast("Profili okuyamadım: ${e.localizedMessage}")
                setButtonLoading(false)
            }
    }

    private fun setButtonLoading(isLoading: Boolean) {
        requireActivity().runOnUiThread {
            binding.uploadButton.isEnabled = !isLoading
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView(); _binding = null
    }
}