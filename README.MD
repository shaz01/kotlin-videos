# Vidster (Compose Multiplatform)

## Before running!
 - check your system with [KDoctor](https://github.com/Kotlin/kdoctor)
 - install JDK 17 or higher on your machine
 - add `local.properties` file to the project root and set a path to Android SDK there

### Android
To run the application on android device/emulator:  
 - open project in Android Studio and run imported android run configuration

To build the application bundle:
 - run `./gradlew :app:composeApp:assembleDebug`
 - find `.apk` file in `app/composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Run android UI tests on the connected device: `./gradlew :app:composeApp:connectedDebugAndroidTest`

### Desktop
Run the desktop application: `./gradlew :app:composeApp:run`
Run the desktop **hot reload** application: `./gradlew :app:composeApp:runHot`
Run desktop UI tests: `./gradlew :app:composeApp:jvmTest`

### iOS
To run the application on iPhone device/simulator:
 - Open `iosApp/iosApp.xcproject` in Xcode and run standard configuration
 - Or use [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) for Android Studio
Run iOS simulator UI tests: `./gradlew :app:composeApp:iosSimulatorArm64Test`

### JS Browser (Experimental)
Run the browser application: `./gradlew :app:composeApp:jsBrowserDevelopmentRun --continue`
Run browser UI tests: `./gradlew :app:composeApp:jsBrowserTest`

### Wasm Browser (Alpha)
Run the browser application: `./gradlew :app:composeApp:wasmJsBrowserDevelopmentRun --continue`
Run browser UI tests: `./gradlew :app:composeApp:wasmJsBrowserTest`

### Target toggles
Multiplatform targets are controlled via `gradle.properties` (`kmp.enable.*`). By default, Android + JVM are enabled and iOS/JS/Wasm are disabled.
