import math
import unittest

from ueassettool.errors import FormatError
from ueassettool.map import (
    MAP_SCHEMA, build_map_gate,
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
             "counts_by_status": {"MISSING_PACKAGE": 1}},
        )
        self.assertEqual(gate["gate_status"], "FAIL")
        self.assertIn("local dependency packages remain unresolved", gate["blockers"])


if __name__ == "__main__":
    unittest.main()
