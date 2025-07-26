package com.rjhtctn.hacostagram.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.*
import com.rjhtctn.hacostagram.model.Posts
import com.rjhtctn.hacostagram.util.FeedEventsBus
import java.util.Date

class ProfilViewModel : ViewModel() {

    data class UserPublic(
        val isim: String = "",
        val soyisim: String = "",
        val photoUrl: String = ""
    )

    fun initByUsername(username: String) {
        if (username.isBlank() || cachedUsername == username) return
        cachedUsername = username
        startListeners()
    }

    val userLive  = MutableLiveData<UserPublic>()
    val postsLive = MutableLiveData<List<Posts>>()

    var cachedUsername: String? = null
        private set

    private val db = FirebaseFirestore.getInstance()
    private var userReg : ListenerRegistration? = null
    private var postReg : ListenerRegistration? = null

    private val busObserver = Observer<FeedEventsBus.Event> { e ->
        when (e) {
            is FeedEventsBus.Event.NewPost -> maybeAddNew(e.postId)
            is FeedEventsBus.Event.CommentUpdated -> refreshSingle(e.postId)
            is FeedEventsBus.Event.PostDeleted   -> removeLocal(e.postId)
            is FeedEventsBus.Event.ProfilePhotoChanged ->
                if (e.username == cachedUsername) {
                    userLive.value = userLive.value?.copy(photoUrl = e.newUrl)
                }
        }
    }

    init { FeedEventsBus.live.observeForever(busObserver) }
    private fun startListeners() { startUserListener(); startPostsListener() }

    private fun startUserListener() {
        val username = cachedUsername ?: return

        userReg?.remove()
        userReg = null

        userReg = db.collection("usersPublic").document(username)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                userLive.postValue(
                    UserPublic(
                        isim     = snap.getString("isim").orEmpty(),
                        soyisim  = snap.getString("soyisim").orEmpty(),
                        photoUrl = snap.getString("profilePhoto").orEmpty()
                    )
                )
            }
    }

    private fun maybeAddNew(id: String) {
        val owner = cachedUsername ?: return
        db.collection("posts").document(id).get(Source.DEFAULT)
            .addOnSuccessListener { d ->
                if (d.exists() && d.getString("kullaniciAdi") == owner) {
                    val newPost = d.toPost()
                    val cur = postsLive.value ?: emptyList()
                    postsLive.postValue(listOf(newPost) + cur)
                }
            }
    }

    private fun startPostsListener() {
        val username = cachedUsername ?: return

        postReg?.remove()
        postReg = null

        postReg = db.collection("posts")
            .whereEqualTo("kullaniciAdi", username)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { s, e ->
                if (e != null || s == null) return@addSnapshotListener
                postsLive.postValue(s.toObjects(Posts::class.java))
            }
    }

    private fun refreshSingle(id: String) {
        val list = postsLive.value ?: return
        val idx  = list.indexOfFirst { it.id == id }
        if (idx == -1) return

        db.collection("posts").document(id).get(Source.DEFAULT).addOnSuccessListener { d ->
            val l = list.toMutableList()
            l[idx] = d.toPost()
            postsLive.postValue(l)
        }
    }

    private fun removeLocal(id: String) =
        postsLive.postValue(postsLive.value?.filterNot { it.id == id })

    private fun DocumentSnapshot.toPost() = Posts(
        id           = id,
        kullaniciAdi = getString("kullaniciAdi") ?: "",
        comment      = getString("comment")      ?: "",
        imageUrl     = getString("imageUrl")     ?: "",
        time         = getTimestamp("createdAt")?.toDate() ?: Date()
    )

    override fun onCleared() {
        userReg?.remove(); postReg?.remove()
        FeedEventsBus.live.removeObserver(busObserver)
    }
}