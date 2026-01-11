## Designing an `exportXyz()` flow for a Compose app (Android + JVM)

This document describes one practical way to implement an **Export** button that:

1. lets the user pick **where to export**,
2. gives you back a “path-like” identifier (or better: a `PlatformFile`), and
3. calls your existing export/writer function.

The examples below use **FileKit**, since the link you shared is FileKit’s docs and it provides a single API across Android + JVM. FileKit’s file saver dialog returns a `PlatformFile` and **you** write the data to it (it does not auto-create/write the file for you). ([filekit.mintlify.app][1])

---

## Why this approach works well on Android + JVM

* FileKit is explicitly built to provide a consistent API across platforms including **Android and JVM (desktop)**. ([filekit.mintlify.app][2])
* The **File saver dialog** (`openFileSaver`) returns a `PlatformFile?` representing the destination; then you write bytes via `file.write(bytes)`. ([filekit.mintlify.app][1])
* `PlatformFile` exposes `path` (string) and other useful metadata (name, extension, etc.). ([filekit.mintlify.app][3])

---

## Setup checklist (Android + JVM)

### Dependencies

Add FileKit dialogs (choose one flavor):

* **Without Compose utilities**: `filekit-dialogs`
* **With Compose utilities**: `filekit-dialogs-compose`

The FileKit docs show these artifacts (example version shown in their docs). ([filekit.mintlify.app][4])

### Initialization

**Android**

* If you use `filekit-dialogs`, you must call `FileKit.init(this)` in your `MainActivity` so it can access the `ActivityResultRegistry`. ([filekit.mintlify.app][4])
* (Per docs) this init step is not needed when using `filekit-dialogs-compose`. ([filekit.mintlify.app][4])

**JVM (Desktop)**

* Initialize with an `appId`, typically in `main()`, before creating your window. ([filekit.mintlify.app][4])

---

## Key design choice: pass `PlatformFile`, not “just a path”

On JVM, a “path” is usually a normal filesystem path.
On Android, the destination may be represented by a `content://...` URI under the hood; FileKit still exposes `PlatformFile.path` as a `String`, but you should treat it as an **opaque identifier**, not guaranteed to be a real filesystem path.

**Recommendation:** design your export pipeline to accept a `PlatformFile` (or bytes + `PlatformFile`) instead of requiring a raw path string.

`PlatformFile` is meant to be the cross-platform abstraction, and includes `path`, `name`, `write(...)`, etc. ([filekit.mintlify.app][3])

---

## A clean `exportXyz()` API

### Outcome model (optional but nice)

```kotlin
sealed interface ExportResult {
    data object Cancelled : ExportResult
    data class Success(val destination: PlatformFile) : ExportResult
    data class Failed(val error: Throwable) : ExportResult
}
```

### `exportXyz()` (common code)

This version:

* opens the save dialog
* calls your “build bytes” function
* writes the bytes
* then calls your follow-up function with the destination

```kotlin
import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKit // package names may vary by module

suspend fun exportXyz(
    suggestedName: String,
    extension: String,
    buildBytes: suspend () -> ByteArray,
    afterExport: suspend (destination: PlatformFile) -> Unit,
): ExportResult {
    return try {
        // 1) user chooses destination
        val destination: PlatformFile? = FileKit.openFileSaver(
            suggestedName = suggestedName,
            extension = extension,
        )
        if (destination == null) return ExportResult.Cancelled

        // 2) produce data
        val bytes = buildBytes()

        // 3) write to chosen location (developer responsibility)
        destination.write(bytes)

        // 4) call follow-up
        afterExport(destination)

        ExportResult.Success(destination)
    } catch (t: Throwable) {
        ExportResult.Failed(t)
    }
}
```

This flow matches FileKit’s documented behavior: `openFileSaver(...)` returns a `PlatformFile?`, and you write using `write()`. ([filekit.mintlify.app][1])

---

## Wiring it to a Compose button

```kotlin
@Composable
fun ExportButton(
    buildBytes: suspend () -> ByteArray,
    onExported: suspend (PlatformFile) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var lastResult by remember { mutableStateOf<ExportResult?>(null) }

    Button(
        onClick = {
            scope.launch {
                lastResult = exportXyz(
                    suggestedName = "my-export",
                    extension = "xyz",
                    buildBytes = buildBytes,
                    afterExport = onExported
                )
            }
        }
    ) {
        Text("Export")
    }

    // Optional: show status in UI
    when (val r = lastResult) {
        is ExportResult.Success -> Text("Saved to: ${r.destination.path}")
        ExportResult.Cancelled -> Text("Export cancelled")
        is ExportResult.Failed -> Text("Export failed: ${r.error.message}")
        null -> {}
    }
}
```

`PlatformFile.path` exists and is useful for display/logging. ([filekit.mintlify.app][3])

---

## If your “other function” *requires* a string path

If you absolutely must pass a string, pass `destination.path` — but treat it as an identifier:

```kotlin
afterExportPath(destination.path)
```

Where `afterExportPath(...)` should NOT assume it can do raw `java.io.File(path)` on Android. If it needs to write/read, it should accept `PlatformFile` instead.

---

## Variant: user picks a directory, you choose the filename

If your UX is “pick a folder” then create the file inside it, you can do:

```kotlin
val dir: PlatformFile? = FileKit.openDirectoryPicker()
if (dir != null) {
    val destination = PlatformFile(dir, "my-export.xyz")
    destination.write(bytes)
}
```

FileKit’s docs show directory picking and creating/writing files via `PlatformFile`. ([filekit.mintlify.app][2])

---

## Notes for real apps (things you’ll be happy you handled)

* **Cancellation:** `openFileSaver(...)` returns `null` when the user cancels. ([filekit.mintlify.app][1])
* **Large exports:** consider streaming with `sink()` instead of holding the whole export in memory. ([filekit.mintlify.app][5])
* **Android storage model:** don’t rely on filesystem paths; keep operations in terms of `PlatformFile` and FileKit read/write APIs. (`PlatformFile` exists specifically to abstract this.) ([filekit.mintlify.app][3])

---

If you tell me what “xyz” is (single file vs folder export, expected size, and whether your “other function” needs read-back), I can tailor the `exportXyz()` signature to fit your exact pipeline.

[1]: https://filekit.mintlify.app/dialogs/file-saver "File saver dialog - FileKit"
[2]: https://filekit.mintlify.app/ "FileKit"
[3]: https://filekit.mintlify.app/core/platform-file "PlatformFile - FileKit"
[4]: https://filekit.mintlify.app/dialogs/setup "Setup FileKit Dialogs - FileKit"
[5]: https://filekit.mintlify.app/core/write-file "Writing files - FileKit"
