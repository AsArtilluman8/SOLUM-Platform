from __future__ import annotations

import hashlib
from pathlib import Path
from typing import Any

from .binary import BinaryReader
from .errors import BoundsError, FormatError, UnsupportedError
from .properties import PropertyParser


GUID_MAIN = "697dd581-e64f41ab-aa4a51ec-beb7b628"
GUID_FRAMEWORK = "cffc743f-43b04480-939114df-171d2073"
GUID_RELEASE = "9c54d522-a8264fbe-94210746-61b482d0"
GUID_UE5_RELEASE = "d89b5e42-24bd4d46-8412aca8-df641779"
GUID_BLUEPRINTS = "b0d832e4-1f894f0d-accf7eb7-36fd4aa2"
GUID_EDITOR = "e4b068ed-f49442e9-a231da0b-2e46bb41"
GUID_FORTNITE_MAIN = "601d1886-ac644f84-aa16d3de-0deac7d6"

MAIN_EDGRAPH_PIN_SOURCE_INDEX = 49
FRAMEWORK_PIN_CONTAINER_TYPE = 25
FRAMEWORK_PINS_STORE_FNAME = 30
RELEASE_PIN_TYPE_UOBJECT_WRAPPER = 31
UE5_RELEASE_REAL_NUMBERS = 28
UE5_RELEASE_SINGLE_PRECISION_DEFAULT = 35
BLUEPRINT_ADVANCED_CONTAINER_SUPPORT = 5
BLUEPRINT_EDGRAPH_PIN_OPTIMIZED = 3
EDITOR_CULTURE_INVARIANT_TEXT_KEY_STABILITY = 31
FORTNITE_TEXT_DEV_NOTES = 259
UE4_MEMBERREFERENCE_IN_PINTYPE = 355

CONTAINER_NAMES = {0: "none", 1: "array", 2: "set", 3: "map"}
DIRECTION_NAMES = {0: "input", 1: "output"}


class BlueprintGraphDecoder:
    """Decode editor graph pins and reconstruct verifiable node connections."""

    def __init__(self, package: Any):
        self.package = package
        self.properties = PropertyParser(package)
        self.main = package.custom_version(GUID_MAIN)
        self.framework = package.custom_version(GUID_FRAMEWORK)
        self.release = package.custom_version(GUID_RELEASE)
        self.ue5_release = package.custom_version(GUID_UE5_RELEASE)
        self.blueprints = package.custom_version(GUID_BLUEPRINTS)
        self.editor = package.custom_version(GUID_EDITOR)
        self.fortnite = package.custom_version(GUID_FORTNITE_MAIN)

    def _fname(self, r: BinaryReader) -> str:
        return self.package.fname(r.fname_raw()).display

    def _reference(self, r: BinaryReader) -> dict[str, Any]:
        owner = r.i32()
        pin_id = r.guid()
        return {"owning_node_index": owner, "owning_node": self.package.object_path(owner), "pin_id": pin_id}

    def _optional_reference(self, r: BinaryReader) -> dict[str, Any] | None:
        is_null = r.boolean32()
        return None if is_null else self._reference(r)

    def _reference_array(self, r: BinaryReader, label: str) -> list[dict[str, Any] | None]:
        count = r.count(label, maximum=1_000_000)
        return [self._optional_reference(r) for _ in range(count)]

    def _ftext(self, r: BinaryReader, *, depth: int = 0) -> dict[str, Any]:
        if depth > 16:
            raise FormatError("FText recursion exceeds 16")
        flags = r.u32()
        history = r.i8()
        result: dict[str, Any] = {"flags": flags, "history_type": history, "text": ""}
        if history == -1:
            if self.editor >= EDITOR_CULTURE_INVARIANT_TEXT_KEY_STABILITY:
                present = r.boolean32()
                result["culture_invariant_present"] = present
                if present:
                    result["text"] = r.fstring()
            return result
        if history == 0:
            namespace = r.fstring()
            key = r.fstring()
            source = r.fstring()
            result.update(namespace=namespace, key=key, source=source, text=source)
            if self.fortnite >= FORTNITE_TEXT_DEV_NOTES:
                result["developer_notes"] = r.fstring()
            return result
        if history in (1, 2):
            source_format = self._ftext(r, depth=depth + 1)
            count = r.count("FText format argument", maximum=1_000_000)
            args: list[Any] = []
            for i in range(count):
                name = r.fstring() if history == 1 else str(i)
                args.append({"name": name, "value": self._format_argument(r, depth + 1)})
            result.update(source_format=source_format, arguments=args, text=source_format.get("text", ""))
            return result
        if history == 10:  # Transform
            source = self._ftext(r, depth=depth + 1)
            transform = r.u8()
            result.update(source=source, transform=transform, text=source.get("text", ""))
            return result
        if history == 11:  # StringTableEntry
            result.update(table_id=self._fname(r), key=r.fstring())
            return result
        raise UnsupportedError(f"FText history type {history} is not implemented")

    def _format_argument(self, r: BinaryReader, depth: int) -> Any:
        kind = r.i8()
        if kind == 0:
            return {"type": "int", "value": r.i64()}
        if kind == 1:
            return {"type": "uint", "value": r.u64()}
        if kind == 2:
            return {"type": "float", "value": r.f32()}
        if kind == 3:
            return {"type": "double", "value": r.f64()}
        if kind == 4:
            return {"type": "text", "value": self._ftext(r, depth=depth)}
        raise UnsupportedError(f"FText argument type {kind}")

    def _simple_member_reference(self, r: BinaryReader) -> dict[str, Any]:
        parent = r.i32()
        return {
            "parent_index": parent,
            "parent": self.package.object_path(parent),
            "name": self._fname(r),
            "guid": r.guid(),
        }

    def _terminal_type(self, r: BinaryReader) -> dict[str, Any]:
        if self.framework < FRAMEWORK_PINS_STORE_FNAME:
            category, subcategory = r.fstring(), r.fstring()
        else:
            category, subcategory = self._fname(r), self._fname(r)
        obj = r.i32()
        result = {
            "category": category,
            "subcategory": subcategory,
            "subcategory_object_index": obj,
            "subcategory_object": self.package.object_path(obj),
            "is_const": r.boolean32(),
            "is_weak_pointer": r.boolean32(),
        }
        if self.release >= RELEASE_PIN_TYPE_UOBJECT_WRAPPER:
            result["is_uobject_wrapper"] = r.boolean32()
        return result

    def _pin_type(self, r: BinaryReader) -> dict[str, Any]:
        if self.framework < FRAMEWORK_PINS_STORE_FNAME:
            category, subcategory = r.fstring(), r.fstring()
        else:
            category, subcategory = self._fname(r), self._fname(r)
        obj = r.i32()
        result: dict[str, Any] = {
            "category": category,
            "subcategory": subcategory,
            "subcategory_object_index": obj,
            "subcategory_object": self.package.object_path(obj),
        }
        if self.framework >= FRAMEWORK_PIN_CONTAINER_TYPE:
            container = r.u8()
            if container not in CONTAINER_NAMES:
                raise FormatError(f"invalid EdGraph pin container {container}")
            result["container"] = CONTAINER_NAMES[container]
            if container == 3:
                result["value_type"] = self._terminal_type(r)
        else:
            is_map = is_set = False
            if self.blueprints >= BLUEPRINT_ADVANCED_CONTAINER_SUPPORT:
                is_map = r.boolean32()
                if is_map:
                    result["value_type"] = self._terminal_type(r)
                is_set = r.boolean32()
            is_array = r.boolean32()
            result["container"] = "array" if is_array else "set" if is_set else "map" if is_map else "none"
        result["is_reference"] = r.boolean32()
        result["is_weak_pointer"] = r.boolean32()
        if self.package.summary.file_version_ue4 >= UE4_MEMBERREFERENCE_IN_PINTYPE:
            result["member_reference"] = self._simple_member_reference(r)
        result["is_const"] = r.boolean32()
        if self.release >= RELEASE_PIN_TYPE_UOBJECT_WRAPPER:
            result["is_uobject_wrapper"] = r.boolean32()
        if self.ue5_release >= UE5_RELEASE_SINGLE_PRECISION_DEFAULT:
            result["serialize_default_as_single_precision"] = r.boolean32()
        elif category == "float":
            result["serialize_default_as_single_precision"] = True
        return result

    def _owning_pin(self, r: BinaryReader) -> dict[str, Any]:
        outer_reference = self._reference(r)
        own_reference = self._reference(r)
        if self.framework >= FRAMEWORK_PINS_STORE_FNAME:
            name = self._fname(r)
        else:
            name = r.fstring()
        friendly = self._ftext(r)
        source_index = r.i32() if self.main >= MAIN_EDGRAPH_PIN_SOURCE_INDEX else -1
        tooltip = r.fstring()
        direction = r.u8()
        if direction not in DIRECTION_NAMES:
            raise FormatError(f"invalid EdGraph pin direction {direction}")
        pin_type = self._pin_type(r)
        default_value = r.fstring()
        autogenerated_default = r.fstring()
        default_object_index = r.i32()
        default_text = self._ftext(r)
        linked_to = self._reference_array(r, "linked pin")
        sub_pins = self._reference_array(r, "sub pin")
        parent_pin = self._optional_reference(r)
        pass_through = self._optional_reference(r)
        persistent_guid = r.guid()
        bitfield = r.u32()
        return {
            "reference": own_reference,
            "outer_reference": outer_reference,
            "name": name,
            "source_index": source_index,
            "direction": DIRECTION_NAMES[direction],
            "friendly_name": friendly,
            "tooltip": tooltip,
            "type": pin_type,
            "default_value": default_value,
            "autogenerated_default_value": autogenerated_default,
            "default_object_index": default_object_index,
            "default_object": self.package.object_path(default_object_index),
            "default_text": default_text,
            "linked_to": linked_to,
            "sub_pins": sub_pins,
            "parent_pin": parent_pin,
            "reference_pass_through": pass_through,
            "persistent_guid": persistent_guid,
            "bitfield": bitfield,
        }

    @staticmethod
    def _properties_by_name(properties: list[dict[str, Any]]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for prop in properties:
            if prop.get("decode_status", "").startswith("decoded"):
                result[prop["name"]] = prop.get("value")
        return result

    def parse_node(self, export_index: int) -> dict[str, Any]:
        export = self.package.exports[export_index - 1]
        base = self.properties.parse_export(export_index)
        result: dict[str, Any] = {
            "export_index": export_index,
            "object": self.package.object_path(export_index),
            "name": export.object_name.display,
            "class": export.class_name,
            "graph": self.package.object_path(export.outer_index),
            "properties": self._properties_by_name(base.get("properties", [])),
            "pins": [],
            "pin_decode_status": "unavailable",
        }
        trailing = base.get("trailing_native")
        if not trailing:
            result["pin_decode_note"] = "no native pin stream"
            return result
        if self.blueprints < BLUEPRINT_EDGRAPH_PIN_OPTIMIZED:
            result["pin_decode_note"] = "package predates optimized EdGraph pin serialization"
            return result
        source = Path(export.payload_source or self.package.path)
        start = int(trailing["physical_offset"])
        end = int(export.payload_physical_offset or 0) + export.serial_size
        try:
            with BinaryReader(source) as r:
                r.seek(start)
                with r.bounded(end):
                    count = r.count("owning EdGraph pin", maximum=1_000_000)
                    pins: list[dict[str, Any] | None] = []
                    for _ in range(count):
                        is_null = r.boolean32()
                        pins.append(None if is_null else self._owning_pin(r))
                    tail_start = r.position
                    tail = r.read(end - tail_start)
            result["pins"] = pins
            result["pin_decode_status"] = "decoded"
            result["derived_native_tail"] = {
                "physical_offset": tail_start,
                "size": len(tail),
                "sha256": hashlib.sha256(tail).hexdigest(),
                "preview_hex": tail[:48].hex(" "),
            } if tail else None
        except (BoundsError, FormatError, UnsupportedError, UnicodeError) as exc:
            result["pin_decode_status"] = "raw"
            result["pin_decode_note"] = str(exc)
            result["pin_stream"] = trailing
        return result

    def decode(self, *, include_niagara: bool = False) -> dict[str, Any]:
        def is_node(name: str | None) -> bool:
            if not name:
                return False
            if name.startswith("K2Node_") or name in ("EdGraphNode", "EdGraphNode_Comment"):
                return True
            return include_niagara and name.startswith("NiagaraNode")

        indices = [i for i, item in enumerate(self.package.exports, 1) if is_node(item.class_name)]
        nodes = [self.parse_node(i) for i in indices if self.package.exports[i - 1].payload_availability == "available"]
        pin_lookup: dict[tuple[int, str], dict[str, Any]] = {}
        duplicate_pin_ids = 0
        owner_mismatches = 0
        for node in nodes:
            for pin in node["pins"]:
                if pin:
                    key = (node["export_index"], pin["reference"]["pin_id"])
                    if key in pin_lookup:
                        duplicate_pin_ids += 1
                    else:
                        pin_lookup[key] = pin
                    if pin["reference"]["owning_node_index"] != node["export_index"]:
                        owner_mismatches += 1
        edges: list[dict[str, Any]] = []
        seen: set[tuple[tuple[int, str], tuple[int, str]]] = set()
        directed_links: set[tuple[tuple[int, str], tuple[int, str]]] = set()
        for node in nodes:
            for pin in node["pins"]:
                if not pin:
                    continue
                source_key = (node["export_index"], pin["reference"]["pin_id"])
                for link in pin["linked_to"]:
                    if not link:
                        continue
                    target_key = (link["owning_node_index"], link["pin_id"])
                    directed_links.add((source_key, target_key))
                    canonical = tuple(sorted((source_key, target_key)))
                    if canonical in seen:
                        continue
                    seen.add(canonical)
                    target = pin_lookup.get(target_key)
                    edges.append({
                        "from_node": source_key[0], "from_pin_id": source_key[1], "from_pin": pin["name"],
                        "to_node": target_key[0], "to_pin_id": target_key[1],
                        "to_pin": target.get("name") if target else None,
                        "target_resolved": target is not None,
                    })
        graphs: dict[str, dict[str, Any]] = {}
        node_lookup = {node["export_index"]: node for node in nodes}
        for node in nodes:
            graph = graphs.setdefault(node["graph"], {"graph": node["graph"], "nodes": [], "edges": []})
            graph["nodes"].append(node)
        for edge in edges:
            source_node = node_lookup.get(edge["from_node"])
            if source_node:
                graphs[source_node["graph"]]["edges"].append(edge)
        decoded_nodes = sum(x["pin_decode_status"] == "decoded" for x in nodes)
        dangling_links = sum(target not in pin_lookup for _source, target in directed_links)
        asymmetric_links = sum((target, source) not in directed_links for source, target in directed_links)
        self_links = sum(source == target for source, target in directed_links)
        verified = bool(nodes) and decoded_nodes == len(nodes) and not any((
            duplicate_pin_ids, owner_mismatches, dangling_links, asymmetric_links, self_links,
        ))
        return {
            "schema": "ueassettool.graph-contract/v1",
            "status": "VERIFIED" if verified else "RAW_VERIFIED" if nodes else "UNSUPPORTED",
            "package": self.package.summary.package_name,
            "node_count": len(nodes),
            "decoded_pin_node_count": decoded_nodes,
            "pin_count": len(pin_lookup),
            "edge_count": len(edges),
            "integrity": {
                "duplicate_pin_id_count": duplicate_pin_ids,
                "pin_owner_mismatch_count": owner_mismatches,
                "directed_link_count": len(directed_links),
                "dangling_link_count": dangling_links,
                "asymmetric_link_count": asymmetric_links,
                "self_link_count": self_links,
            },
            "graphs": list(graphs.values()),
        }
