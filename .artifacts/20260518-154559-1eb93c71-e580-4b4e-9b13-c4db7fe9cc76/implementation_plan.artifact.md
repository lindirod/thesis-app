# Persisting App State on Mobile and Smartwatch

Ensure that the application remains active and visible on both the handheld and Wear OS devices until the user explicitly closes it. This is achieved by preventing the screen from timing out and ensuring sensor processes remain active.

## User Review Required

- **Battery Impact**: Keeping the screen on and sensors active will significantly increase battery consumption on both devices.
- **Background Sensors**: On Wear OS, reading body sensors in the background requires the `BODY_SENSORS_BACKGROUND` permission, which may require user approval in system settings depending on the Android version.

## Proposed Changes

### Handheld App (Mobile)

Prevent the screen from turning off while the app is in the foreground.

#### [MainActivity.kt](file:///C:/Users/Linda/AndroidStudioProjects/Thesis/app/src/main/java/com/example/thesis/MainActivity.kt)

- Add `android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` to the window in `onCreate`.

### Wear OS App (Watch)

Ensure the sensor reading continues and the screen remains active.

#### [AndroidManifest.xml](file:///C:/Users/Linda/AndroidStudioProjects/Thesis/thesis/src/main/AndroidManifest.xml)

- Ensure `WAKE_LOCK` permission is present (already exists).
- Ensure `BODY_SENSORS` and `BODY_SENSORS_BACKGROUND` permissions are present (already exist).

#### [MainActivity.kt](file:///C:/Users/Linda/AndroidStudioProjects/Thesis/thesis/src/main/java/com/example/thesis/presentation/MainActivity.kt)

- Add `android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` to the window in `onCreate`.
- Implement a `WakeLock` to keep the CPU active while sensors are being read.

---

## Verification Plan

### Manual Verification
- **Screen Persistence**: Run the app on both devices and verify that the screen does not dim or turn off after the system timeout period.
- **Sensor Persistence (Watch)**: Verify that heart rate readings continue to be sent to the mobile app even if the watch screen dims (if `FLAG_KEEP_SCREEN_ON` is somehow bypassed by system battery savers).
- **Logcat Monitoring**:
    - `adb logcat -s Smartwatch` to verify continuous BPM data.
    - `adb logcat -s RecsLog` to verify the mobile app is still receiving data.
