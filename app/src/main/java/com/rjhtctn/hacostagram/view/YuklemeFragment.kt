package com.rjhtctn.hacostagram.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
    private var secilenBitmap: Bitmap? = null
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
        binding.uploadImageView.setImageURI(uri)

        secilenBitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(requireActivity().contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(
                requireActivity().contentResolver, uri)
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

    private fun yukleButton() {
        if (secilenGorsel == null) {
            Toast.makeText(requireContext(), "Önce fotoğraf seçin", Toast.LENGTH_SHORT).show()
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
                }
                override fun onReschedule(reqId: String?, err: ErrorInfo?) {}
            })
            .dispatch()
    }

    private fun firestoreKaydet(url: String) {
        val post = hashMapOf(
            "email"     to auth.currentUser?.email,
            "imageUrl"  to url.replace("upload/", "upload/q_auto,f_auto/"),
            "comment"   to binding.uploadCommentEditText.text.toString(),
            "userId"    to auth.currentUser?.uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        Firebase.firestore.collection("posts")
            .add(post)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Paylaşıldı!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView(); _binding = null
    }
}
