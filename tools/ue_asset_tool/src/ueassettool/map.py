from __future__ import annotations

import hashlib
import json
import math
import struct
from pathlib import Path
from typing import Any, Iterable

from .contracts import verify_package
from .dataset import sha256_file, write_json
from .errors import BoundsError, FormatError, UEAssetError, UnsupportedError
from .package import UnrealPackage
from .properties import PropertyParser


MAP_SCHEMA = "ueassettool.map-contract/v1"
UE_SOURCE_REVISION = "0bcfaffa52e9557258dbd388835ac7668fe864a4"
UE_LEVEL_AUTHORITY = "Engine/Source/Runtime/Engine/Private/Level.cpp:ULevel::Serialize"
UE_WORLD_AUTHORITY = "Engine/Source/Runtime/Engine/Private/World.cpp:UWorld::Serialize"
UE_SCENE_DEFAULT_AUTHORITY = (
    "Engine/Source/Runtime/Engine/Private/Components/SceneComponent.cpp:"
    "USceneComponent::USceneComponent"
)
UE_LANDSCAPE_DEFAULT_AUTHORITY = (
    "Engine/Source/Runtime/Landscape/Private/Landscape.cpp:"
    "ALandscapeProxy::ALandscapeProxy"
)


def _region(path: Path, offset: int, size: int) -> dict[str, Any]:
    if offset < 0 or size < 0 or offset + size > path.stat().st_size:
        raise BoundsError(f"region 0x{offset:x}+{size} leaves {path}")
    digest = hashlib.sha256()
    with path.open("rb") as source:
        source.seek(offset)
        remaining = size
        while remaining:
            chunk = source.read(min(remaining, 1024 * 1024))
            if not chunk:
                raise BoundsError(f"short read in {path} at 0x{source.tell():x}")
            digest.update(chunk)
            remaining -= len(chunk)
    return {
        "source_file": str(path), "byte_offset": offset, "byte_length": size,
        "sha256": digest.hexdigest(),
    }


def _decoded_properties(decoded: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        str(item["name"]): item for item in decoded.get("properties", [])
        if str(item.get("decode_status", "")).startswith("decoded")
    }


def _object_index(prop: dict[str, Any] | None) -> int | None:
    if not prop or not isinstance(prop.get("value"), dict):
        return None
    value = prop["value"].get("package_index")
    return int(value) if isinstance(value, int) else None


def _object_array(prop: dict[str, Any] | None) -> list[int]:
    if not prop or not isinstance(prop.get("value"), dict):
        return []
    values = prop["value"].get("items")
    if not isinstance(values, list):
        return []
    return [
        int(item["package_index"]) for item in values
        if isinstance(item, dict) and isinstance(item.get("package_index"), int)
    ]


def _safe_vector(value: Any, labels: tuple[str, str, str]) -> list[float] | None:
    if not isinstance(value, dict):
        return None
    result: list[float] = []
    for label in labels:
        item = value.get(label)
        if not isinstance(item, (int, float)) or not math.isfinite(float(item)):
            return None
        result.append(float(item))
    return result


def rotator_to_quaternion(rotation: Iterable[float]) -> list[float]:
    """Exact scalar formula from UE FRotator3d::Quaternion."""
    pitch, yaw, roll = (math.fmod(float(value), 360.0) for value in rotation)
    sp, cp = math.sin(math.radians(pitch) / 2.0), math.cos(math.radians(pitch) / 2.0)
    sy, cy = math.sin(math.radians(yaw) / 2.0), math.cos(math.radians(yaw) / 2.0)
    sr, cr = math.sin(math.radians(roll) / 2.0), math.cos(math.radians(roll) / 2.0)
    return [
        cr * sp * sy - sr * cp * cy,
        -cr * sp * cy - sr * cp * sy,
        cr * cp * sy - sr * sp * cy,
        cr * cp * cy + sr * sp * sy,
    ]


def quaternion_norm(value: Iterable[float]) -> float:
    return math.sqrt(sum(float(item) * float(item) for item in value))


def quaternion_multiply(a: Iterable[float], b: Iterable[float]) -> list[float]:
    ax, ay, az, aw = map(float, a)
    bx, by, bz, bw = map(float, b)
    return [
        aw * bx + ax * bw + ay * bz - az * by,
        aw * by - ax * bz + ay * bw + az * bx,
        aw * bz + ax * by - ay * bx + az * bw,
        aw * bw - ax * bx - ay * by - az * bz,
    ]


def quaternion_rotate(q: Iterable[float], v: Iterable[float]) -> list[float]:
    x, y, z, w = map(float, q)
    vx, vy, vz = map(float, v)
    tx = 2.0 * (y * vz - z * vy)
    ty = 2.0 * (z * vx - x * vz)
    tz = 2.0 * (x * vy - y * vx)
    return [
        vx + w * tx + (y * tz - z * ty),
        vy + w * ty + (z * tx - x * tz),
        vz + w * tz + (x * ty - y * tx),
    ]


def compose_transforms(parent: dict[str, Any], child: dict[str, Any]) -> dict[str, Any]:
    if parent.get("status") != "VERIFIED" or child.get("status") != "VERIFIED":
        raise FormatError("world transform requires two VERIFIED transforms")
    scaled = [
        float(child["translation"][axis]) * float(parent["scale"][axis])
        for axis in range(3)
    ]
    rotated = quaternion_rotate(parent["quaternion"], scaled)
    quaternion = quaternion_multiply(parent["quaternion"], child["quaternion"])
    norm = quaternion_norm(quaternion)
    if not 0.999999 <= norm <= 1.000001:
        raise FormatError(f"composed quaternion norm {norm} is invalid")
    return {
        "status": "VERIFIED",
        "serialization_variant": "derived_parent_world_x_child_relative",
        "quaternion": quaternion,
        "translation": [
            float(parent["translation"][axis]) + rotated[axis] for axis in range(3)
        ],
        "scale": [
            float(parent["scale"][axis]) * float(child["scale"][axis])
            for axis in range(3)
        ],
        "finite": True,
        "quaternion_norm": norm,
        "composition": "parent world × child relative",
        "sources": [parent.get("owner_object"), child.get("owner_object")],
    }


def ue_to_renderer_position(value: Iterable[float]) -> list[float]:
    x, y, z = map(float, value)
    return [y * 0.01, z * 0.01, -x * 0.01]


def renderer_transform(transform: dict[str, Any]) -> dict[str, Any] | None:
    if transform.get("status") != "VERIFIED":
        return None
    return {
        "coordinate_system": "right-handed meters; UE left-handed centimeters converted as [Y,Z,-X]",
        "translation": ue_to_renderer_position(transform["translation"]),
        "quaternion": transform["quaternion"], "scale": transform["scale"],
        "source_chain": transform.get("sources", [transform.get("owner_object")]),
    }


def _property_provenance(prop: dict[str, Any]) -> dict[str, Any] | None:
    raw = prop.get("raw")
    if not isinstance(raw, dict):
        return None
    return {
        "byte_offset": raw.get("physical_offset"), "byte_length": raw.get("size"),
        "sha256": raw.get("sha256"),
    }


def _relative_transform(
    *,
    owner_object: str,
    owner_class: str,
    properties: dict[str, dict[str, Any]],
    exact_default_profile: str | None,
) -> dict[str, Any]:
    location_prop = properties.get("RelativeLocation")
    rotation_prop = properties.get("RelativeRotation")
    scale_prop = properties.get("RelativeScale3D")
    location = _safe_vector(location_prop.get("value") if location_prop else None, ("x", "y", "z"))
    rotation = _safe_vector(
        rotation_prop.get("value") if rotation_prop else None, ("pitch", "yaw", "roll")
    )
    scale = _safe_vector(scale_prop.get("value") if scale_prop else None, ("x", "y", "z"))
    defaults: list[dict[str, Any]] = []
    if exact_default_profile:
        if location is None:
            location = [0.0, 0.0, 0.0]
            defaults.append({
                "field": "RelativeLocation", "value": location,
                "basis": "native zero-initialized USceneComponent default",
                "authority": UE_SCENE_DEFAULT_AUTHORITY,
            })
        if rotation is None:
            rotation = [0.0, 0.0, 0.0]
            defaults.append({
                "field": "RelativeRotation", "value": rotation,
                "basis": "native zero-initialized USceneComponent default",
                "authority": UE_SCENE_DEFAULT_AUTHORITY,
            })
        if scale is None:
            scale = (
                [128.0, 128.0, 256.0]
                if exact_default_profile == "LANDSCAPE_ROOT" else [1.0, 1.0, 1.0]
            )
            defaults.append({
                "field": "RelativeScale3D", "value": scale,
                "basis": (
                    "ALandscapeProxy constructor compatibility scale"
                    if exact_default_profile == "LANDSCAPE_ROOT"
                    else "USceneComponent constructor identity scale"
                ),
                "authority": (
                    UE_LANDSCAPE_DEFAULT_AUTHORITY
                    if exact_default_profile == "LANDSCAPE_ROOT" else UE_SCENE_DEFAULT_AUTHORITY
                ),
            })
    missing = [
        name for name, value in (
            ("RelativeLocation", location), ("RelativeRotation", rotation),
            ("RelativeScale3D", scale),
        ) if value is None
    ]
    if missing:
        return {
            "status": "PARTIAL", "owner_object": owner_object, "owner_class": owner_class,
            "missing_fields": missing,
            "reason": "omitted fields depend on an unverified archetype/CDO default",
            "explicit": {
                "translation": location, "rotator_degrees": rotation, "scale": scale,
            },
        }
    assert location is not None and rotation is not None and scale is not None
    quaternion = rotator_to_quaternion(rotation)
    norm = quaternion_norm(quaternion)
    finite = all(math.isfinite(item) for item in location + rotation + scale + quaternion)
    status = "VERIFIED" if finite and 0.999999 <= norm <= 1.000001 else "INVALID"
    return {
        "status": status,
        "owner_object": owner_object,
        "owner_class": owner_class,
        "serialization_variant": {
            "translation": (
                location_prop["type"]["display"] if location_prop else "native_default"
            ),
            "rotation": (
                rotation_prop["type"]["display"] if rotation_prop else "native_default"
            ),
            "scale": scale_prop["type"]["display"] if scale_prop else "native_default",
        },
        "quaternion": quaternion,
        "rotator_degrees": rotation,
        "translation": location,
        "scale": scale,
        "finite": finite,
        "quaternion_norm": norm,
        "provenance": {
            "RelativeLocation": _property_provenance(location_prop) if location_prop else None,
            "RelativeRotation": _property_provenance(rotation_prop) if rotation_prop else None,
            "RelativeScale3D": _property_provenance(scale_prop) if scale_prop else None,
            "native_defaults": defaults,
        },
    }


def _read_first_object_array(
    package: UnrealPackage,
    decoded: dict[str, Any],
    *,
    label: str,
    authority: str,
    require_class: str | None = None,
) -> dict[str, Any]:
    trailing = decoded.get("trailing_native")
    if not isinstance(trailing, dict):
        raise UnsupportedError(f"{label} has no native serialization suffix")
    source = Path(package.exports[decoded["export_index"] - 1].payload_source or package.path)
    offset = int(trailing["physical_offset"])
    available = int(trailing["size"])
    if available < 4:
        raise BoundsError(f"{label} array count is truncated")
    with source.open("rb") as handle:
        handle.seek(offset)
        raw_count = handle.read(4)
        count = struct.unpack("<i", raw_count)[0]
        if not 0 <= count <= 10_000_000:
            raise FormatError(f"invalid {label} count {count}")
        size = 4 + count * 4
        if size > available:
            raise BoundsError(f"{label} array needs {size} bytes, only {available} remain")
        raw_indices = handle.read(count * 4)
    indices = list(struct.unpack(f"<{count}i", raw_indices)) if count else []
    items: list[dict[str, Any]] = []
    for index in indices:
        if not 1 <= index <= len(package.exports):
            raise FormatError(f"{label} index {index} is not a local export")
        export = package.exports[index - 1]
        if require_class and export.class_name != require_class:
            raise FormatError(f"{label} index {index} class {export.class_name} != {require_class}")
        items.append({
            "package_index": index, "object_path": package.object_path(index),
            "class": export.class_name,
        })
    return {
        "status": "VERIFIED", "count": count, "items": items,
        "provenance": _region(source, offset, size),
        "array_boundary": {"start": offset, "end": offset + size, "available_suffix_end": offset + available},
        "authority": authority,
    }


def _read_first_object_reference(
    package: UnrealPackage,
    decoded: dict[str, Any],
    *,
    label: str,
    authority: str,
    require_class: str | None = None,
) -> dict[str, Any]:
    trailing = decoded.get("trailing_native")
    if not isinstance(trailing, dict):
        raise UnsupportedError(f"{label} has no native serialization suffix")
    source = Path(package.exports[decoded["export_index"] - 1].payload_source or package.path)
    offset = int(trailing["physical_offset"])
    available = int(trailing["size"])
    if available < 4:
        raise BoundsError(f"{label} reference is truncated")
    with source.open("rb") as handle:
        handle.seek(offset)
        raw_index = handle.read(4)
    index = struct.unpack("<i", raw_index)[0]
    if not 1 <= index <= len(package.exports):
        raise FormatError(f"{label} index {index} is not a local export")
    export = package.exports[index - 1]
    if require_class and export.class_name != require_class:
        raise FormatError(f"{label} index {index} class {export.class_name} != {require_class}")
    return {
        "status": "VERIFIED",
        "item": {
            "package_index": index,
            "object_path": package.object_path(index),
            "class": export.class_name,
        },
        "provenance": _region(source, offset, 4),
        "reference_boundary": {
            "start": offset,
            "end": offset + 4,
            "available_suffix_end": offset + available,
        },
        "authority": authority,
    }


def detect_attachment_cycles(components: list[dict[str, Any]]) -> list[list[int]]:
    parents = {
        int(item["export_index"]): item.get("attachment_parent_index")
        for item in components
        if isinstance(item.get("export_index"), int)
    }
    cycles: list[list[int]] = []
    recorded: set[frozenset[int]] = set()
    for start in parents:
        order: list[int] = []
        positions: dict[int, int] = {}
        current: int | None = start
        while current in parents:
            if current in positions:
                cycle = order[positions[current]:]
                key = frozenset(cycle)
                if key not in recorded:
                    cycles.append(cycle)
                    recorded.add(key)
                break
            positions[current] = len(order)
            order.append(current)
            parent = parents[current]
            current = int(parent) if isinstance(parent, int) and parent > 0 else None
    return cycles


class MapContractBuilder:
    def __init__(self, path: str | Path):
        self.path = Path(path)

    def build(self) -> dict[str, Any]:
        with UnrealPackage(self.path) as package:
            parser = PropertyParser(package)
            decoded = {
                index: parser.parse_export(index) for index in range(1, len(package.exports) + 1)
            }
            worlds = [i for i, item in enumerate(package.exports, 1) if item.class_name == "World"]
            levels = [i for i, item in enumerate(package.exports, 1) if item.class_name == "Level"]
            if len(worlds) != 1 or len(levels) != 1:
                raise UnsupportedError(
                    f"map needs exactly one World and Persistent Level; found {worlds}, {levels}"
                )
            world_index, level_index = worlds[0], levels[0]
            persistent = _read_first_object_reference(
                package, decoded[world_index], label="UWorld.PersistentLevel",
                authority=UE_WORLD_AUTHORITY, require_class="Level",
            )
            if persistent["item"]["package_index"] != level_index:
                raise FormatError("UWorld PersistentLevel does not identify the unique Level export")
            actor_array = _read_first_object_array(
                package, decoded[level_index], label="ULevel.Actors", authority=UE_LEVEL_AUTHORITY,
            )
            actor_indices = [int(item["package_index"]) for item in actor_array["items"]]
            level_path = package.object_path(level_index)
            for item in actor_array["items"]:
                export = package.exports[item["package_index"] - 1]
                if export.outer_index != level_index:
                    raise FormatError(
                        f"ULevel actor {item['object_path']} outer {export.outer_index} != {level_index}"
                    )

            actor_for_export: dict[int, int] = {}
            for index in range(1, len(package.exports) + 1):
                current = index
                seen: set[int] = set()
                while current > 0 and current not in seen:
                    seen.add(current)
                    if current in actor_indices:
                        actor_for_export[index] = current
                        break
                    current = package.exports[current - 1].outer_index

            typed_members: set[int] = set()
            for actor_index in actor_indices:
                actor_props = _decoded_properties(decoded[actor_index])
                typed_members.update(_object_array(actor_props.get("InstanceComponents")))
                typed_members.update(_object_array(actor_props.get("BlueprintCreatedComponents")))
                root = _object_index(actor_props.get("RootComponent"))
                if root:
                    typed_members.add(root)

            components: list[dict[str, Any]] = []
            component_by_index: dict[int, dict[str, Any]] = {}
            for index, owner in sorted(actor_for_export.items()):
                export = package.exports[index - 1]
                typed = index in typed_members
                if not typed and (not export.class_name or not export.class_name.endswith("Component")):
                    continue
                props = _decoded_properties(decoded[index])
                exact_profile = None
                owner_class = package.exports[owner - 1].class_name
                if owner_class == "Landscape":
                    exact_profile = "LANDSCAPE_ROOT" if index == _object_index(
                        _decoded_properties(decoded[owner]).get("RootComponent")
                    ) else "NATIVE_SCENE_COMPONENT"
                transform = _relative_transform(
                    owner_object=package.object_path(index), owner_class=export.class_name,
                    properties=props, exact_default_profile=exact_profile,
                )
                attach = _object_index(props.get("AttachParent"))
                component = {
                    "export_index": index,
                    "object_path": package.object_path(index),
                    "class": export.class_name,
                    "inclusion_basis": "typed actor component reference" if typed else "class name suffix",
                    "owner_actor_index": owner,
                    "owner_actor": package.object_path(owner),
                    "outer_index": export.outer_index,
                    "attachment_parent_index": attach,
                    "attachment_parent": package.object_path(attach) if attach else None,
                    "attachment_socket": props.get("AttachSocketName", {}).get("value"),
                    "visibility": {
                        "visible": props.get("bVisible", {}).get("value"),
                        "hidden_in_game": props.get("bHiddenInGame", {}).get("value"),
                        "basis": "serialized properties only; omitted archetype values are not guessed",
                    },
                    "mobility": props.get("Mobility", {}).get("value"),
                    "local_transform": transform,
                    "world_transform": None,
                    "renderer_local_transform": renderer_transform(transform),
                    "provenance": _region(
                        Path(export.payload_source or package.path),
                        int(export.payload_physical_offset or 0), export.serial_size,
                    ),
                }
                components.append(component)
                component_by_index[index] = component

            cycles = detect_attachment_cycles(components)
            unresolved_parents = [
                {"component": item["object_path"], "parent_index": item["attachment_parent_index"]}
                for item in components
                if item["attachment_parent_index"] is not None
                and item["attachment_parent_index"] not in component_by_index
            ]

            def resolve_world(index: int, stack: set[int] | None = None) -> dict[str, Any] | None:
                component = component_by_index[index]
                if component["world_transform"] is not None:
                    return component["world_transform"]
                local = component["local_transform"]
                if local.get("status") != "VERIFIED":
                    return None
                parent = component["attachment_parent_index"]
                if not parent:
                    component["world_transform"] = {**local, "serialization_variant": "verified_root_relative_is_world"}
                    return component["world_transform"]
                if parent not in component_by_index:
                    return None
                active = set() if stack is None else set(stack)
                if index in active:
                    return None
                active.add(index)
                parent_world = resolve_world(parent, active)
                if parent_world is None:
                    return None
                world = compose_transforms(parent_world, local)
                world["owner_object"] = component["object_path"]
                world["renderer"] = renderer_transform(world)
                component["world_transform"] = world
                return world

            for index in component_by_index:
                resolve_world(index)

            actors: list[dict[str, Any]] = []
            for index in actor_indices:
                export = package.exports[index - 1]
                props = _decoded_properties(decoded[index])
                root = _object_index(props.get("RootComponent"))
                member_component_set = set(_object_array(props.get("InstanceComponents")))
                member_component_set |= set(_object_array(props.get("BlueprintCreatedComponents")))
                member_component_set |= {
                    item["export_index"]
                    for item in components
                    if item["owner_actor_index"] == index
                }
                if isinstance(root, int):
                    member_component_set.add(root)
                member_components = sorted(member_component_set)
                unresolved_members = [member for member in member_components if member not in component_by_index]
                actors.append({
                    "export_index": index,
                    "object_path": package.object_path(index),
                    "class": export.class_name,
                    "outer": level_path,
                    "owner": None,
                    "root_component_index": root,
                    "root_component": package.object_path(root) if root else None,
                    "component_indices": member_components,
                    "component_membership": {
                        "status": "VERIFIED" if not unresolved_members else "TERMINAL_UNRESOLVED",
                        "unresolved_indices": unresolved_members,
                    },
                    "tags": props.get("Tags", {}).get("value"),
                    "actor_label": props.get("ActorLabel", {}).get("value"),
                    "hidden": props.get("bHidden", {}).get("value"),
                    "world_transform": (
                        component_by_index[root]["world_transform"]
                        if root in component_by_index else None
                    ),
                    "provenance": _region(
                        Path(export.payload_source or package.path),
                        int(export.payload_physical_offset or 0), export.serial_size,
                    ),
                })

            landscape_actors = [item for item in actors if item["class"] == "Landscape"]
            landscapes: list[dict[str, Any]] = []
            for actor in landscape_actors:
                props = _decoded_properties(decoded[actor["export_index"]])
                component_indices = _object_array(props.get("LandscapeComponents"))
                component_records = []
                for component_index in component_indices:
                    cprops = _decoded_properties(decoded[component_index])
                    section_x = cprops.get("SectionBaseX", {}).get("value", 0)
                    section_y = cprops.get("SectionBaseY", {}).get("value", 0)
                    component_records.append({
                        "export_index": component_index,
                        "object_path": package.object_path(component_index),
                        "section_base": [int(section_x), int(section_y)],
                        "section_base_basis": {
                            "x": "serialized" if "SectionBaseX" in cprops else "native int32 zero default",
                            "y": "serialized" if "SectionBaseY" in cprops else "native int32 zero default",
                        },
                        "component_size_quads": cprops.get("ComponentSizeQuads", {}).get("value"),
                        "subsection_size_quads": cprops.get("SubsectionSizeQuads", {}).get("value"),
                        "num_subsections": cprops.get("NumSubsections", {}).get("value"),
                        "heightmap_texture_index": _object_index(cprops.get("HeightmapTexture")),
                        "heightmap_scale_bias": cprops.get("HeightmapScaleBias", {}).get("value"),
                        "weightmap_scale_bias": cprops.get("WeightmapScaleBias", {}).get("value"),
                        "relative_transform": component_by_index[component_index]["local_transform"],
                        "world_transform": component_by_index[component_index]["world_transform"],
                        "materials": [
                            props.get("LandscapeMaterial", {}).get("value"),
                            props.get("LandscapeHoleMaterial", {}).get("value"),
                        ],
                    })
                landscapes.append({
                    "actor_index": actor["export_index"], "object_path": actor["object_path"],
                    "landscape_guid": props.get("LandscapeGuid", {}).get("value"),
                    "root_transform": actor["world_transform"],
                    "materials": [
                        props.get("LandscapeMaterial", {}).get("value"),
                        props.get("LandscapeHoleMaterial", {}).get("value"),
                    ],
                    "components": component_records,
                })

            def transform_counts(key: str) -> dict[str, int]:
                counts = {"VERIFIED": 0, "PARTIAL": 0, "INVALID": 0, "MISSING": 0}
                for item in components:
                    value = item.get(key)
                    status = value.get("status") if isinstance(value, dict) else "MISSING"
                    counts[status if status in counts else "MISSING"] += 1
                return counts
            local_counts, world_counts = transform_counts("local_transform"), transform_counts("world_transform")
            membership_errors = [
                {"actor": actor["object_path"], "indices": actor["component_membership"]["unresolved_indices"]}
                for actor in actors if actor["component_membership"]["unresolved_indices"]
            ]
            layout_gate = package.summary.file_version_ue4 == 522 and package.summary.file_version_ue5 == 1013
            aggregate_status = "PARTIAL_VERIFIED" if (membership_errors or world_counts["PARTIAL"] or world_counts["MISSING"]) else "VERIFIED"
            contract = {
                "schema": MAP_SCHEMA,
                "status": aggregate_status,
                "source": {
                    "path": str(package.path), "size": package.path.stat().st_size,
                    "sha256": package.sha256, "package_name": package.summary.package_name,
                },
                "package_version": {
                    "file_version_ue4": package.summary.file_version_ue4,
                    "file_version_ue5": package.summary.file_version_ue5,
                    "saved_engine": (
                        package.summary.saved_by_engine_version.display
                        if package.summary.saved_by_engine_version else None
                    ),
                    "custom_versions": package.summary.custom_versions,
                },
                "serialization_authority": {
                    "ue_source_revision": UE_SOURCE_REVISION,
                    "world": UE_WORLD_AUTHORITY, "level": UE_LEVEL_AUTHORITY,
                },
                "layout_gate": {"status": "VERIFIED" if layout_gate else "UNSUPPORTED", "ue4": 522, "ue5": 1013},
                "world": {
                    "export_index": world_index, "object_path": package.object_path(world_index),
                    "persistent_level": persistent,
                },
                "persistent_level": {
                    "export_index": level_index, "object_path": level_path,
                    "actor_array": actor_array,
                },
                "actors": actors,
                "components": components,
                "landscapes": landscapes,
                "validation": {
                    "world_count": len(worlds), "level_count": len(levels),
                    "actor_count": len(actors), "component_count": len(components),
                    "local_transform_counts": local_counts,
                    "world_transform_counts": world_counts,
                    "actor_component_membership_errors": membership_errors,
                    "unresolved_parents": unresolved_parents,
                    "attachment_cycles": cycles,
                    "actor_array_exact_serialized_basis": True,
                },
            }
            return contract


def inventory_maps(roots: Iterable[str | Path], output: str | Path | None = None) -> dict[str, Any]:
    paths: dict[Path, None] = {}
    roots_list = [Path(value) for value in roots]
    for root in roots_list:
        if root.is_file() and root.suffix.lower() == ".umap":
            paths[root.resolve()] = None
        elif root.is_dir():
            for path in root.rglob("*.umap"):
                paths[path.resolve()] = None
    candidates: list[dict[str, Any]] = []
    for path in sorted(paths):
        try:
            verification = verify_package(path)
            with UnrealPackage(path) as package:
                actor_related = [
                    {
                        "export_index": index,
                        "object_path": package.object_path(index),
                        "class": item.class_name,
                    }
                    for index, item in enumerate(package.exports, 1)
                    if item.class_name and (
                        item.class_name.endswith("Actor")
                        or item.class_name.endswith("Actor_C")
                        or item.class_name in ("World", "Level", "WorldSettings", "Landscape")
                    )
                ]
                worlds = [index for index, item in enumerate(package.exports, 1) if item.class_name == "World"]
                levels = [index for index, item in enumerate(package.exports, 1) if item.class_name == "Level"]
                settings = [index for index, item in enumerate(package.exports, 1) if item.class_name == "WorldSettings"]
                record = {
                    "absolute_path": str(path),
                    "relative_paths": [
                        str(path.relative_to(root.resolve())) for root in roots_list
                        if root.is_dir() and path.is_relative_to(root.resolve())
                    ],
                    "size": path.stat().st_size,
                    "sha256": package.sha256,
                    "package_name": package.summary.package_name,
                    "file_version_ue4": package.summary.file_version_ue4,
                    "file_version_ue5": package.summary.file_version_ue5,
                    "saved_engine": (
                        package.summary.saved_by_engine_version.display
                        if package.summary.saved_by_engine_version else None
                    ),
                    "custom_versions": package.summary.custom_versions,
                    "import_count": len(package.imports),
                    "export_count": len(package.exports),
                    "sidecars": verification.get("companions", []),
                    "world_exports": worlds,
                    "level_exports": levels,
                    "world_settings_exports": settings,
                    "actor_related_exports": actor_related,
                    "soft_paths": package.soft_object_paths,
                    "soft_package_references": package.soft_package_references,
                    "missing_dependencies": [],
                    "integrity": verification.get("integrity"),
                    "truncation_status": "COMPLETE" if verification.get("status") == "VERIFIED" else verification.get("status"),
                    "status": "CANDIDATE" if worlds and levels else "NOT_A_WORLD_MAP",
                }
                candidates.append(record)
        except (UEAssetError, OSError, ValueError) as exc:
            candidates.append({
                "absolute_path": str(path), "size": path.stat().st_size,
                "sha256": sha256_file(path), "status": "DECODE_ERROR",
                "reason": f"{type(exc).__name__}: {exc}",
            })
    usable = [item for item in candidates if item.get("status") == "CANDIDATE"]
    selected = usable[0]["absolute_path"] if len(usable) == 1 else None
    result = {
        "schema": "ueassettool.map-candidates/v1",
        "search_roots": [str(path) for path in roots_list],
        "candidate_count": len(candidates),
        "selected_map": selected,
        "selection_basis": (
            "only locally complete UWorld/ULevel candidate; dependency coverage evaluated by closure"
            if selected else "no unique usable map"
        ),
        "candidates": candidates,
    }
    if output:
        write_json(Path(output), result)
    return result


def build_map_gate(contract: dict[str, Any], package_index: dict[str, Any], closure: dict[str, Any]) -> dict[str, Any]:
    validation = contract["validation"]
    local, world = validation["local_transform_counts"], validation["world_transform_counts"]
    blockers: list[str] = []
    if contract["status"] != "VERIFIED": blockers.append(f"map contract is {contract['status']}")
    if validation["actor_component_membership_errors"]: blockers.append("actor component membership is unresolved")
    if world["PARTIAL"] or world["INVALID"] or world["MISSING"]: blockers.append("displayable component world transforms are incomplete")
    if package_index["errors"]: blockers.append("package-index errors are present")
    if closure["unique_missing_package_count"]: blockers.append("local dependency packages remain unresolved")
    return {
        "schema": "ueassettool.map-gate/v1", "selected_map": contract["source"],
        "source_roots": package_index["roots"], "package_index_file_count": package_index["file_count"],
        "indexed_package_count": package_index["package_count"], "package_index_errors": package_index["errors"],
        "world_count": validation["world_count"], "level_count": validation["level_count"],
        "actor_count": validation["actor_count"], "component_count": validation["component_count"],
        "local_transforms": local, "world_transforms": world,
        "actor_component_membership_errors": validation["actor_component_membership_errors"],
        "unresolved_attachment_parents": validation["unresolved_parents"], "attachment_cycles": validation["attachment_cycles"],
        "dependency_edge_occurrences": closure["edge_count"], "dependency_unique_edges": closure["unique_edge_count"],
        "dependency_counts_by_status": closure["counts_by_status"],
        "uds_local_missing_package_count": closure["unique_missing_package_count"],
        "engine_external_missing_count": closure["counts_by_status"].get("EXTERNAL_ENGINE_PACKAGE", 0),
        "landscape_status": "DATA_ONLY" if contract["landscapes"] else "ABSENT",
        "blockers": blockers, "test_results": "external test runner required", "gate_status": "PASS" if not blockers else "FAIL",
    }
