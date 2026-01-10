## Project Overview

This is a Kotlin Multiplatform project for creating programmatic videos using Compose Multiplatform. The project is called "Vidster" (com.olcayaras.vidster) and enables creating videos through a declarative DSL with text-to-speech, animations, and timeline-based sequencing.

## Documentation

- [Build System](BUILD_SYSTEM.md) - Gradle configuration, convention plugins, build commands, and dependencies
- [Videoscript Framework](VIDEOSCRIPT_FRAMEWORK.md) - Core video framework, DSL usage, and architectural concepts
- [Pivot MVP Features](pivot-mvp-features.md) - Feature roadmap for the Pivot Animator clone

## Module Architecture

The project is organized into several modules (see [VIDEOSCRIPT_FRAMEWORK.md](VIDEOSCRIPT_FRAMEWORK.md) for detailed framework documentation):

### Application Modules
- **app/composeApp** - Main multiplatform UI application
  - Uses all custom convention plugins
  - Targets: Android, Desktop (JVM), iOS, JS, Wasm
  - Depends on Koin for DI, Ktor client, Molecule for reactive state

- **app/backend** - Ktor-based backend server (JVM only)
  - Uses standard plugin configuration (not convention plugins)
  - Ktor server with Netty, auth, JWT, content negotiation
  - Main class: `org.company.backend.ApplicationKt`

- **app/shared** - Shared code between app and backend

- **app/iosApp** - iOS native wrapper

## Current Project: Pivot Animator Clone

The project is pivoting to build a stick figure animation tool (Pivot Animator clone) with AI integration. This is a 2-week MVP focusing on hierarchical figure animation.

### Figures Module (`figures/`)

The core animation framework for stick figure animations:

**Data Model:**
- `Joint` - Hierarchical node with id, length, angle (radians), and children
  - Represents connection points in a figure (e.g., shoulder, elbow, wrist)
  - Angles are relative to parent for natural parent-child transformations
- `Figure` - Complete stick figure with name, root joint, and x/y position
- `FigureFrame` - Snapshot containing figures, viewport, and viewport transitions
- `Segment` - Compiled joint for rendering (startX, startY, length, angle)
- `SegmentFrame` - Compiled frame ready for rendering (segments + viewport)

**Viewport System:**
- Controls camera (offsetX, offsetY, scale, rotation)
- Supports lerp interpolation for smooth camera movements
- Viewport transitions (None, Lerp)

**Rendering:**
- `SegmentFrameCanvas` - Non-interactive Compose canvas for rendering frames
  - Supports screen size scaling for consistent rendering across sizes
  - Draws segments as lines with rounded caps and joint circles
  - Applies viewport transformations around canvas center

### Editor UI (`com.olcayaras.vidster.ui.screens.editor`)

The animation editor interface:

**Layout:**
- Left: Timeline column (frame thumbnails, selection)
- Center: Main canvas with top toolbar (Play, Text, Add, Tools)
- Right: Properties panel

**State Management:**
- `EditorViewModel` - Manages frames list, selected frame, screen size
- `EditorEvent` - User actions (SelectFrame, AddFrame, RemoveFrame)
- `EditorState` - Current editor state

**Architecture:**
- Uses custom `ViewModel` base class with Decompose ComponentContext
- Molecule-style reactive state with Flow-based events
- Frame rendering uses the `figures` module's rendering system

### MVP Roadmap (from pivot-mvp-features.md)

**Week 1 (Essential):**
- ✓ Line segments and hierarchical figure model
- ✓ Basic rendering system
- Frame timeline and keyframes
- Joint dragging (Forward Kinematics)
- Playback controls
- Save/Load JSON

**Week 2 (High-Value):**
- Onion skinning (show previous frames at low alpha)
- Linear inbetweening between keyframes
- Undo/redo stack
- Circle segments for figure expressiveness
- **Koog AI integration** for smart inbetweening and pose suggestions

**Deferred:**
- Virtual camera animations (viewport is ready, UI needed)
- Bendy segments, polygons, sprites
- GIF/video export (PNG sequence sufficient for MVP)
- Auto-easing, gradient backgrounds
- Figure builder UI

### AI Integration Points

Planned integration with Koog AI agent:
1. **Smart Inbetweening** - "Generate 5 frames between these two poses"
2. **Pose Suggestions** - "Make this figure walk" or "Raise the left arm"
3. **Figure Generation** - "Create a stickman holding a sword"
4. **Animation Assistance** - "Make this animation smoother" or "Add anticipation"
