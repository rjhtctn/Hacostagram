package com.rjhtctn.hacostagram.application

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.rjhtctn.hacostagram.R

class HacostagramApp : Application() {
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()

        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) { currentActivity = a }
            override fun onActivityPaused(a: Activity)  { currentActivity = null }

            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser == null) {
                android.os.Handler(mainLooper).post {
                    val act = currentActivity ?: return@post
                    val nav = act.findNavController(R.id.fragmentContainerView)
                    if (nav.currentDestination?.id != R.id.girisFragment) {
                        nav.navigate(
                            R.id.action_global_girisFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build()
                        )
                    }
                }
            }
        }
    }
}
