package com.rjhtctn.hacostagram.view

import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rjhtctn.hacostagram.util.CircleTransform
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.rjhtctn.hacostagram.R
import com.rjhtctn.hacostagram.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var defaultTint: ColorStateList? = null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var profileReg: ListenerRegistration? = null
    private var picassoTarget: Target? = null
    private var profileDrawableWithRing: ProfileDrawableWithRing? = null
    private var destinationListener: NavController.OnDestinationChangedListener? = null

    private class NoTintDrawableWrapper(drawable: Drawable) : DrawableWrapper(drawable) {
        override fun setTintList(tint: ColorStateList?) {
        }

        override fun setTint(tintColor: Int) {
        }
    }

    private class ProfileDrawableWithRing(
        private val profileDrawable: Drawable,
        private val context: android.content.Context
    ) : Drawable() {
        private val ringWidth = 6f * context.resources.displayMetrics.density // 6dp halka kalınlığı
        private val extraPadding = ringWidth + 4f * context.resources.displayMetrics.density // 4dp extra boşluk

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = ContextCompat.getColor(context, R.color.colorPrimary) // veya istediğiniz renk
            strokeWidth = ringWidth
        }

        var isSelected = false
            set(value) {
                field = value
                invalidateSelf()
            }

        override fun draw(canvas: Canvas) {
            val bounds = getBounds()

            val innerSize = bounds.width() - (extraPadding * 2).toInt()
            val offsetX = (bounds.width() - innerSize) / 2
            val offsetY = (bounds.height() - innerSize) / 2

            val innerBounds = Rect(
                bounds.left + offsetX,
                bounds.top + offsetY,
                bounds.left + offsetX + innerSize,
                bounds.top + offsetY + innerSize
            )

            profileDrawable.bounds = innerBounds
            profileDrawable.draw(canvas)

            if (isSelected) {
                val centerX = bounds.exactCenterX()
                val centerY = bounds.exactCenterY()
                val radius = (bounds.width() / 2f) - (ringWidth / 2f)
                canvas.drawCircle(centerX, centerY, radius, paint)
            }
        }

        override fun setAlpha(alpha: Int) {
            profileDrawable.alpha = alpha
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            profileDrawable.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        override fun getIntrinsicWidth(): Int =
            profileDrawable.intrinsicWidth + (extraPadding * 2).toInt()

        override fun getIntrinsicHeight(): Int =
            profileDrawable.intrinsicHeight + (extraPadding * 2).toInt()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeBottomNav) { v, insets ->
            insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPaddingRelative(0, 0, 0, 0)
            insets
        }
        defaultTint = binding.homeBottomNav.itemIconTintList
        observeProfilePhoto()
        val wantedPx = (40 * resources.displayMetrics.density).toInt()
        binding.homeBottomNav.layoutParams.height = wantedPx
        binding.homeBottomNav.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorSurface))
        binding.homeBottomNav.requestLayout()
        val navHostFragment = childFragmentManager.findFragmentById(binding.homeNavHost.id) as? NavHostFragment
        val navController = navHostFragment?.navController
        if (navController != null) {
            NavigationUI.setupWithNavController(binding.homeBottomNav, navController)
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val navController = navHostFragment?.navController
                    if (navController != null && navController.currentDestination?.id != navController.graph.startDestinationId) {
                        navController.navigateUp()
                    } else {
                        requireActivity().moveTaskToBack(true)
                    }
                }
            }
        )
    }

    private fun observeProfilePhoto() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("usersPrivate").document(uid).get().addOnSuccessListener { privateDoc ->
            val username = privateDoc.getString("kullaniciAdi").orEmpty()
            if (username.isBlank()) return@addOnSuccessListener

            profileReg = db.collection("usersPublic").document(username)
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) return@addSnapshotListener
                    val url = snap.getString("profilePhoto").orEmpty()
                    if (url.isNotBlank())  setBottomIcon(url)
                    else                   resetBottomIcon()
                }
        }
    }

    private fun setBottomIcon(url: String) {
        picassoTarget = object : Target {
            override fun onBitmapLoaded(bmp: Bitmap, from: Picasso.LoadedFrom) {
                if (!isAdded) return

                val originalDrawable = bmp.toDrawable(resources)

                val protectedDrawable = NoTintDrawableWrapper(originalDrawable)

                profileDrawableWithRing = ProfileDrawableWithRing(protectedDrawable, requireContext())

                val profileItem = binding.homeBottomNav.menu.findItem(R.id.profilFragment)
                profileItem.icon = profileDrawableWithRing

                setupSelectionListener()
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                resetBottomIcon()
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) { /* no‑op */ }
        }

        Picasso.get()
            .load(url)
            .placeholder(R.drawable.ic_profile)
            .transform(CircleTransform())
            .into(picassoTarget!!)
    }

    private fun setupSelectionListener() {
        val navHostFragment = childFragmentManager.findFragmentById(binding.homeNavHost.id) as? NavHostFragment
        val navController = navHostFragment?.navController

        if (navController != null) {
            destinationListener = NavController.OnDestinationChangedListener { _, destination, _ ->
                profileDrawableWithRing?.isSelected = (destination.id == R.id.profilFragment)
            }

            navController.addOnDestinationChangedListener(destinationListener!!)

            profileDrawableWithRing?.isSelected = (navController.currentDestination?.id == R.id.profilFragment)
        }
    }

    private fun resetBottomIcon() {
        val profileItem = binding.homeBottomNav.menu.findItem(R.id.profilFragment)
        profileItem.setIcon(R.drawable.ic_profile)
        profileDrawableWithRing = null

        val navHostFragment = childFragmentManager.findFragmentById(binding.homeNavHost.id) as? NavHostFragment
        val navController = navHostFragment?.navController
        destinationListener?.let { navController?.removeOnDestinationChangedListener(it) }
        destinationListener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profileReg?.remove()
        picassoTarget = null

        val navHostFragment = childFragmentManager.findFragmentById(binding.homeNavHost.id) as? NavHostFragment
        val navController = navHostFragment?.navController
        destinationListener?.let { navController?.removeOnDestinationChangedListener(it) }

        _binding = null
    }
}