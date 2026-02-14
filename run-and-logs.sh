#!/bin/bash
# Run Truckspot app on connected device and stream logs (no Android Studio needed).
# Usage: ./run-and-logs.sh [install|logs|logs-all|both]
#   install   - build and install debug APK only
#   logs      - stream filtered logcat (app must be installed)
#   logs-all  - stream full logcat (to debug crashes)
#   both      - install then stream logs (default)

set -e
cd "$(dirname "$0")"
MODE="${1:-both}"

# Check for device
check_device() {
  if ! command -v adb &>/dev/null; then
    echo "adb not found. Add Android SDK platform-tools to PATH (e.g. export PATH=\$PATH:\$ANDROID_HOME/platform-tools)"
    exit 1
  fi
  if ! adb devices | grep -q 'device$'; then
    echo "No device/emulator connected. Connect a device with USB debugging enabled and run: adb devices"
    exit 1
  fi
}

case "$MODE" in
  install)
    check_device
    echo "Building and installing debug APK..."
    ./gradlew installDebug
    echo "Done. Launch the app on your device (Eagleye/Moeving)."
    ;;
  logs)
    check_device
    echo "Streaming logcat (Ctrl+C to stop). Filter: Truckspot, LogsFragment, AndroidRuntime, FATAL."
    adb logcat -c
    adb logcat | grep -E "LogsFragment|Truckspot|AndroidRuntime|FATAL|crash|Exception"
    ;;
  logs-all)
    check_device
    echo "Streaming full logcat (Ctrl+C to stop). Use this if crash is not in filtered logs."
    adb logcat
    ;;
  both)
    check_device
    echo "Building and installing..."
    ./gradlew installDebug
    echo "Starting logcat (Ctrl+C to stop)..."
    adb logcat -c
    adb logcat | grep -E "LogsFragment|Truckspot|AndroidRuntime|FATAL|crash|Exception"
    ;;
  *)
    echo "Usage: $0 [install|logs|logs-all|both]"
    echo "  install   - build and install APK"
    echo "  logs      - stream filtered app logs (Truckspot, crashes)"
    echo "  logs-all  - stream full logcat (see everything)"
    echo "  both      - install then stream logs (default)"
    exit 1
    ;;
esac
