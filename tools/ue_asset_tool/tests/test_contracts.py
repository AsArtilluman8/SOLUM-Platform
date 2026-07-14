import hashlib
import struct
import unittest

from ueassettool.contracts import (
    _is_blueprint_contract_export,
    _niagara_compile_hash, _niagara_parameter_store, _niagara_scalar,
    _niagara_variable_summary,
)


def _property(name: str, value: object) -> dict[str, object]:
    return {"name": name, "value": value, "decode_status": "decoded"}


def _type_definition(object_path: str) -> dict[str, object]:
    return {
        "properties": [
            _property("ClassStructOrEnum", {"package_index": -1, "object": object_path}),
            _property("UnderlyingType", 2),
            _property("Flags", 0),
        ],
        "terminated": True,
    }


def _offset_variable(name: str, object_path: str, offset: int) -> dict[str, object]:
    return {
        "serialization": "type-definition-registry",
        "name": name,
        "type_definition": _type_definition(object_path),
        "offset": offset,
    }


class NiagaraContractTests(unittest.TestCase):
    def test_blueprint_contract_includes_cdo_not_generated_instances(self) -> None:
        self.assertTrue(_is_blueprint_contract_export(
            "Ultra_Dynamic_Sky_C",
            "/Game/UDS/Ultra_Dynamic_Sky.Default__Ultra_Dynamic_Sky_C",
        ))
        self.assertFalse(_is_blueprint_contract_export(
            "Ultra_Dynamic_Sky_C",
            "/Game/UDS/Map.Ultra_Dynamic_Sky_C_0",
        ))
        self.assertTrue(_is_blueprint_contract_export(
            "K2Node_CallFunction", "/Game/UDS/BP.Graph.K2Node_CallFunction_0",
        ))

    def test_bool_and_compile_hash_require_official_value_widths(self) -> None:
        invalid_bool = _niagara_scalar(struct.pack("<i", 2), 0, "/Script/Niagara.NiagaraBool")
        self.assertEqual(invalid_bool["status"], "RAW_VERIFIED")
        short_hash = {
            "properties": [_property("DataHash", {"items": list(range(19))})],
            "terminated": True,
        }
        self.assertEqual(_niagara_compile_hash(short_hash)["status"], "UNSUPPORTED")

    def test_parameter_store_decodes_offsets_and_checks_spans(self) -> None:
        data = struct.pack("<f4f", 0.25, 1.0, 0.5, 0.25, 1.0)
        store = {
            "properties": [
                _property("SortedParameterOffsets", {
                    "items": [
                        _offset_variable("NPC.Test.Amount", "/Script/Niagara.NiagaraFloat", 0),
                        _offset_variable("NPC.Test.Color", "/Script/CoreUObject.LinearColor", 4),
                    ],
                }),
                _property("ParameterData", {"items": list(data)}),
            ],
            "terminated": True,
        }
        decoded = _niagara_parameter_store(store)
        self.assertEqual(decoded["status"], "VERIFIED")
        self.assertEqual(decoded["parameter_count"], 2)
        self.assertEqual(decoded["parameters"][0]["value"]["value"], 0.25)
        self.assertEqual(decoded["parameters"][1]["value"]["value"], [1.0, 0.5, 0.25, 1.0])
        self.assertEqual(decoded["parameter_data"]["sha256"], hashlib.sha256(data).hexdigest())
        self.assertEqual(decoded["integrity"]["overlap_count"], 0)

    def test_parameter_store_rejects_out_of_bounds_value(self) -> None:
        store = {
            "properties": [
                _property("SortedParameterOffsets", {
                    "items": [_offset_variable("NPC.Test.Amount", "/Script/Niagara.NiagaraFloat", 4)],
                }),
                _property("ParameterData", {"items": [0, 0, 0, 0]}),
            ],
            "terminated": True,
        }
        decoded = _niagara_parameter_store(store)
        self.assertEqual(decoded["status"], "UNSUPPORTED")
        self.assertEqual(decoded["parameters"][0]["value"]["status"], "UNSUPPORTED")

    def test_variable_default_requires_matching_size_and_hash(self) -> None:
        data = struct.pack("<f", 2.5)
        variable = {
            "serialization": "type-definition-registry",
            "name": "NPC.Test.Amount",
            "type_definition": _type_definition("/Script/Niagara.NiagaraFloat"),
            "data_size": len(data),
            "data_hex": data.hex(),
            "data_sha256": hashlib.sha256(data).hexdigest(),
        }
        decoded = _niagara_variable_summary(variable)
        self.assertEqual(decoded["status"], "VERIFIED")
        self.assertEqual(decoded["default"]["value"], 2.5)
        variable["data_sha256"] = "0" * 64
        rejected = _niagara_variable_summary(variable)
        self.assertEqual(rejected["status"], "UNSUPPORTED")


if __name__ == "__main__":
    unittest.main()
