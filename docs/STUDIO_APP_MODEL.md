# STUDIO_APP_MODEL — model for future SOLUM Studio apps

Этот файл фиксирует общую модель будущих Studio-приложений: AniStudio/Cutscene Studio, Material Studio, VFX Studio, Motion Studio, Character Studio и другие.

## Main principle

Studio app is not just a form with parameters. It is a focused production tool with:

- asset model;
- preview;
- editing workflow;
- save/validate;
- export or send-to-engine flow;
- diagnostics.

## Modes

Каждое Studio app может работать в трёх режимах:

### 1. Standalone mode

Приложение создаёт результат само по себе.

Example:

```text
AniStudio exports MP4
Material Studio exports material asset
Sound Studio exports audio clip
```

### 2. SOLUM Project mode

Приложение работает с конкретным SOLUM project.

Example:

```text
Cutscene Studio opens project
↓
uses project characters/assets/sound/scene
↓
saves cutscene asset back to project
```

### 3. Export mode

Приложение экспортирует наружу.

Example:

```text
GLB / VRM / PNG / MP4 / MP3 / JSON / SOLUM bundle
```

## Mixed scene model

Для AniStudio/Cutscene Studio и похожих tools нужна mixed scene:

```text
3D character layer
2D/2.5D background layer
sprite/VFX layer
camera track
timeline track
audio track
export target
```

Sprite is one asset type, not the whole pipeline.

For long scenes, fights, different angles and character interaction, main path should be 3D characters with skeletons.

## Cutscene Studio minimal vertical slice

Do not start with AI/mocap/OpenCV.

Start with:

```text
scene objects
↓
timeline
↓
camera keyframes
↓
preview playback
↓
export/report
```

## OpenCV / AI / ML as assist layer

OpenCV, AI and ML are assist layers, not foundation.

Correct flow:

```text
working editor/runtime exists
↓
assistant analyzes reference/video
↓
assistant suggests parameters/motion
↓
user edits manually
↓
result saved as normal SOLUM asset
```

Wrong flow:

```text
start with AI magic
↓
no editor/runtime foundation
↓
no manual correction path
↓
bad UX and dead end
```

## Reference analyzer idea

Future Character/AniStudio can use OpenCV for:

- face detection;
- silhouette analysis;
- color extraction;
- body proportion estimate;
- motion from video later.

But output must be editable:

```text
reference image
↓
analysis
↓
character_profile.json
↓
apply to editable character parameters
↓
manual correction
```

## Studio app v1 rule

First version of any Studio app must be narrow but complete.

Examples:

### Material Studio v1

```text
material asset
preview object
baseColor/roughness/metallic
live preview
save/validate
```

No node graph in v1.

### VFX Studio v1

```text
VfxClip asset
SpriteEmitter
preview
few parameters
save/validate
```

No huge graph in v1.

### Cutscene Studio v1

```text
scene object list
timeline
camera keyframes
preview playback
save/validate
```

No AI/mocap in v1.

## UI rule

Each Studio app uses the same platform UI structure:

```text
Status Bar
↓
Preview/Viewport
↓
Context Toolbar
↓
Bottom Sheet Inspector
↓
Bottom Navigation
```

App accent color changes, component behavior stays consistent.

## Do not

- Do not create a giant MainActivity.
- Do not create many empty tabs.
- Do not use sprites as the only long-term animation strategy.
- Do not start with mocap/OpenCV before viewer/editor foundation.
- Do not hide export/save/validation behind manual file hunting.
