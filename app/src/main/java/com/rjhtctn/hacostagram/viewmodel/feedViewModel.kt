package com.rjhtctn.hacostagram.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.*
import com.rjhtctn.hacostagram.model.Posts
import com.rjhtctn.hacostagram.util.FeedEventsBus
import java.util.Date

class feedViewModel : ViewModel() {

    val postsLive = MutableLiveData<List<Posts>>()

    private val db = FirebaseFirestore.getInstance()
    private val items = mutableListOf<Posts>()
    private val photoCache = mutableMapOf<String, String>()
    private val userRegMap = mutableMapOf<String, ListenerRegistration>()
    private var listener : ListenerRegistration? = null

    private val busObserver = Observer<FeedEventsBus.Event> { e ->
        when (e) {
            is FeedEventsBus.Event.NewPost -> refreshSingle(e.postId)
            is FeedEventsBus.Event.CommentUpdated -> refreshSingle(e.postId)
            is FeedEventsBus.Event.PostDeleted   -> removeLocal(e.postId)
            is FeedEventsBus.Event.ProfilePhotoChanged -> {
                photoCache[e.username] = e.newUrl
                items.indices.forEach { i ->
                    if (items[i].kullaniciAdi == e.username) {
                        items[i] = items[i].copy(profilePhotoUrl = e.newUrl)
                    }
                }
                postsLive.postValue(items.toList())
            }
        }
    }

    init { FeedEventsBus.live.observeForever(busObserver) }

    fun startListening() {
        if (listener != null) return

        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get(Source.CACHE)
            .addOnSuccessListener { snap ->
                items.clear()
                snap.documents.forEach { d ->
                    val p = d.toPost()
                    items.add(p); fetchProfilePicIfNeeded(p)
                }
                postsLive.postValue(items.toList())
            }
            .addOnCompleteListener {
                listener = db.collection("posts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener(MetadataChanges.INCLUDE) { s, e ->
                        if (e != null || s == null || s.metadata.isFromCache) return@addSnapshotListener
                        for (dc in s.documentChanges) {
                            when (dc.type) {
                                DocumentChange.Type.ADDED -> {
                                    val post = dc.document.toPost()
                                    items.add(dc.newIndex, post)
                                    fetchProfilePicIfNeeded(post)
                                }
                                DocumentChange.Type.MODIFIED -> {
                                    val upd = dc.document.toPost()
                                        .copy(profilePhotoUrl = items[dc.oldIndex].profilePhotoUrl)
                                    items[dc.oldIndex] = upd
                                }
                                DocumentChange.Type.REMOVED ->
                                    items.removeAt(dc.oldIndex)
                            }
                        }
                        postsLive.postValue(items.toList())
                    }
            }
    }

    private fun fetchProfilePicIfNeeded(post: Posts) {
        val user = post.kullaniciAdi

        photoCache[user]?.let { cached ->
            val idx = items.indexOf(post)
            items[idx] = post.copy(profilePhotoUrl = cached)
            postsLive.postValue(items.toList())
            ensureUserListener(user); return
        }

        db.collection("usersPublic").document(user)
            .get(Source.CACHE)
            .addOnSuccessListener { doc ->
                val url = doc.getString("profilePhoto").orEmpty()
                if (url.isNotBlank()) updatePostPhoto(user, url, post)
                ensureUserListener(user)
                if (!doc.exists()) fetchFromServer(post, user)
            }
            .addOnFailureListener {
                ensureUserListener(user); fetchFromServer(post, user)
            }
    }

    private fun ensureUserListener(user: String) {
        if (userRegMap.containsKey(user)) return
        val reg = db.collection("usersPublic").document(user)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val newUrl = snap.getString("profilePhoto").orEmpty()
                if (newUrl != photoCache[user]) {
                    photoCache[user] = newUrl
                    items.indices.forEach { i ->
                        if (items[i].kullaniciAdi == user)
                            items[i] = items[i].copy(profilePhotoUrl = newUrl)
                    }
                    postsLive.postValue(items.toList())
                }
            }
        userRegMap[user] = reg
    }

    private fun fetchFromServer(post: Posts, user: String) {
        db.collection("usersPublic").document(user).get()
            .addOnSuccessListener { doc ->
                val url = doc.getString("profilePhoto").orEmpty()
                if (url.isNotBlank()) updatePostPhoto(user, url, post)
                ensureUserListener(user)
            }
            .addOnFailureListener { ensureUserListener(user) }
    }

    private fun updatePostPhoto(user: String, url: String, post: Posts) {
        photoCache[user] = url
        val idx = items.indexOf(post)
        if (idx != -1) {
            items[idx] = post.copy(profilePhotoUrl = url)
            postsLive.postValue(items.toList())
        }
    }

    private fun refreshSingle(id: String) {
        db.collection("posts").document(id).get(Source.DEFAULT)
            .addOnSuccessListener { d ->
                val idx = items.indexOfFirst { it.id == id }
                if (idx == -1) return@addOnSuccessListener
                val upd = d.toPost()
                    .copy(profilePhotoUrl = items[idx].profilePhotoUrl)
                items[idx] = upd
                postsLive.postValue(items.toList())
            }
    }

    private fun removeLocal(id: String) {
        val removed = items.removeAll { it.id == id }
        if (removed) postsLive.postValue(items.toList())
    }
    private fun DocumentSnapshot.toPost() = Posts(
        id           = id,
        kullaniciAdi = getString("kullaniciAdi") ?: "",
        comment      = getString("comment")      ?: "",
        imageUrl     = getString("imageUrl")     ?: "",
        time         = getTimestamp("createdAt")?.toDate() ?: Date()
    )

    override fun onCleared() {
        listener?.remove()
        userRegMap.values.forEach { it.remove() }
        FeedEventsBus.live.removeObserver(busObserver)
    }
}