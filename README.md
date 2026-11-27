# PackEnglish Mobile — 3DS Logo Assets

This package contains **only the 3DS logo assets**. It does not include the Android application source code or an APK.

## Files

- `logo/3ds-logo-master.png` — original 1024 × 1024 master image.
- `logo/3ds-logo-1024.png`, `3ds-logo-512.png`, `3ds-logo-256.png`, `3ds-logo-128.png` — resized PNG exports.
- `android/` — Android legacy launcher icon PNGs for the five standard density folders.

## Android integration

Copy the PNG from each folder under `android/` into the matching folder in your Android project:

- `mipmap-mdpi/ic_launcher.png`
- `mipmap-hdpi/ic_launcher.png`
- `mipmap-xhdpi/ic_launcher.png`
- `mipmap-xxhdpi/ic_launcher.png`
- `mipmap-xxxhdpi/ic_launcher.png`

Then ensure the `<application>` element in `AndroidManifest.xml` references the launcher icon:

```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher"
    ... >
```

The logo is a raster PNG design. The wordmark uses a generated bold, italic, angular style; it is not a font file or an editable typeface project.
