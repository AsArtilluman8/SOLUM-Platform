import math
import unittest

from ueassettool.errors import FormatError
from ueassettool.map import (
    MAP_SCHEMA, _relative_transform, build_map_gate, build_renderable_candidates,
    compose_transforms,
    detect_attachment_cycles,
    quaternion_norm,
    rotator_to_quaternion,
    ue_to_renderer_position,
)


def _transform(translation, quaternion=(0.0, 0.0, 0.0, 1.0), scale=(1.0, 1.0, 1.0)):
    return {
        "status": "VERIFIED",
        "owner_object": "test",
        "translation": list(translation),
        "quaternion": list(quaternion),
        "scale": list(scale),
    }


class MapContractTests(unittest.TestCase):
    def test_schema_and_rotator_quaternion_are_finite_and_normalized(self) -> None:
        self.assertEqual(MAP_SCHEMA, "ueassettool.map-contract/v1")
        quaternion = rotator_to_quaternion((90.0, 0.0, 0.0))
        self.assertTrue(all(math.isfinite(value) for value in quaternion))
        self.assertAlmostEqual(quaternion_norm(quaternion), 1.0, places=12)

    def test_parent_world_times_child_relative_composes_translation_and_scale(self) -> None:
        world = compose_transforms(
            _transform((100.0, 0.0, 0.0), scale=(2.0, 2.0, 2.0)),
            _transform((5.0, 0.0, 0.0), scale=(3.0, 4.0, 5.0)),
        )
        self.assertEqual(world["translation"], [110.0, 0.0, 0.0])
        self.assertEqual(world["scale"], [6.0, 8.0, 10.0])
        self.assertEqual(world["composition"], "parent world × child relative")

    def test_non_verified_transform_never_generates_a_world_transform(self) -> None:
        child = _transform((0.0, 0.0, 0.0))
        child["status"] = "PARTIAL"
        with self.assertRaisesRegex(FormatError, "VERIFIED"):
            compose_transforms(_transform((0.0, 0.0, 0.0)), child)

    def test_coordinate_conversion_is_documented_left_to_right_handed_cm_to_meters(self) -> None:
        self.assertEqual(ue_to_renderer_position((100.0, 200.0, 300.0)), [2.0, 3.0, -1.0])

    def test_attachment_cycle_detection(self) -> None:
        cycles = detect_attachment_cycles([
            {"export_index": 1, "attachment_parent_index": 2},
            {"export_index": 2, "attachment_parent_index": 3},
            {"export_index": 3, "attachment_parent_index": 1},
            {"export_index": 4, "attachment_parent_index": None},
        ])
        self.assertEqual(len(cycles), 1)
        self.assertEqual(set(cycles[0]), {1, 2, 3})

    def test_map_gate_fails_for_partial_transform_and_missing_dependency(self) -> None:
        gate = build_map_gate(
            {"source": {}, "status": "PARTIAL_VERIFIED", "landscapes": [], "validation": {
                "world_count": 1, "level_count": 1, "actor_count": 1, "component_count": 1,
                "local_transform_counts": {"VERIFIED": 1, "PARTIAL": 0, "INVALID": 0, "MISSING": 0},
                "world_transform_counts": {"VERIFIED": 0, "PARTIAL": 1, "INVALID": 0, "MISSING": 0},
                "actor_component_membership_errors": [], "unresolved_parents": [], "attachment_cycles": [],
            }},
            {"roots": ["/exact"], "file_count": 2, "package_count": 2, "errors": []},
            {"edge_count": 1, "unique_edge_count": 1, "unique_missing_package_count": 1,
             "counts_by_status": {"MISSING_PACKAGE": 1}, "unique_edges": [{
                 "terminal_status": "MISSING_PACKAGE",
                 "dependency_classification": {"classification": "REQUIRED_FOR_TRANSFORM"}}]},
        )
        self.assertEqual(gate["gate_status"], "FAIL")
        self.assertIn("required rendered-scene/transform dependencies remain unresolved", gate["blockers"])

    def test_serialized_archetype_transform_fields_are_inherited(self) -> None:
        prop = lambda value: {"value": value, "type": {"display": "Vector3d"},
                              "raw": {"physical_offset": 1, "size": 24, "sha256": "a" * 64}}
        result = _relative_transform(owner_object="Child", owner_class="SceneComponent",
            properties={"RelativeLocation": prop({"x": 1.0, "y": 2.0, "z": 3.0})},
            inherited_properties={
                "RelativeRotation": prop({"pitch": 0.0, "yaw": 0.0, "roll": 0.0}),
                "RelativeScale3D": prop({"x": 1.0, "y": 1.0, "z": 1.0})},
            inheritance_chain=[{"object_path": "Archetype"}], exact_default_profile=None)
        self.assertEqual(result["status"], "VERIFIED")
        self.assertEqual(result["provenance"]["serialized_inheritance_chain"][0]["object_path"], "Archetype")

    def test_optional_missing_does_not_block_verified_subset_gate(self) -> None:
        validation = {"world_count": 1, "level_count": 1, "actor_count": 1, "component_count": 1,
            "local_transform_counts": {"VERIFIED": 1, "PARTIAL": 0, "INVALID": 0, "MISSING": 0},
            "world_transform_counts": {"VERIFIED": 1, "PARTIAL": 0, "INVALID": 0, "MISSING": 0},
            "actor_component_membership_errors": [], "unresolved_parents": [], "attachment_cycles": []}
        gate = build_map_gate({"source": {}, "status": "PARTIAL_VERIFIED", "landscapes": [], "validation": validation},
            {"roots": [], "file_count": 1, "package_count": 1, "errors": []},
            {"edge_count": 1, "unique_edge_count": 1, "unique_missing_package_count": 1,
             "counts_by_status": {"MISSING_PACKAGE": 1}, "unique_edges": [{"terminal_status": "MISSING_PACKAGE",
             "dependency_classification": {"classification": "OPTIONAL_RUNTIME"}}]},
            {"candidate_count": 1, "verified_renderable_count": 1})
        self.assertEqual(gate["gate_status"], "PASS")

    def test_renderable_subset_requires_verified_world_transform(self) -> None:
        component = {"object_path": "C", "export_index": 1, "owner_actor": "A", "provenance": {},
                     "world_transform": {"status": "VERIFIED", "translation": [0, 0, 0],
                     "quaternion": [0, 0, 0, 1], "scale": [1, 1, 1]}}
        result = build_renderable_candidates({"components": [component]}, {"unique_edges": []})
        self.assertEqual(result["candidate_count"], 1)
        self.assertEqual(result["candidates"][0]["status"], "NON_RENDERABLE_NO_GEOMETRY_REFERENCE")


if __name__ == "__main__":
    unittest.main()
