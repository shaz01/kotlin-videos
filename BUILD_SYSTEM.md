# Build System

The project uses Gradle with Kotlin DSL and custom convention plugins located in `build-logic/`.

## Convention Plugins

Located in `build-logic/convention/src/main/kotlin/`:
- `KmpBasePlugin.kt` - Base Kotlin Multiplatform configuration with target selection via gradle.properties
- `ComposeBasePlugin.kt` - Compose Multiplatform setup
- `ComposeDecomposePlugin.kt` - Decompose navigation library integration
- `AndroidLibraryPlugin.kt` - Android library configuration
- `KmpSerializationPlugin.kt` - kotlinx.serialization setup

Key convention plugin IDs used in build files:
- `convention.kmp.base` - Apply to KMP modules
- `convention.compose.base` - Apply for Compose support
- `convention.compose.decompose` - Apply for Decompose navigation
- `convention.android.library` - Apply to Android library modules
- `convention.kmp.serialization` - Apply for kotlinx.serialization

## Platform Targeting

Platform targets are controlled in `gradle.properties`:
```
kmp.enable.jvm=true
kmp.enable.android=true
kmp.enable.js=false
kmp.enable.wasm=false
kmp.enable.ios=false
kmp.enable.dev.target.optimization=true
```

Current configuration focuses on JVM and Android platforms only.

## Version Catalogs

The project uses multiple version catalogs:
- `libs.versions.toml` - Main dependencies (Compose, Coroutines, Koin, etc.)
- `ktor.versions.toml` - Ktor client/server dependencies (accessed via `ktorhttp` catalog)
- `decompose.versions.toml` - Decompose navigation library

## Common Build Commands

### Build and Run

```bash
# Desktop application (hot reload enabled)
./gradlew :composeApp:runHot

# Desktop application (standard)
./gradlew :composeApp:run

# Android debug APK
./gradlew :composeApp:assembleDebug
# Output: composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Backend server
./gradlew :app:backend:run
```

### Testing

```bash
# Run all tests
./gradlew test

# Desktop/JVM tests
./gradlew :composeApp:jvmTest

# Android instrumented tests (requires connected device)
./gradlew :composeApp:connectedDebugAndroidTest

# iOS simulator tests
./gradlew :composeApp:iosSimulatorArm64Test

# Browser tests (experimental)
./gradlew :composeApp:jsBrowserTest
./gradlew :composeApp:wasmJsBrowserTest
```

### Development

```bash
# Run browser app with continuous build (experimental)
./gradlew :composeApp:jsBrowserDevelopmentRun --continue
./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continue
```

## Dependencies and Requirements

- JDK 17 or higher required
- `local.properties` must contain Android SDK path for Android builds
- Native dependencies:
  - LWJGL 3.3.3 (platform-specific natives for Linux, macOS arm64, Windows)
  - JavaCV 1.5.11 platform bundle
  - MP3 decoder libraries (soundlibs)

## Project Configuration

- **Project ID**: `com.olcayaras.vidster` (defined in `build-logic/convention/src/main/kotlin/ProjectId.kt`)
- **Desktop app main class**: `MainKt`
- **Gradle configuration**: Uses configuration cache, parallel execution, 4GB heap
- **Kotlin toolchain**: JDK 17

## Hot Reload

The project has Compose Hot Reload commented out but configured. To enable:
1. Uncomment hot reload plugin in relevant build.gradle.kts files
2. Use `./gradlew :composeApp:runHot` for desktop
3. See comments referencing https://github.com/JetBrains/compose-hot-reload

## Testing Setup

Tests use:
- `kotlin("test")` for common tests
- Compose UI Test (`compose.uiTest`) for UI testing
- `kotlinx-coroutines-test` for coroutine testing
- JUnit for JVM tests
- Android instrumentation runner for Android tests