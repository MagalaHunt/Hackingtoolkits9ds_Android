# PackEnglish Mobile

Aplikasi Android sederhana yang dibuat ulang dari alur kerja pemeriksaan berkas pada PackEnglishV12 / HackingToolkit9DS.

## Fitur versi Android ini

- Memilih satu atau beberapa berkas `.3ds`, `.cia`, dan `.cxi` melalui pemilih berkas Android.
- Memeriksa signature NCSD / NCCH dan ukuran dasar pada header CIA.
- Menampilkan rentang partisi yang tercatat dalam header NCSD dan masih berada di dalam ukuran file.
- Mengekspor partisi NCCH dari berkas NCSD sebagai salinan byte mentah.
- Berjalan secara offline; berkas tidak diunggah ke internet.

## Fitur yang belum tersedia

Ini **bukan konversi langsung** dari program Windows. ZIP yang diberikan berisi program `.exe` dan ReadMe, bukan kode sumber atau library Android yang dapat dipakai ulang. Karena itu versi ini belum dapat mendekripsi konten, mencari atau memakai title key, mengekstrak ROMFS/banner, membangun ulang berkas `.3ds` / `.cia`, atau melakukan batch-rebuild. Partisi yang diekspor tetap berupa data mentah dan mungkin masih terenkripsi.

Gunakan hanya berkas milik sendiri atau berkas homebrew yang memang boleh kamu periksa.

## Cara membangun APK

APK belum dapat dikompilasi di lingkungan ini karena Android SDK dan Gradle tidak terpasang. Proyek sumber sudah disiapkan untuk dibuka di Android Studio:

1. Buka folder `PackEnglishMobile` di Android Studio.
2. Gunakan JDK 17 dan pasang **Android SDK Platform 35** saat diminta.
3. Pastikan Gradle 8.9 dan Android Gradle Plugin 8.7.3 dapat diunduh oleh Android Studio.
4. Jalankan Gradle Sync.
5. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. APK debug akan dibuat di `app/build/outputs/apk/debug/`.

Aplikasi tidak meminta izin penyimpanan yang luas. Akses file dilakukan melalui pemilih dokumen bawaan Android.

## Struktur

- `app/src/main/java/id/packenglish/mobile/MainActivity.java` — antarmuka dan alur pemilihan/ekspor file.
- `app/src/main/java/id/packenglish/mobile/NcsdInspector.java` — pembaca header NCSD/NCCH/CIA dan penyalin rentang partisi.
- `tests/NcsdInspectorSmokeTest.java` — uji sederhana parser dan penyalinan byte.
