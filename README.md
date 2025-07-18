# Hacostagram

Kotlin ile geliştirilen, Firebase tabanlı basit bir fotoğraf paylaşım uygulaması.

## Özellikler

* **Kullanıcı girişi** – E‑posta & şifre ile oturum açma (Firebase Authentication)
* **Fotoğraf paylaşımı** – Cihazdan seçilen görselleri yükleyip gönderi oluşturma
* **Gerçek‑zamanlı akış** – Gönderileri kronolojik olarak listeleme (Cloud Firestore)
* **Görsel önbellekleme** – Resimleri hızlıca göstermek için Picasso
* **Harici medya depolama** – Cloudinary entegrasyonu ile esnek resim saklama

## Hızlı Başlangıç

> Proje anahtarları dâhil değildir; kendi Firebase yapılandırmanızı kullanmalısınız.

1. Depoyu klonlayın:

   ```bash
   git clone https://github.com/kullanici/hacostagram.git
   ```
2. Firebase Console’da yeni bir proje oluşturun.
3. **Authentication** (E‑posta/Şifre) ve **Cloud Firestore**’u etkinleştirin.
4. Android uygulaması ekleyin, `google-services.json` dosyasını indirip `app/` klasörüne yerleştirin.
5. [Cloudinary](https://cloudinary.com/) hesabı oluşturun ve API anahtarınızı güvenli şekilde ekleyin.
6. Android Studio ile projeyi açın ve **Run** tuşuna basın.

## Temel Mimarî

| Katman      | Açıklama                                       |
| ----------- | ---------------------------------------------- |
| **UI**      | Activity / Fragment + View Binding             |
| **Adapter** | `PostAdapter` sınıfı ile RecyclerView          |
| **Data**    | Firebase Cloud Firestore ‑ `Posts` koleksiyonu |

## Önemli Bağımlılıklar

* **Firebase BoM** – `com.google.firebase:firebase-bom`
* **Cloudinary Android SDK** – `com.cloudinary:cloudinary-android`
* **Picasso** – `com.squareup.picasso:picasso`
* **AndroidX RecyclerView**