# Run Truckspot Android App on a Real Device

Follow these steps to run the Kotlin app on your **physical Android phone** (no emulator).

---

## 1. Prerequisites

### Install Android Studio (easiest)
- Download: https://developer.android.com/studio  
- Install it. This gives you the Android SDK, Gradle, and device drivers.

### Or: Command-line only
- Install **Java 17** (required by the project).
- Install **Android SDK** (command-line tools): https://developer.android.com/studio#command-tools  
- Set `ANDROID_HOME` to your SDK path (e.g. `~/Library/Android/sdk` on Mac).

---

## 2. Point the project to your Android SDK

Edit **`local.properties`** in the `Truckingapp` folder and set your SDK path:

```properties
sdk.dir=/Users/macpro/Library/Android/sdk
```

- **Mac (default):** `sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk`  
- **Windows:** `sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk`  
- **Linux:** `sdk.dir=/home/YOUR_USERNAME/Android/Sdk`

If you use Android Studio, it will create/update this file when you open the project.

---

## 3. Enable USB debugging on your phone

1. On your Android device: **Settings → About phone**.  
2. Tap **Build number** 7 times to enable Developer options.  
3. Go back to **Settings → Developer options**.  
4. Turn on **USB debugging**.  
5. Connect the phone to your Mac with a USB cable.  
6. On the phone, when prompted “Allow USB debugging?”, tap **Allow** (and optionally “Always allow from this computer”).

---

## 4. Run the app

### Option A: Using Android Studio (recommended)

1. Open **Android Studio**.  
2. **File → Open** and select the **`Truckingapp`** folder (the one that contains `build.gradle`).  
3. Wait for Gradle sync to finish (first time may download dependencies).  
4. In the toolbar device dropdown, choose your **physical device** (e.g. “Pixel 6” or your phone model).  
5. Click the green **Run** button (or **Run → Run 'app'**).  
6. The app will build, install, and launch on your phone.

### Option B: Using the command line

1. Open Terminal.  
2. Go to the project:
   ```bash
   cd /Users/macpro/Documents/Truckspot/Truckingapp
   ```
3. Build and install the **debug** build on the connected device:
   ```bash
   ./gradlew installDebug
   ```
4. First run may take several minutes (Gradle download + dependencies).  
5. When it finishes, the app will be installed; open it from your phone’s app drawer (e.g. **“Eagleye”** or **“Moeving”**).

---

## 5. If your device is not listed

- Confirm the cable supports **data** (not charge-only).  
- Try another USB port or cable.  
- In Terminal run:
  ```bash
  adb devices
  ```
  You should see your device. If it shows “unauthorized”, check the phone for the USB debugging prompt and tap **Allow**.  
- Install/update device drivers if you’re on Windows.

---

## 6. Run from terminal and see logs (no Android Studio)

From the `Truckingapp` folder:

```bash
./run-and-logs.sh         # install app + stream filtered logs
./run-and-logs.sh install # only build and install
./run-and-logs.sh logs    # stream logs (Truckspot, crashes)
./run-and-logs.sh logs-all # full logcat (to debug crashes)
```

Requires `adb` in PATH (Android SDK platform-tools) and a device connected with USB debugging.

---

## 7. Build only (APK file, no install)

To just build the debug APK (e.g. to copy to another phone):

```bash
cd /Users/macpro/Documents/Truckspot/Truckingapp
./gradlew assembleDebug
```

The APK will be at:  
**`app/build/outputs/apk/debug/app-debug.apk`**

You can copy this file to your phone and install it (you may need to allow “Install from unknown sources” in Settings).

---

## Summary

| Step | Action |
|------|--------|
| 1 | Install Android Studio (or SDK + Java 17) |
| 2 | Set `sdk.dir` in `local.properties` |
| 3 | Enable USB debugging on your phone and connect via USB |
| 4 | Open `Truckingapp` in Android Studio and click **Run**, or run `./gradlew installDebug` in Terminal |

The app package name is **com.eagleeye** and it may appear as **“Eagleye”** or **“Moeving”** on your device.
