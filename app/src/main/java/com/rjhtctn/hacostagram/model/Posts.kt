package com.rjhtctn.hacostagram.model

import java.util.Date
data class Posts(
    val id: String,
    val kullaniciAdi: String,
    val comment: String,
    val imageUrl: String,
    val time: Date
)
