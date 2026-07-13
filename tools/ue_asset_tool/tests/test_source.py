import unittest

from ueassettool.source import classify_dependency, normalize_object_reference


class SourceResolverTests(unittest.TestCase):
    def test_generated_class_cdo_and_subobject_keep_owning_package(self) -> None:
        generated = normalize_object_reference("/Game/UDS/BP_Sky.BP_Sky_C")
        cdo = normalize_object_reference("/Game/UDS/BP_Sky.Default__BP_Sky_C")
        subobject = normalize_object_reference("/Game/UDS/BP_Sky.BP_Sky_C:Root")
        self.assertEqual(generated["package"], "/Game/UDS/BP_Sky")
        self.assertEqual(cdo["package"], "/Game/UDS/BP_Sky")
        self.assertEqual(generated["kind"], "GENERATED_CLASS_OR_CDO")
        self.assertEqual(cdo["kind"], "GENERATED_CLASS_OR_CDO")
        self.assertEqual(subobject["kind"], "SUBOBJECT")
        self.assertEqual(
            normalize_object_reference("/Game/UDS/Materials/UDS_C.UDS_C")["kind"],
            "PACKAGE_OR_OBJECT",
        )

    def test_dependency_classification_is_conservative_and_explicit(self) -> None:
        required = classify_dependency({"target_object_path": "/Game/UDS/Meshes/S.S",
            "target_package": "/Game/UDS/Meshes/S", "source_object": "/Game/Map.C",
            "source_package": "/Game/Map", "source_property": "StaticMesh", "reference_type": "DECODED_PROPERTY"})
        optional = classify_dependency({"target_object_path": "/Game/UDS/Sound/Rain.Rain",
            "target_package": "/Game/UDS/Sound/Rain", "source_object": "/Game/BP",
            "source_package": "/Game/BP", "source_property": None, "reference_type": "IMPORT_TABLE"})
        self.assertEqual(required["classification"], "REQUIRED_FOR_RENDERED_SCENE")
        self.assertEqual(optional["classification"], "OPTIONAL_RUNTIME")


if __name__ == "__main__":
    unittest.main()
