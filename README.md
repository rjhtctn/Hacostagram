# Hacostagram

Kotlin + Jetpack Navigation ile geliştirilen, Firebase ve **Cloudinary (unsigned upload)** destekli fotoğraf paylaşım uygulaması  
“Instagram mantığını” sade fakat **ölçeklenebilir** bir mimaride örneklemeyi amaçlar.

---

## 🚀 Özellikler

| Kategori | Özellikler                                                                                             |
| -------- |--------------------------------------------------------------------------------------------------------|
| **Kayıt** | Ad‑Soyad, Kullanıcı Adı, E‑posta, Şifre                                                                |
| **Giriş** | Kullanıcı Adı / E-Posta + Şifre                                                                        |
| **Şifre Sıfırlama** | Kullanıcı Adı + E‑posta                                                                                |
| **Şifre Değiştirme** | Mevcut Şifre + Yeni Şifre                                                                              |
| **Gönderi** | Görsel seç + Yorum Ekle & yükle <br> Gerçek‑zamanlı akış (Firestore) <br> Gönderi menüsü – Sil / Düzenle |
| **Profil** | Kullanıcı gönderilerini listeleme & Bilgiler <br> Hesap silme (Firebase + Cloudinary)                  |
| **Arayüz** | Bottom Navigation → Feed / Home / Profile <br> Drawer Menu <br> Material 3 tema                        |
| **Medya** | Cloudinary “unsigned upload” <br> Picasso ile yerel önbellek                                           |

---

## 🗂 Ekran / Fragment Haritası

| Fragment | Amaç                                                  |
| -------- |-------------------------------------------------------|
| `GirisFragment` | **Kullanıcı Adı/E-Posta + Şifre** ile giriş           |
| `KayitFragment` | Ad‑Soyad, Kullanıcı Adı, E‑posta, Şifre ile kayıt     |
| `SifreSifirlamaFragment` | Kullanıcı Adı & E‑posta ile sıfırlama isteği          |
| `SifreDegistirFragment` | Mevcut Şifre + Yeni Şifre doğrulama                   |
| `FeedFragment` | Tüm gönderileri listeler                              |
| `YuklemeFragment` | Yeni fotoğraf yükleme + Yorum ekleme                  |
| `ProfilFragment` | Kullanıcının profil duvarı + Kullanıcının gönderileri |
| `KayitSilFragment` | Kullanıcı adı + E-Posta + Şifre + Hesap kapatma onayı |
| `HomeFragment` | Alt gezinmenin kök noktası                            |

Tam yönlendirme yapısı iki ayrı Navigation Graph’te (`nav_graph.xml`, `home_nav_graph.xml`) tanımlıdır.

---

## 🔧 Kurulum

> Proje anahtarları **dahil değildir**. Aşağıdaki adımlar size ait yapılandırmayı içerir.

```bash
git clone https://github.com/rjhtctn/hacostagram.git
````

1. **Firebase Console** ► yeni proje ► Android uygulaması ekleyin
   `google-services.json` dosyasını `app/` klasörüne koyun.
2. Authentication’da **E‑posta/Şifre**; Firestore’da **test kuralları** (veya kendi kurallarınız) etkinleştirin.
3. **Cloudinary** hesabı açın → **Unsigned Preset** oluşturun.
   `local.properties` veya CI gizli değişkenlerinde:

   ```
   CLOUD_NAME=xxx
   API_KEY=xxx
   UNSIGNED_PRESET=unsigned_preset
   ```
4. Android Studio ► **Run** ▶️

---

## 🏗 Katmanlı Mimarî

```
app/
 ├── ui/ (Activity & Fragment’ler)
 ├── adapter/ PostAdapter.kt
 ├── model/  Posts.kt
 └── res/
      ├── layout/ …xml
      └── navigation/ …xml
```

Basit **Fragment + Repository** düzeni; ileri seviye için ViewModel‑Hilt’e geçirilebilir.

---

## 📦 Önemli Bağımlılıklar

| Grup          | Kütüphane                                                                                                                            |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| Firebase      | `com.google.firebase:firebase-bom`                                                                                                   |
| Cloud Storage | `com.cloudinary:cloudinary-android`                                                                                                  |
| UI            | `androidx.navigation:navigation-fragment-ktx` <br>`androidx.navigation:navigation-ui-ktx` <br>`com.google.android.material:material` |
| Görsel        | `com.squareup.picasso:picasso`                                                                                                       |
| Test          | `junit:junit`, `androidx.test.ext:junit`                                                                                             |

---