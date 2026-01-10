# Pivot Animator Clone - MVP Feature Set

A filtered feature set for a 2-week development timeline with Koog AI agent integration.

---

## Tier 1: Essential (Week 1)

| Feature | Effort | Rationale |
|---------|--------|-----------|
| Line segments | Easy | Core building block for all figures |
| Hierarchical figure model | Medium | The defining characteristic - connected limbs with parent-child relationships |
| Joint dragging (Forward Kinematics) | Medium | Primary user interaction for posing |
| Basic timeline + keyframes | Easy-Medium | Need frames to create animation |
| Playback | Easy | Must be able to preview the animation |
| Solid color background | Easy | Simplest background option |
| Save/Load project (JSON) | Easy | Essential for not losing work |

---

## Tier 2: High-Value Additions (Week 2)

| Feature | Effort | Rationale |
|---------|--------|-----------|
| Onion skinning | Easy | Massive UX improvement for animators, just render previous frame at low alpha |
| Inbetweening | Medium | Core animation feature - **prime candidate for Koog AI integration** |
| Undo/redo | Easy-Medium | Essential for usability |
| Shape segments (circle/rectangle/ellipse/arc) | Easy | Low effort addition that adds figure expressiveness |

---

## Deferred (Post-MVP)

| Feature | Reason to Defer |
|---------|-----------------|
| Virtual camera | Adds complexity, not essential for basic animations |
| Bendy segments / polygons | Nice-to-have, not core functionality |
| Sprite images | Requires image loading, transforms, transparency handling |
| Text tool | Secondary feature |
| GIF/Video export | Platform-specific, external libraries needed - PNG sequence is sufficient for MVP |
| Auto-easing | Linear tweening is acceptable for MVP |
| Gradient backgrounds | Cosmetic enhancement |
| Figure builder UI | Can use pre-built figures or simple code-based creation initially |

---

## Koog AI Agent Integration Points

The AI agent can provide unique value in these areas:

### 1. Smart Inbetweening
- **Prompt**: "Generate 5 frames between these two poses"
- AI interprets start/end keyframes and produces natural intermediate poses

### 2. Pose Suggestions
- **Prompt**: "Make this figure walk" or "Raise the left arm"
- AI manipulates joint angles based on natural language commands

### 3. Figure Generation
- **Prompt**: "Create a stickman holding a sword"
- AI generates figure hierarchy with appropriate segments

### 4. Animation Assistance
- **Prompt**: "Make this animation smoother" or "Add anticipation before the jump"
- AI analyzes existing frames and suggests/applies improvements

---

## Week 1 Milestones

- [X] Figure data model + rendering
- [X] Joint manipulation (drag to rotate)
- [X] Canvas with single figure
- [X] Timeline UI (add/remove/select/duplicate/reorder frames)
- [X] Basic playback
- [ ] JSON serialization

## Week 2 Milestones

- [X] Onion skinning toggle (previous/next/both)
- [ ] Linear inbetweening
- [ ] Koog agent integration (at least one feature)
- [X] Undo/redo stack
- [X] Multiple figures support
- [ ] Polish and demo preparation
