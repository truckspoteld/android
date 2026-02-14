# How to Build an APK (Truckspot Android App)

Simple steps to create an APK you can install on any Android phone. No Kotlin knowledge needed.

---

## 1. Prerequisites

- **Java 17** (required by the project).
- **Android SDK** — easiest is to install **Android Studio** once: [Download Android Studio](https://developer.android.com/studio). You don’t have to use it every time; it installs the SDK and Gradle.

- **Point the project to the SDK**  
  In the `Truckingapp` folder, edit **`local.properties`** and set your SDK path:

  **Mac:**
  ```properties
  sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
  ```
  Replace `YOUR_USERNAME` with your Mac username (e.g. `macpro`).  
  If you installed Android Studio, it often creates this file when you open the project.

---

## 2. Build a Debug APK (easiest — for testing / your own use)

Open **Terminal**, go to the app folder, and run:

```bash
cd /Users/macpro/Documents/Truckspot/Truckingapp
./gradlew assembleDebug
```

- First run can take several minutes (downloads Gradle and dependencies).
- When it finishes, the APK is here:
  - **`app/build/outputs/apk/debug/app-debug.apk`**  
  or (if the project renames it):
  - **`app/build/outputs/apk/debug/Eagleye-1.3.17.apk`**

You can copy this file to your phone (USB, email, cloud) and install it.  
On the phone you may need to allow **“Install from unknown sources”** for the file manager or browser you use.

---

## 3. Build a Release APK (for distribution / Play Store style)

Release builds are signed and a bit smaller. From the same folder:

```bash
cd /Users/macpro/Documents/Truckspot/Truckingapp
./gradlew assembleRelease
```

- The project is already set up to use the keystore in **`keyStore/release.keystore`** (see `gradle.properties`).
- Output is usually:
  - **`app/build/outputs/apk/release/Eagleye-1.3.17.apk`**  
  (or similar name with the version from `app/build.gradle`).

If you get an error about the keystore or passwords, check that `keyStore/release.keystore` exists and that the passwords in `gradle.properties` match the keystore.

---

## 4. Using Android Studio (optional)

1. Open **Android Studio**.
2. **File → Open** and select the **`Truckingapp`** folder.
3. Wait for **Gradle sync** to finish.
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. When done, click **“Locate”** in the notification to open the folder with the APK.

This builds a **debug** APK by default. For release: **Build → Generate Signed Bundle / APK** and follow the wizard (you can use the existing `keyStore/release.keystore` and the passwords in `gradle.properties`).

---

## Quick reference

| Goal                    | Command               | APK location (typical)                    |
|-------------------------|-----------------------|-------------------------------------------|
| Debug APK (testing)     | `./gradlew assembleDebug`  | `app/build/outputs/apk/debug/`             |
| Release APK (signed)     | `./gradlew assembleRelease` | `app/build/outputs/apk/release/`            |
| Install on connected device | `./gradlew installDebug`   | Installs on phone, no file to copy         |

---

## If something fails

- **“SDK location not found”**  
  Add or fix `sdk.dir=...` in **`local.properties`** (see step 1).

- **“Permission denied” on `./gradlew`**  
  Run: `chmod +x gradlew` in the `Truckingapp` folder, then try again.

- **Java version error**  
  The project needs **Java 17**. In Terminal: `java -version`. If it’s older, install JDK 17 and set `JAVA_HOME` to it, or use Android Studio (it uses its own JDK).

- **Gradle / build errors**  
  Open the project in Android Studio once and let it sync; that often fixes Gradle/configuration issues. Then you can use the command line again.
