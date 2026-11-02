# Wisdom Share Android TV Launcher Değiştirme ve Hızlandırma Rehberi (Dijitsu / Onvo)

Bu rehber; **Dijitsu**, **Onvo**, **Sunny**, **Awox** ve **CVTE / Amlogic / MStar** anakartlı diğer **Wisdom Share (Smart Cloud TV)** AOSP arayüzüne sahip televizyonlarda:
1. Varsayılan başlatıcıyı (Launcher) değiştirmeyi,
2. Kumandadaki **Home (Ana Ekran)** tuşunun eski fabrika arayüzüne dönmesini engelleyip doğrudan **CouchyLauncher**'a yönlendirmeyi,
3. Arayüz animasyonlarını hızlandırıp sistemi optimize etmeyi anlatır.

---

## 📦 Paketler ve Hazır APK Dosyaları

Derlenmiş hazır kurulum dosyaları projenin `releases/` klasöründedir:
- **`releases/couchy-launcher-release.apk`** (`com.conreo.couchytv`): Hızlı, modern ve özelleştirilebilir TV arayüzü.
- **`releases/couchy-bridge-release.apk`** (`com.conreo.couchybridge`): Home tuşunu ve stok fabrika pencerelerini yakalayıp CouchyLauncher'a aktaran özel köprü servisi.

---

## 🛠️ Adım Adım Kurulum Rehberi

### Adım 1: Televizyonun Servis Menüsünden ADB'yi Açma
1. TV kumandasından `Menü` tuşuna basıp ardından hızlıca sırayla `1 1 47` (bazı kart modellerinde `Kaynak / Source` + `2 5 8 0`) tuşlayarak servis menüsünü açın.
2. **Debug** veya **Factory Settings** sekmesine gelin.
3. **ADB Enable** seçeneğini `ON` (Açık) yapın.
4. TV Ayarları > Ağ bölümünden televizyonunuzun yerel IP adresini (örn: `192.168.1.50`) not edin.

---

### Adım 2: APK'ları Televizyona Yükleme
Bilgisayarınızdan terminal veya komut satırını açarak TV ile aynı Wi-Fi ağındayken şu komutları çalıştırın:

```bash
# TV'ye bağlanın
adb connect <TV_IP_ADRESI>:5555

# CouchyLauncher'ı yükleyin
adb install -r releases/couchy-launcher-release.apk

# CouchyBridge (Home tuşu kancası) uygulamasını yükleyin
adb install -r releases/couchy-bridge-release.apk
```

---

### Adım 3: Home Tuşu Kancasını (Erişilebilirlik Servisi) Aktifleştirme
Wisdom Share televizyonlarda kumandanın Home tuşu donanımsal olarak eski arayüze kilitlenmiştir. Hazırladığımız `CouchyBridgeService`, arka planda bu tuşu ve eski arayüzün açılma isteklerini yakalayarak anında CouchyLauncher'a yönlendirir.

Aşağıdaki komutu çalıştırarak servise yetki verin:

```bash
adb shell settings put secure enabled_accessibility_services com.conreo.couchybridge/com.conreo.couchybridge.CouchyBridgeService:com.conreo.couchytv/com.conreo.couchytv.HomeOverrideService
adb shell settings put secure accessibility_enabled 1
```

*(Alternatif: TV Ayarları > Cihaz Tercihleri > **Erişilebilirlik** menüsünden **Couchy Bridge** seçeneğini kumandayla da açabilirsiniz).*

---

### Adım 4: Varsayılan Home Başlatıcısı Olarak Tanımlama
```bash
adb shell cmd package set-home-activity "com.conreo.couchybridge/.HomeRedirectActivity"
```

---

### Adım 5: TV'yi Hızlandırma & Animasyon Ayarları
Düşük donanımlı Android TV'lerde arayüz akıcılığını 2 katına çıkarmak için geçiş animasyon sürelerini kısaltın:

```bash
adb shell settings put global window_animation_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb shell settings put global animator_duration_scale 0.5
```

---

## ⚠️ Dikkat Edilmesi Gerekenler & Güvenli Debloat

1. **Bootloop Riski:** Wisdom Share tabanlı televizyonlarda stok launcher paketini tamamen silmek (`adb uninstall`) televizyonun açılış logosunda takılmasına (bootloop) yol açabilir.
2. Bu nedenle stok arayüzü **silmek yerine** yukarıdaki `CouchyBridge` yöntemini kullanmak en güvenli çözümdür.
3. Eğer stok paketi dondurmak isterseniz sadece `disable-user` kullanın:
   ```bash
   adb shell pm disable-user --user 0 <stok_launcher_paketi>
   ```
   *(Wisdom Share sistemlerde genellikle `com.cvte.tv.launcher`, `com.clover.launcher` veya `com.softwinner.launcher` olarak adlandırılır).*
4. Geri açmak (Rollback) için:
   ```bash
   adb shell pm enable <stok_launcher_paketi>
   ```
