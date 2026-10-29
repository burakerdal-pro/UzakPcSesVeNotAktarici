# 📱 PC Not Defteri & Ses Kaydı Aktarıcı (Android to PC Sync)

Bu proje, Android cihazınızdan aynı yerel ağda (Wi-Fi) bulunan bilgisayarınıza **yazılı notlar** ve **ses kayıtları** göndermenizi sağlayan tam kapsamlı bir mobil uygulama ve bilgisayar sunucu sistemidir.

---

## 🌟 Öne Çıkan Özellikler

* 📝 **Yazılı Not Aktarımı:**
  * Telefondan yazılan metinleri bilgisayarın Masaüstü'nde `.txt` dosyasına kaydeder.
  * **Üstüne Yaz** veya **Altına Ekle** modlarını destekler.
  * Not gönderildiği anda bilgisayarda **Not Defteri (Notepad.exe)** uygulamasını otomatik başlatır.
* 🎙 **Ses Kaydı Aktarımı:**
  * Uygulama içinden yüksek kaliteli `.m4a` formatında ses kaydı alır.
  * Ses kaydını ağ üzerinden bilgisayara iletir ve Masaüstü'ne kaydeder.
  * Ses dosyası alındığı an bilgisayarın varsayılan **Müzik Oynatıcısı** ile otomatik açılır.
* ⚡ **Sıfır Bağımlılıklı PC Sunucusu:**
  * Windows bilgisayarlarda hiçbir kurulum yapmadan **PowerShell** ile çalışabilir.
  * Alternatif olarak **Python 3** ile de çalıştırılabilir.
  * Tek tıkla çalıştırmak için `baslat.bat` dosyası içerir.

---

## 🛠️ Teknolojiler ve Mimarisi

* **Mobil Uygulama (Android):**
  * **Dil:** Kotlin
  * **Arayüz:** Jetpack Compose & Material 3
  * **Ağ İşlemleri:** Kotlin Coroutines (`Dispatchers.IO`) & `HttpURLConnection`
  * **Ses Kaydı:** Android `MediaRecorder` API (AAC / MPEG_4)
  * **Veri Transfer Formatı:** JSON & Base64 (Ses akışı için)
* **Bilgisayar Sunucusu (PC):**
  * **PowerShell:** `System.Net.HttpListener` (Windows Yerleşik)
  * **Python:** `http.server.HTTPServer` & `BaseHTTPRequestHandler`

---

## 💻 Bilgisayar Tarafı: Port, Dinleyici ve Güvenlik Duvarı Yapılandırması

Bilgisayarınızın telefon ile haberleşebilmesi için yerel ağda dinleme yapan bir **HTTP Server (Dinleyici)** çalışması gerekir.

### 1. Port Dinleyici Mantığı (Port 5000)
* Sunucu scripti (`pc_server.ps1` veya `pc_server.py`) çalıştırıldığında bilgisayarın `0.0.0.0` IP adresi ve **`5000`** portunu dinlemeye başlar.
* Ağdaki Android telefon `http://<PC_IP_ADRESI>:5000` adresine HTTP istekleri (POST/GET) gönderir.

### 2. Sunucu Endpoint'leri (Erişim Noktaları)

| Endpoint | HTTP Method | Açıklama |
| :--- | :--- | :--- |
| `/ping` | `GET` | Bağlantı testidir. Telefon "Bağlantıyı Test Et" butonuna bastığında çalışır. |
| `/note` | `POST` | Yazılı not verilerini içerir (`filename`, `content`, `append`, `open_notepad`). Masaüstü'ne `.txt` kaydeder. |
| `/audio` | `POST` | Base64 ile kodlanmış ses dosyasını alır (`filename`, `audio_data`), çözer ve Masaüstü'ne `.m4a` olarak kaydeder. |

---

## 🚀 Kurulum ve Çalıştırma Rehberi

### Adım 1: Bilgisayarda Sunucuyu Çalıştırma

Proje klasöründeki **`baslat.bat`** dosyasına çift tıklayarak sunucuyu başlatabilirsiniz.

Eğer manuel çalıştırmak isterseniz:

#### Yöntem A: PowerShell İle (Kurulumsuz)
PowerShell'i açıp proje klasörüne gidin ve çalıştırın:
```powershell
.\pc_server.ps1
```

#### Yöntem B: Python İle
Komut satırından çalıştırın:
```cmd
python pc_server.py
```

Ekranda bilgisayarınızın yerel IP adresi görünecektir:
```text
============================================================
      PC NOT DEFTERI & SES SUNUCUSU BASLATILDI
============================================================
Bilgisayarinizin Yerel IP Adresi : 192.168.1.37
Dinlenen Port                     : 5000
Dosyalarin Kaydedilecegi Klasor   : C:\Users\Kullanici\Desktop
```

---

### Adım 2: Windows Güvenlik Duvarı (Firewall) İzni

Windows varsayılan olarak dışarıdan gelen port bağlantılarını engeller. Eğer telefondan bağlantı hatası alırsanız, bilgisayarınızda **PowerShell'i Yönetici Olarak** açıp şu komutu bir kez çalıştırın:

```powershell
netsh advfirewall firewall add rule name="NotDefteriPort" dir=in action=allow protocol=TCP localport=5000
```

---

### Adım 3: Mobil Uygulamayı Çalıştırma

1. Android telefonunuz ile bilgisayarınızın **aynı Wi-Fi ağına** bağlı olduğundan emin olun.
2. Android Studio'da projeyi derleyip telefonunuza yükleyin.
3. Uygulama açıldığında ekrandaki **IP Adresi** alanına bilgisayarınızın IP adresini (örn: `192.168.1.37`) girin.
4. **"Bağlantıyı Test Et"** butonuna basarak bağlantıyı doğrulayın.
5. İstediğiniz yazılı notu veya ses kaydını bilgisayarınıza gönderin!

---

## 📁 Dosya Yapısı

```text
├── app/                  # Android Uygulaması Kaynak Kodları
│   └── src/main/java/com/example/sat/
│       └── MainActivity.kt  # Jetpack Compose UI & Ağ Gönderim Kodları
├── pc_server.ps1         # Windows PowerShell Sunucu Scripti (Kurulumsuz)
├── pc_server.py          # Python HTTP Sunucu Scripti
├── baslat.bat            # Windows için Tek Tıkla Sunucu Başlatıcı
└── README.md             # Proje Dokümantasyonu
```

---

## 📝 Lisans
Bu proje eğitim ve kişisel kullanım amacıyla geliştirilmiştir. MIT Lisansı altındadır.
