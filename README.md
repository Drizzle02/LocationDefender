# LocationDefender - Multi-Layer Location Security

Aplikasi edukasi Android yang mengimplementasikan 6 layer deteksi lokasi berdasarkan analisis reverse engineering modul HideMockLocation.

## 📊 Fitur Deteksi

### Layer 1-6: API & Behavioral Analysis
- **Layer 1**: `isMock()` & `isFromMockProvider()` check
- **Layer 2**: Accuracy validation (< 1m or > 1km)
- **Layer 3**: Speed/velocity & teleportation detection
- **Layer 4**: Timestamp verification
- **Layer 5**: Provider classification
- **Layer 6**: Coordinate jitter analysis

### Behavioral Monitoring (Real-time)
- Speed consistency check
- Coordinate variance analysis
- Update interval regularity
- Accuracy stability
- Position stability patterns

## 🚀 Build dengan GitHub Actions

### Langkah-langkah:

1. **Buat repository baru di GitHub**
   ```bash
   # Di GitHub, buat repo baru bernama "LocationDefender"
   ```

2. **Push code ke GitHub**
   ```bash
   git remote add origin https://github.com/USERNAME/LocationDefender.git
   git branch -M main
   git push -u origin main
   ```

3. **Aktifkan GitHub Actions**
   - Buka repository di GitHub
   - Klik tab "Actions"
   - Workflow akan otomatis running

4. **Download APK**
   - Setelah build selesai, download artifact di section "Artifacts"

### Alternatif: Manual Trigger
Workflow bisa di-trigger manual via tombol "Run workflow" di tab Actions.

## 📱 Instalasi

Setelah mendapatkan APK dari GitHub Actions:
```bash
adb install app-debug.apk
```

## 🛠️ Build Lokal (Opsional)

```bash
# Dengan Android Studio
Open project → Build → Build Bundle(s)/APK(s)

# Dengan command line
./gradlew assembleDebug
```

## 📚 Referensi

Berdasarkan analisis reverse engineering dari:
- `app-release.apk` (HideMockLocation v2.3.1)
- Android Location API Documentation
- OWASP Mobile Security Testing Guide

## ⚠️ Disclaimer

Aplikasi ini dibuat untuk tujuan edukasi dan defensive security testing.
