package com.rjhtctn.hacostagram.util

import androidx.lifecycle.MutableLiveData

object FeedEventsBus {

    sealed interface Event {
        data class NewPost(val postId: String)                : Event
        data class CommentUpdated(val postId: String)         : Event
        data class PostDeleted(val postId: String)            : Event
        data class ProfilePhotoChanged(
            val username: String,
            val newUrl : String
        ) : Event
    }
    val live: MutableLiveData<Event> = MutableLiveData()
    fun publish(event: Event) = live.postValue(event)
}
