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
import android.widget.Toast
import com.rjhtctn.hacostagram.util.CircleTransform
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.rjhtctn.hacostagram.R
import com.rjhtctn.hacostagram.databinding.FragmentHomeBinding
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.lang.Exception

class HomeFragment : Fragment() {
    private lateinit var username: String
    private var defaultTint: ColorStateList? = null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var profileReg: ListenerRegistration? = null
    private var picassoTarget: Target? = null
    private var profileDrawableWithRing: ProfileDrawableWithRing? = null
    private var destinationListener: NavController.OnDestinationChangedListener? = null

    private class NoTintDrawableWrapper(drawable: Drawable) : DrawableWrapper(drawable) {
        override fun setTintList(tint: ColorStateList?) {}
        override fun setTint(tintColor: Int) {}
    }

    private class ProfileDrawableWithRing(
        private val profileDrawable: Drawable,
        private val context: android.content.Context
    ) : Drawable() {
        private val density = context.resources.displayMetrics.density
        private val photoDp = 32f
        private val gapDp = 3f
        private val ringDp = 3f
        private val ringPx = ringDp * density
        private val gapPx = gapDp * density
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = ContextCompat.getColor(context, R.color.colorPrimary)
            strokeWidth = ringPx
        }

        var isSelected = false
            set(value) {
                field = value
                invalidateSelf()
            }

        override fun draw(canvas: Canvas) {
            val b = bounds
            val cx = b.exactCenterX()
            val cy = b.exactCenterY()
            val r = (photoDp * density) / 2f
            profileDrawable.bounds = Rect(
                (cx - r).toInt(),
                (cy - r).toInt(),
                (cx + r).toInt(),
                (cy + r).toInt()
            )
            profileDrawable.draw(canvas)
            if (isSelected) {
                val outer = r + gapPx + ringPx / 2f
                canvas.drawCircle(cx, cy, outer, paint)
            }
        }

        override fun setAlpha(alpha: Int) {
            profileDrawable.alpha = alpha
            paint.alpha = alpha
        }

        override fun setColorFilter(cf: ColorFilter?) {
            profileDrawable.colorFilter = cf
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        override fun getIntrinsicWidth(): Int =
            ((photoDp + 2 * gapDp + 2 * ringDp) * density).toInt()

        override fun getIntrinsicHeight(): Int =
            ((photoDp + 2 * gapDp + 2 * ringDp) * density).toInt()
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
        binding.homeBottomNav.itemIconTintList = null

        binding.homeBottomNav.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.colorOnPrimary)
        )

        observeProfilePhoto()

        val navController = (childFragmentManager
            .findFragmentById(binding.homeNavHost.id) as NavHostFragment)
            .navController

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (navController.currentDestination?.id != navController.graph.startDestinationId) {
                        navController.navigateUp()
                    } else {
                        requireActivity().moveTaskToBack(true)
                    }
                }
            })

        binding.homeBottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.feedFragment -> {
                    val cur      = navController.currentDestination?.id
                    val args     = navController.currentBackStackEntry?.arguments
                    val atOwn    = cur == R.id.profilFragment && args?.getString("targetUsername") == null
                    val atUpload = cur == R.id.yuklemeFragment

                    if (atOwn || atUpload) {
                        navController.navigateUp()
                    } else {
                        navController.popBackStack(R.id.feedFragment, false)
                    }
                    NavigationUI.onNavDestinationSelected(item, navController)
                    true
                }
                R.id.yuklemeFragment -> {
                    NavigationUI.onNavDestinationSelected(item, navController)
                    true
                }
                R.id.profilFragment -> {
                    navController.popBackStack(R.id.profilFragment, true)
                    navController.navigate(R.id.profilFragment)
                    true
                }
                else -> false
            }
        }

        binding.homeBottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.profilFragment) {
                navController.popBackStack(R.id.profilFragment, true)
                navController.navigate(R.id.profilFragment)
            }
        }

        setupSelectionListener(navController)
    }

    private fun observeProfilePhoto() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("usersPrivate").document(uid).get()
            .addOnSuccessListener { doc ->
                username = doc.getString("kullaniciAdi").orEmpty()
                if (username.isBlank()) return@addOnSuccessListener

                profileReg = db.collection("usersPublic").document(username)
                    .addSnapshotListener { snap, err ->
                        if (err != null || snap == null) return@addSnapshotListener
                        val url = snap.getString("profilePhoto").orEmpty()
                        if (url.isNotBlank()) setBottomIcon(url)
                        else resetBottomIcon()
                    }
            }
    }

    private fun setBottomIcon(url: String) {
        val px = (32f * resources.displayMetrics.density).toInt()
        picassoTarget = object : Target {
            override fun onBitmapLoaded(bmp: Bitmap, from: Picasso.LoadedFrom) {
                if (!isAdded) return
                val scaled = bmp.scale(px, px)
                val drawable = NoTintDrawableWrapper(scaled.toDrawable(resources))
                profileDrawableWithRing = ProfileDrawableWithRing(drawable, requireContext())
                binding.homeBottomNav.menu.findItem(R.id.profilFragment).icon =
                    profileDrawableWithRing
                setupSelectionListener(
                    (childFragmentManager.findFragmentById(binding.homeNavHost.id)
                            as NavHostFragment).navController
                )
            }
            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) = resetBottomIcon()
            override fun onPrepareLoad(placeHolderDrawable: Drawable?){}
        }
        Picasso.get()
            .load(url)
            .placeholder(R.drawable.ic_profile)
            .transform(CircleTransform())
            .into(picassoTarget!!)
    }

    private fun resetBottomIcon() {
        profileDrawableWithRing = null
        setupSelectionListener(
            (childFragmentManager.findFragmentById(binding.homeNavHost.id)
                    as NavHostFragment).navController
        )
    }

    private fun setupSelectionListener(navController: NavController) {
        destinationListener?.let { navController.removeOnDestinationChangedListener(it) }
        destinationListener = NavController.OnDestinationChangedListener { _, dest, args ->
            val menu   = binding.homeBottomNav.menu
            val target = args?.getString("targetUsername")
            val isOther   = dest.id == R.id.profilFragment && !target.isNullOrBlank() && target != username
            val isYukleme = dest.id == R.id.yuklemeFragment
            val isOwnProf = dest.id == R.id.profilFragment && (target.isNullOrBlank() || target == username)

            val feedSelected = isOther || (!isYukleme && !isOwnProf)
            menu.findItem(R.id.feedFragment).apply {
                isChecked = feedSelected
                icon = ContextCompat.getDrawable(
                    requireContext(),
                    if (feedSelected) R.drawable.ic_home_selected else R.drawable.ic_home
                )
            }

            menu.findItem(R.id.yuklemeFragment).apply {
                isChecked = !isOther && isYukleme
                icon = ContextCompat.getDrawable(
                    requireContext(),
                    if (!isOther && isYukleme) R.drawable.ic_addpost_selected else R.drawable.ic_addpost
                )
            }

            menu.findItem(R.id.profilFragment).apply {
                isChecked = !isOther && isOwnProf
                icon = when {
                    profileDrawableWithRing != null -> {
                        profileDrawableWithRing!!.isSelected = (!isOther && isOwnProf)
                        profileDrawableWithRing
                    }
                    !isOther && isOwnProf -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_profile_selected)
                    else                  -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_profile)
                }
            }
        }
        navController.addOnDestinationChangedListener(destinationListener!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profileReg?.remove()
        picassoTarget = null
        destinationListener?.let {
            (childFragmentManager.findFragmentById(binding.homeNavHost.id)
                    as NavHostFragment).navController
                .removeOnDestinationChangedListener(it)
        }
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        FirebaseAuth.getInstance().currentUser
            ?.reload()
            ?.addOnCompleteListener { t ->
                if (!t.isSuccessful || FirebaseAuth.getInstance().currentUser == null) {
                    Toast.makeText(requireContext(),
                        "Oturum geçersiz, tekrar giriş yapın.",
                        Toast.LENGTH_LONG).show()
                    requireActivity().findNavController(R.id.fragmentContainerView)
                        .navigate(R.id.action_global_girisFragment, null,
                            NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                        )
                }
            }
    }
}
