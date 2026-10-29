This is a photo editing application that provides functionality for loading an image from the device gallery or using a synthetic sample bitmap,
applying real-time photo filters (brightness, contrast, saturation, and gamma),and saving the edited photo to media storage with runtime 
permission management.

Performance is enhanced with the use of Kotlin coroutines.

# Seam Slicing Photo Editor for Android

A native Android photo editing application powered by a custom **Seam Slicing (Seam Carving)** content-aware image resizing engine. This application allows users to intelligently resize, scale, and manipulate images without distorting critical visual subjects.

Built entirely using **Kotlin**, **Jetpack Compose**, and **Android Studio**.

## Features

* **Content-Aware Resizing:** Dynamically scale images by calculating low-energy pixel paths (seams) to preserve vital objects.
* **Modern UI:** Clean, intuitive interface built entirely using Jetpack Compose components.
* **Local Processing:** All seam carving calculations and image processing occur strictly on-device for maximum speed and privacy.

## Open Source & Licensing

This application is proudly open source under the **GNU General Public License v3 (GPLv3)**. 

* **No Closed-Source Reuse:** The core seam-slicing logic and UI components *cannot* be bundled into proprietary or closed-source applications. Any modifications or derivative works must also be open-sourced and licensed under the GPLv3.
* **Google Play Compliance:** In accordance with GPLv3 compliance for compiled binaries distributed on storefronts, the complete source code for this live application is freely accessible in this repository.

## Installation & Build Requirements

* Android Studio (Ladybug or later)
* Android SDK 34+
* Kotlin Toolchain CLI / Gradle Build Tool

```bash
git clone https://github.com
cd photo-editor-android
./gradlew assembleDebug
```

