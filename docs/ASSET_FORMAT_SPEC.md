# ASSET_FORMAT_SPEC — SOLUM asset/project formats

## Цель

Все приложения SOLUM должны понимать одни и те же ассеты.

Нельзя, чтобы Material Studio, Asset Hub, AniStudio и SOLUM Engine писали разные JSON “как получится”.

## Стартовое решение

На старте asset = папка + `asset_manifest.json`.

Zip/bundle (`.solumasset`, `.solumscene`, `.solummat`) появится позже как import/export format.

Почему папка лучше на старте:

- легче смотреть через Termux;
- легче чинить;
- легче diff в Git;
- легче валидировать;
- легче читать другим AI;
- не нужно распаковывать для диагностики.

## Shared storage

Preferred root:

```text
/storage/emulated/0/SOLUMCreative/
```

Fallback:

```text
/storage/emulated/0/Download/SOLUMCreative/
```

Structure:

```text
SOLUMCreative/
  projects/
  assets/
    textures/
    meshes/
    materials/
    characters/
    animations/
    vfx/
    scenes/
    sounds/
    videos/
    worlds/
    mechanics/
  exports/
  diagnostics/
    latest/
    archive/
  reports/
    latest/
    archive/
  releases/
    latest/
    archive/
  inbox/
  temp/
```

## Asset Manifest v1

Минимальный v1:

```json
{
  "schema": "solum.asset",
  "schemaVersion": 1,
  "assetId": "uuid-v4",
  "assetType": "material",
  "assetSubType": "material_definition",
  "sourceFormat": "solum_material_json",
  "runtimeFormat": "solum_material_runtime",
  "displayName": "Metal Panels",
  "createdAt": "2026-05-05T12:00:00Z",
  "createdBy": "solum.material-studio",
  "fileList": [
    "material.json",
    "preview.png"
  ],
  "contentHashes": {
    "material.json": "sha256:...",
    "preview.png": "sha256:..."
  },
  "schemaCompatibleWith": ">=1.0 <2.0",
  "validationState": "pending",
  "dependencies": [],
  "metadata": {}
}
```

## Required fields

- `schema`
- `schemaVersion`
- `assetId`
- `assetType`
- `displayName`
- `createdAt`
- `createdBy`
- `fileList`
- `contentHashes`
- `validationState`

Recommended v1 fields:

- `assetSubType`
- `sourceFormat`
- `runtimeFormat`
- `metadata`

`dependencies` можно иметь пустым массивом в v1, но полноценный dependency graph строится позже.

## Asset type levels

SOLUM разделяет тип ассета на уровни:

```text
assetType     = крупная семья ассета
assetSubType  = точный тип внутри семьи
sourceFormat  = исходный формат файла
runtimeFormat = формат после обработки движком/tool pipeline
```

Примеры:

```json
{
  "assetType": "texture",
  "assetSubType": "image_2d",
  "sourceFormat": "png",
  "runtimeFormat": "ktx2_astc_later"
}
```

```json
{
  "assetType": "mesh",
  "assetSubType": "static_mesh",
  "sourceFormat": "glb",
  "runtimeFormat": "solum_static_mesh"
}
```

```json
{
  "assetType": "material",
  "assetSubType": "material_instance",
  "sourceFormat": "solum_material_instance_json",
  "runtimeFormat": "solum_material_runtime"
}
```

```json
{
  "assetType": "sound",
  "assetSubType": "music",
  "sourceFormat": "mp3",
  "runtimeFormat": "decoded_audio_runtime"
}
```

```json
{
  "assetType": "video",
  "assetSubType": "cutscene_render",
  "sourceFormat": "mp4",
  "runtimeFormat": "android_media_runtime"
}
```

## Asset types

Initial/future families:

```text
texture
mesh
material
character
animation
vfx
scene
sound
video
world
mechanic
diagnostic
shader
font
ui
prefab
```

## Validation states

```text
valid
invalid
pending
incompatible
missing_file
```

Каждое состояние должно иметь:

- status code;
- readable message;
- machine-readable validation report.

## Import sandbox

Импорт не должен сразу попадать в main assets.

Правильный flow:

```text
copy to imported_pending/
↓
validate schema
↓
validate file list
↓
validate hashes
↓
validate compatibility
↓
if OK → move to assets/type/id/
↓
write import_report.json
```

## Transaction save

Нельзя напрямую перезаписывать asset/project files.

Algorithm:

```text
serialize to temp
↓
validate temp
↓
backup current
↓
atomic replace
↓
write save_report.json
```

## Preview rule

Asset Hub v1 показывает:

- `preview.png`, если есть;
- fallback colored type icon, если preview нет.

Это не throwaway, потому что fallback asset cards останутся частью Asset Hub.

## Future bundle formats

Later:

```text
.solumasset
.solummat
.solumchar
.solumanim
.solumvfx
.solumscene
.solumworld
.solummech
```

Внутри bundle будет та же folder structure + manifest.

## Migration

Нельзя менять schema без migration plan.

Future migration flow:

```text
open old schema
↓
backup original
↓
run migration
↓
validate migrated
↓
write migration_report.json
```

## Asset Hub card v1

Минимальная карточка:

```text
[preview / fallback icon]
[asset type icon + label]
displayName
schema v1 · status
```

Остальное в details view:

- path;
- size;
- hashes;
- dependencies;
- validation report;
- used by.

## Rules

- Никаких случайных JSON без schema/version.
- Каждый asset имеет manifest.
- Каждый файл в manifest имеет content hash.
- Asset write должен быть transaction-safe.
- Import идёт через sandbox.
- Zip/bundle не использовать как primary editing format на старте.
- Не кодировать `png/wav/mp3/mp4/static mesh/material instance` только через `assetType`.
- Для точного типа использовать `assetSubType`, `sourceFormat`, `runtimeFormat`.
