# Videoscript Framework

## Core Video Framework

- **videoscript-core** - Core video building framework with multiplatform support (JVM, JS)
  - Provides `buildVideo()` DSL for creating video definitions
  - Timeline-based sequencing system with `SequenceScope`
  - Text-to-speech integration via `TTSProvider` interface
  - Audio definitions (TTS and resource-based)
  - Platform-specific implementations using `expect/actual`
  - JVM uses LWJGL for audio/graphics (OpenAL, STB) and JavaCV for video processing
  - Includes `elevenlabs-integration` submodule for TTS

## Video Tools

- **videoscript-previewer** - Real-time video preview component (JVM, JS)
  - `VideoController` for playback control
  - `VideoPlayerUI` for UI components
  - Desktop window support via `videoPlayerWindow()`

- **videoscript-rendering** - Video export functionality (JVM only)
  - `exportVideo()` function for rendering to file
  - `FrameRenderer` for Compose-to-frame conversion
  - FFmpeg integration for video encoding

## Key Architectural Concepts

### Video Definition DSL

The core pattern uses a builder DSL to define videos. All functions use capital letters (e.g., `Sequence`, `TTS`, `Subsequence`):

```kotlin
buildVideo(ttsProvider, fps = 60) {
    // Inside SequenceScope context

    // Add a visual sequence with timing
    Sequence(
        start = SequenceStart.AfterPrevious,  // or SequenceStart.FixedPoint(duration)
        end = SequenceEnd.BeforeNext(),        // or FixedDuration, FixedPoint, UntilEnd
        enter = fadeIn(),
        exit = fadeOut(),
        tag = null
    ) {
        // Composable content (receives SequenceAnimationScope)
        Text("Hello")
    }

    // Add text-to-speech with word-synchronized sequences
    TTS("Hello world, this is a test") {
        // Inside SequenceWithTTSScope - sequences can trigger on specific words

        Sequence("Hello") {
            // Shows when "Hello" is spoken
            Icon(Icons.WavingHand)
        }

        Sequence("test", end = SequenceEnd.BeforeNext()) {
            // Shows when "test" is spoken
            Text("Testing...")
        }

        // Get subtitle for current moment
        val subtitle = getSubtitle()

        // Check if specific text is being spoken (Composable context)
        if (isActive("world")) {
            // ...
        }

        // Get time range of specific text
        val range = rangeOf("world")  // returns ClosedRange<Duration>
    }

    // Nested scope for grouping
    Subsequence(
        start = SequenceStart.AfterPrevious,
        end = SequenceEnd.BeforeNext()
    ) {
        // Another SequenceScope
        Sequence { /* ... */ }
    }

    // Add video with audio track
    WithVideo(
        videoPath = "/path/to/video.mp4",
        mediaStart = 0.seconds,
        mediaEnd = MediaEnd.UntilEnd,
        start = { videoLength -> SequenceStart.AfterPrevious },
        end = { videoLength -> SequenceEnd.FixedDuration(videoLength) }
    ) { video ->
        // SequenceScope with video resource
        Sequence {
            // Use video resource
        }
    }

    // Convenience function for video sequences
    VideoSequence("/path/to/video.mp4") { video ->
        // Composable content with video
    }
}
```

### Timing System

**SequenceStart** options:
- `SequenceStart.AfterPrevious` - Start after all previous items
- `SequenceStart.FixedPoint(duration)` - Start at specific time

**SequenceEnd** options:
- `SequenceEnd.FixedDuration(duration)` - Run for specific duration
- `SequenceEnd.FixedPoint(duration)` - End at specific absolute time
- `SequenceEnd.BeforeNext(minimumLength)` - End when next item starts (with minimum)
- `SequenceEnd.UntilEnd(extra)` - Run until video end

### Timeline System

Located in `videoscript-core/src/commonMain/kotlin/com/olcayaras/lib/videobuilders/Timeline.kt`:
- `TimelineBuilder` manages component timing and resolves relative timings to absolute
- Components are resolved to absolute timings during build
- `BeforeNext` endings are resolved when the next component is added
- `UntilEnd` endings are resolved at final build time

### TTS Integration

- `TTSProvider` interface defines text-to-speech capabilities
- `CachedTTSProvider` wrapper caches TTS results (use `.asCachedTTSProvider()`)
- `SpeechWithTimestamps` contains audio data and word-level timing
- Inside `TTS { }` blocks, sequences can be synchronized to specific spoken words
- `SequenceWithTTSScope.Sequence(triggerText)` automatically times sequences to when words are spoken

### Platform-Specific Audio

Audio handling uses expect/actual:
- Common: `AudioController.kt` defines interface
- JVM: Uses LWJGL OpenAL (`AudioController.jvm.kt`, `AudioContext.kt`)
- JS: Browser-based implementation (`AudioController.js.kt`)

### Video Export Pipeline

1. Build video definition with `buildVideo(ttsProvider, fps, content)`
2. Create `FrameRendererSimple` with screen size and density
3. Use `VideoRenderer` with audio definitions
4. Render each frame as Composable with composition locals:
   - `LocalCurrentFrame` - current frame number
   - `LocalFPS` - frames per second
   - `LocalIsRendering` - true during export, false during preview
5. Export via FFmpeg to output path using `VideoRenderer.exportVideo()`