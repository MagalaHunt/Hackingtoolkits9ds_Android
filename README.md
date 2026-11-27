# PackEnglish Mobile

An Android app for basic, read-only inspection of Nintendo 3DS `.3ds`, `.cia`, and `.cxi` files.

This is an independent Android implementation of a small part of the file-inspection workflow associated with PackEnglishV12 / HackingToolkit9DS. It is not a direct port of the original Windows application.

## Features

- Select one or multiple `.3ds`, `.cia`, and `.cxi` files using Android's document picker.
- Inspect NCSD and NCCH signatures and read basic CIA header size fields.
- For NCSD / 3DS containers, list partition entries that fit within the source file.
- Export a detected NCCH partition as an unchanged raw byte copy.
- Work offline. The app does not upload selected files to the internet.

## How to use the app

1. Open **PackEnglish Mobile**.
2. Tap **Choose 3DS / CIA / CXI Files**.
3. In Android's file picker, select one file or choose multiple files if your file picker supports multi-selection. Select a `.3ds`, `.cia`, or `.cxi` file.
4. Wait for the scan to finish. The app displays the file name, size, detected format, and any available inspection details.
5. For a **3DS / NCSD** file with valid NCCH partition entries, buttons such as **Export partition 0** will appear.
6. Tap the partition you want to copy. Android will open a **Save** dialog. Choose a destination folder, confirm the filename, and save the exported `.ncch` file.
7. Repeat for any other partition you need to export.

### What the results mean

- **3DS / NCSD**: The app found an NCSD container header. It lists partition entries whose offsets and sizes fit within the selected file.
- **CXI / NCCH**: The app recognized an NCCH header or a `.cxi` file. This version inspects the header but does not export CXI content.
- **CIA**: The app reads basic CIA header size fields. It does not unpack or export CIA contents.
- **Unknown**: The expected NCSD / NCCH signature was not found and the selected file was not identified as CIA.

**Important:** Partition export is a raw copy of the selected byte range. The app does not decrypt it or change its contents. The exported file may still be encrypted and may not be directly usable by other tools.

## Limitations and safe use

This version does **not**:

- decrypt game or system content;
- retrieve, extract, or use title keys;
- extract ROMFS, banners, or other internal resources;
- rebuild or convert `.3ds` / `.cia` files; or
- perform batch rebuilding.

The app only performs basic structural inspection and raw partition copying. Use it only with files you own or homebrew files you are authorized to inspect. Respect the applicable copyright and terms for your files.

## Download the APK from GitHub Actions

The repository's **Build Android APK** workflow builds a debug APK when changes are pushed to the `main` branch.

1. Open the repository on GitHub and select the **Actions** tab.
2. Open the latest successful **Build Android APK** run.
3. Scroll to **Artifacts** at the bottom of the run summary.
4. Download **PackEnglishMobile-debug-apk** and extract the downloaded ZIP file.
5. The APK inside the archive is named `app-debug.apk`.

A debug APK is intended for testing. Android may ask you to approve installation from the app or browser you used to download it.

## Build locally with Android Studio

Requirements:

- JDK 17
- Android SDK Platform 35
- Android Gradle Plugin 8.7.3
- Gradle 8.9

Then:

1. Open the project folder in Android Studio.
2. Allow Android Studio to sync the Gradle project and install the required Android SDK components.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Find the debug APK in `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

- `app/src/main/java/id/packenglish/mobile/MainActivity.java` — user interface, file selection, inspection display, and partition export.
- `app/src/main/java/id/packenglish/mobile/NcsdInspector.java` — read-only NCSD / NCCH / CIA header inspection and byte-range copying.
- `tests/NcsdInspectorSmokeTest.java` — parser and byte-copy smoke test.
- `.github/workflows/build-apk.yml` — GitHub Actions workflow for building the debug APK.

## Privacy

File inspection and partition copying are performed on the device. Selected files are not uploaded by this app.
