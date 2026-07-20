#!/usr/bin/env python3
"""Build the source-backed P63.10 Sun-values checkpoint contract.

This tool intentionally stops before runtime implementation. It publishes the exact decoded
Blueprint topology, referenced parameters/curves/material functions and an explicit formula ledger
so an unresolved expression can never be replaced by a visually similar constant.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import deque
from pathlib import Path
from typing import Any

from p63_10_blueprint_schedule_ir import (
    build_operation_order_contract,
    build_writer_schedule_contract,
)
from p63_10_material_expression_ir import (
    build_expression_program,
    resolve_inter_function_defaults,
)


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DATASET = ROOT / "_work/private_uds_p63_10"
DEFAULT_SYSTEM_CONTRACT = (
    ROOT / "_work/agent_reports/p63_10_contract/P63_10_UDS_SYSTEM_CONTRACT.json"
)
DEFAULT_OUTPUT = ROOT / "_work/agent_reports/p63_10_sun_values"
BLUEPRINT_ASSET = "/Game/UltraDynamicSky/Blueprints/Ultra_Dynamic_Sky"
BLUEPRINT_CONTRACT = "contracts/Blueprints__Ultra_Dynamic_Sky.blueprint.json"
PLAYER_OCCLUSION_ASSET = "/Game/UltraDynamicSky/Blueprints/System/UDS_PlayerOcclusion"
PLAYER_OCCLUSION_CONTRACT = (
    "contracts/Blueprints__System__UDS_PlayerOcclusion.blueprint.json"
)
EQUATION_OF_TIME_ASSET = "/Game/UltraDynamicSky/Materials/Float_Curves/Equation_of_Time"
EQUATION_OF_TIME_CONTRACT = (
    "contracts/Materials__Float_Curves__Equation_of_Time.auto.json"
)
GREGORIAN_CALENDAR_ASSET = (
    "/Game/UltraDynamicSky/Blueprints/System/Calendars/Gregorian_Calendar"
)
GREGORIAN_CALENDAR_CONTRACT = (
    "contracts/Blueprints__System__Calendars__Gregorian_Calendar.auto.json"
)
UDS_CALENDAR_ASSET = "/Game/UltraDynamicSky/Blueprints/System/UDS_Calendar"
UDS_CALENDAR_CONTRACT = "contracts/Blueprints__System__UDS_Calendar.blueprint.json"

UE_COORDINATE_SOURCE = (
    "https://dev.epicgames.com/documentation/unreal-engine/"
    "coordinate-system-and-spaces-in-unreal-engine?lang=en-US"
)
FILAMENT_COORDINATE_SOURCE = "https://google.github.io/filament/main/filament.html"
FILAMENT_LIGHT_SOURCE = FILAMENT_COORDINATE_SOURCE
UE_ROTATOR_SOURCE = (
    "https://dev.epicgames.com/documentation/unreal-engine/API/Runtime/Core/Math/"
    "TRotator?application_version=5.5"
)
UE_COMPOSE_ROTATORS_SOURCE = (
    "https://dev.epicgames.com/documentation/unreal-engine/API/Runtime/Engine/"
    "UKismetMathLibrary"
)
UE_ROTATOR_TUPLE_SOURCE = (
    "https://dev.epicgames.com/documentation/en-us/unreal-engine/"
    "vector-/-rotator-controls?application_version=4.27"
)
UE_SEQUENCE_SOURCE = (
    "https://dev.epicgames.com/documentation/en-us/unreal-engine/"
    "flow-control-in-unreal-engine"
)
UE_OBJECT_ZERO_INIT_SOURCE = (
    "https://dev.epicgames.com/documentation/en-us/unreal-engine/"
    "unreal-object-handling-in-unreal-engine"
)
SOLUM_COORDINATE_OWNER = (
    "apps/engine/src/main/java/com/solum/engine/environment/p63/"
    "SolumCelestialCoordinateSystem.java"
)

ROOT_FUNCTIONS = (
    "Current Sun Radius",
    "Current Sun Light Intensity",
    "Current Sun Light Color",
    "Current Sun Disk Intensity",
    "Current Sun Disk Color",
    "Sun Height",
)

EXPECTED_FUNCTION_SLICE = (
    "Adjust Base Sun Light Intensity",
    "Current Sun Disk Color",
    "Current Sun Disk Intensity",
    "Current Sun Light Color",
    "Current Sun Light Intensity",
    "Current Sun Radius",
    "Scaled Directional Balance",
    "Sun Height",
)

EXPECTED_LOCAL_GRAPH_SYMBOLS = (
    "Out Color",
    "Sun Vector",
    "Unscaled Color",
    "Unscaled Intensity",
)


def coordinate_mapping_contract() -> dict[str, Any]:
    """Publish engine semantics separately from UDS-authored behavior.

    UDS provides the world-light meaning and the Sun-height sign. UE and Filament provide the
    coordinate/API semantics needed to move that value without inventing a visual convention.
    """
    matrix = [
        [0.0, 1.0, 0.0],
        [0.0, 0.0, 1.0],
        [-1.0, 0.0, 0.0],
    ]
    payload = {
        "status": "VERIFIED_ENGINE_COORDINATE_CONVERSION",
        "implementationStatus": "NOT_IMPLEMENTED",
        "authoritySeparation": {
            "uds": "world-space light behavior, writer topology and Sun-height sign",
            "unrealEngine": "source coordinate basis, FRotator/GetForwardVector semantics",
            "filament": "target coordinate basis and directional-light ray-direction semantics",
            "solum": "owned target world-axis convention",
        },
        "sourceSpace": {
            "name": "UNREAL_ENGINE_WORLD",
            "handedness": "LEFT_HANDED",
            "axes": {
                "+X": "forward",
                "+Y": "right",
                "+Z": "up",
            },
            "engineSource": UE_COORDINATE_SOURCE,
        },
        "targetSpace": {
            "name": "SOLUM_FILAMENT_WORLD",
            "handedness": "RIGHT_HANDED",
            "axes": {
                "+X": "right/east",
                "+Y": "up",
                "-Z": "forward/north",
            },
            "engineSource": FILAMENT_COORDINATE_SOURCE,
            "solumOwner": SOLUM_COORDINATE_OWNER,
        },
        "vectorTransform": {
            "equation": "filament = (ue.y, ue.z, -ue.x)",
            "rowMajorMatrix": matrix,
            "determinant": -1.0,
            "axisFixtures": {
                "ueForward(+X)": [0.0, 0.0, -1.0],
                "ueRight(+Y)": [1.0, 0.0, 0.0],
                "ueUp(+Z)": [0.0, 1.0, 0.0],
            },
            "properties": [
                "orthonormal",
                "length preserving",
                "changes handedness exactly once",
            ],
        },
        "directionSemantics": {
            "udsCachedSunVector": {
                "meaning": "world-space direction travelled by sunlight rays",
                "evidence": [
                    (
                        "UDS Sun Height: CachedSunVector.z * -0.5 + 0.5; an overhead "
                        "downward ray (+height) therefore has UE z=-1"
                    ),
                    (
                        "UDS Sun World Rotation tooltip identifies the world rotation of the "
                        "sunlight; Sun Height can use GetForwardVector(Sun World Rotation)"
                    ),
                ],
                "status": "VERIFIED_UDS_GRAPH_PLUS_UE_SEMANTICS",
            },
            "filamentDirectionalLight": {
                "meaning": "world-space direction travelled by light rays",
                "shaderRelation": "surfaceToLight = normalize(-lightDirection)",
                "engineSource": FILAMENT_LIGHT_SOURCE,
                "status": "VERIFIED_FILAMENT_API_SEMANTICS",
            },
            "visualSunBodyDirection": {
                "equation": "observerToSun = -filamentDirectionalLightDirection",
                "status": "VERIFIED_SIGN_CONVERSION",
            },
        },
        "rotationRule": {
            "source": "UE FRotator in degrees",
            "method": (
                "evaluate the required UE basis/direction first, transform each vector with the "
                "matrix, then reconstruct a target-space basis/rotation only if needed"
            ),
            "engineSemantics": {
                "rotator": {
                    "angles": "degrees",
                    "intrinsicOrder": ["Yaw", "Pitch", "Roll"],
                    "axes": {"Yaw": "+Z/up", "Pitch": "+Y/right", "Roll": "+X/forward"},
                    "source": UE_ROTATOR_SOURCE,
                },
                "serializedTuple": {
                    "mapping": {"X": "Roll", "Y": "Pitch", "Z": "Yaw"},
                    "decodedLiteral": {
                        "serialized": "0.000000,0.000000,90.000000",
                        "roll": 0.0,
                        "pitch": 0.0,
                        "yaw": 90.0,
                    },
                    "source": UE_ROTATOR_TUPLE_SOURCE,
                },
                "ComposeRotators(A,B)": {
                    "order": "apply A first, then B",
                    "compositionEquation": "R_compose = R_B * R_A",
                    "source": UE_COMPOSE_ROTATORS_SOURCE,
                },
                "GreaterGreater_VectorRotator": {
                    "meaning": "Rotator.RotateVector(Vector)",
                    "targetEquation": "M * (R_ue * v_ue)",
                    "source": UE_ROTATOR_SOURCE,
                },
                "LessLess_VectorRotator": {
                    "meaning": "Rotator.UnrotateVector(Vector)",
                    "targetEquation": "M * (transpose(R_ue) * v_ue)",
                    "source": UE_ROTATOR_SOURCE,
                },
                "coordinateConjugation": {
                    "equation": "R_filament = M * R_ue * transpose(M)",
                    "determinant": 1.0,
                    "reason": "the handedness-changing basis map is applied on both sides",
                },
            },
            "status": "VERIFIED_ENGINE_MAPPING_IMPLEMENTATION_NOT_DONE",
        },
        "remainingTrajectoryEvidence": [
            "runtime mutable astronomical inputs and system-time values",
            "external engine tick/delegate lifecycle and unresolved timer initial/reset values",
        ],
        "prohibitedSimplifications": [
            "permute or negate FRotator pitch/yaw/roll components directly",
            "pass the visual observer-to-Sun vector to Filament LightManager without inversion",
            "derive Sun position from camera rotation",
            "claim the coordinate conversion proves the still-partial UDS trajectory",
        ],
    }
    payload["contractSha256"] = sha256_bytes(
        json.dumps(payload, ensure_ascii=False, sort_keys=True).encode("utf-8")
    )
    return payload

DERIVED_SUN_PARAMETER_WRITERS = {
    "Apply Flat Cloudiness": {
        "graph": "Static Properties - Mode Derivatives",
        "equation": (
            "contains([SkyMode::NewEnumerator4, SkyMode::NewEnumerator6, "
            "SkyMode::NewEnumerator8], SkyMode) || "
            "(SkyMode == SkyMode::NewEnumerator0 && ColorMode == ColorMode::NewEnumerator1)"
        ),
        "ast": {
            "or": [
                {
                    "contains": [
                        [
                            "SkyMode::NewEnumerator4",
                            "SkyMode::NewEnumerator6",
                            "SkyMode::NewEnumerator8",
                        ],
                        "Sky Mode",
                    ]
                },
                {
                    "and": [
                        {"equal": ["Sky Mode", "SkyMode::NewEnumerator0"]},
                        {"equal": ["Color Mode", "ColorMode::NewEnumerator1"]},
                    ]
                },
            ]
        },
        "sourceNodes": ["Array_Contains", "BooleanOR", "BooleanAND", "K2Node_EnumEquality"],
    },
    "Using Sky Atmosphere": {
        "graph": "Static Properties - Mode Derivatives",
        "equation": "ColorMode == ColorMode::NewEnumerator0",
        "ast": {"equal": ["Color Mode", "ColorMode::NewEnumerator0"]},
        "sourceNodes": ["K2Node_EnumEquality"],
    },
    "Using Space Mode": {
        "graph": "Static Properties - Mode Derivatives",
        "equation": "SkyMode == SkyMode::NewEnumerator10",
        "ast": {"equal": ["Sky Mode", "SkyMode::NewEnumerator10"]},
        "sourceNodes": ["K2Node_EnumEquality"],
    },
    "Cached Inverted Global Occlusion": {
        "graph": "Monitor for Changes",
        "equation": "GetInvertedGlobalOcclusion()",
        "ast": {"call": "Get Inverted Global Occlusion"},
        "sourceNodes": ["Get Inverted Global Occlusion"],
    },
    "Cached Directional Inscattering Multiplier": {
        "graph": "Cache Properties",
        "equation": "DirectionalInscatteringMultiplier()",
        "ast": {"call": "Directional Inscattering Multiplier"},
        "sourceNodes": ["Directional Inscattering Multiplier"],
    },
    "Cached Directional Light Dimming": {
        "graph": "Cache Properties",
        "equation": "DirectionalLightDimming()",
        "ast": {"call": "Directional Light Dimming"},
        "sourceNodes": ["Directional Light Dimming"],
    },
    "Cloud Paint Can Subtract Coverage": {
        "graph": "Update Painted Cloud Coverage Target",
        "equation": "false on both decoded K2 writer execution paths",
        "ast": {"constantOnDecodedWriters": False},
        "sourceNodes": ["Cloud Paint Can Subtract Coverage"],
    },
    "Current Scene Lighting Brightness Scale": {
        "graph": "Cache Properties",
        "equation": (
            "ThreeTimeFloats(LightingBrightnessDay, LightingBrightnessDawnDusk, "
            "LightingBrightnessNight, Cached=true)"
        ),
        "ast": {
            "Three Time Floats": {
                "Day": "Lighting Brightness (Day)",
                "Dawn/Dusk": "Lighting Brightness (Dawn/Dusk)",
                "Night": "Lighting Brightness (Night)",
                "Cached": True,
            }
        },
        "sourceNodes": ["Three Time Floats"],
    },
    "Local Cloud Coverage": {
        "graph": "Update Common Derivatives",
        "equation": "GetCloudCoverageLocal()",
        "ast": {"call": "Get Cloud Coverage Local"},
        "sourceNodes": ["Get Cloud Coverage Local"],
    },
}

ORIENTATION_ROOT_FUNCTIONS = (
    "Cache Sun and Moon Orientation",
    "AP - Sun Root Vector",
    "Current Solar Eclipse Values",
    "Static Properties - Calendar",
)

EXPECTED_ORIENTATION_FUNCTION_SLICE = (
    "AP - Sun Root Vector",
    "Approximate Real Sun Moon and Stars",
    "Cache Color",
    "Cache Float",
    "Cache Sun and Moon Orientation",
    "Check If Year is Leap Year",
    "Current Month Lengths",
    "Current Solar Eclipse Values",
    "Day Count at the Start of a Month",
    "Days Since J2000",
    "Days Since Y1D1M1",
    "Force Valid Day",
    "Get Cached Color",
    "H/M/S/MS to Time of Day",
    "Lights Update Degree Threshold Test",
    "Moon Z Vector",
    "Night Filter",
    "Non Sim Moon Alignment",
    "Number of Days in a Year",
    "Offset Date by a Number of Days",
    "Set Apply Property Event Binding",
    "Set Time Cycle Degrees",
    "Simulation Horizon Compensation",
    "Solar Eclipse Circle Mask",
    "Static Properties - Calendar",
    "Sun Z Vector",
    "Update Atlas Light Vectors",
)

SUN_SCHEDULING_FUNCTION_SLICE = (
    "Bind Events to Tick",
    "Cache Properties",
    "Editor Tick",
    "EventGraph",
    "Hard Reset Cache",
    "Increment Cache Timer",
    "Runtime Tick",
    "Set Apply Property Event Binding",
    "Size Cache Arrays",
    "Startup Sky",
    "Update Active Variables",
)

CELESTIAL_SUN_PARAMETER_WRITERS = {
    "Cached Sun Vector": {
        "graph": "Cache Sun and Moon Orientation",
        "equation": (
            "candidate = ManuallyPositionSunTarget ? "
            "GreaterGreater_VectorRotator(SunTarget * -1, ActorRotation) : "
            "(SimulateRealSun ? RealSunPosition : ManualSunOrbit); "
            "CachedSunVector = Normal(candidate, 0.0001); ManualSunOrbit = "
            "MakeVector(pitchedOrbit.x, pitchedOrbit.y, pitchedOrbit.z - SunVerticalOffset) "
            "* ExtendDawnDuskMultiplier after SunPitch/TimeCycleDegrees/world-yaw rotations"
        ),
        "ast": {
            "candidate": {
                "if": "Manually Position Sun Target",
                "then": {
                    "GreaterGreater_VectorRotator": [
                        {"multiply": ["Sun Target", -1.0]},
                        "Actor Rotation",
                    ]
                },
                "else": {
                    "if": "Simulate Real Sun",
                    "then": "Real Sun Position",
                    "else": {
                        "componentMultiply": [
                            {
                                "MakeVector": [
                                    "yawRotated.x",
                                    "yawRotated.y",
                                    {"subtract": ["yawRotated.z", "Sun Vertical Offset"]},
                                ]
                            },
                            "Extend Dawn Dusk Multiplier",
                        ]
                    },
                },
            },
            "manualOrbitIntermediates": {
                "pitchRotation": {"MakeRotator": [0.0, "Sun Pitch", 0.0]},
                "orbitAxis": {
                    "GreaterGreater_VectorRotator": [[1.0, 0.0, 0.0], "pitchRotation"]
                },
                "pitchedBase": {
                    "GreaterGreater_VectorRotator": [[0.0, 0.0, 1.0], "pitchRotation"]
                },
                "timeRotated": {
                    "RotateAngleAxis": [
                        "pitchedBase",
                        "Time Cycle Degrees",
                        "orbitAxis",
                    ]
                },
                "yawRotated": {
                    "RotateAngleAxis": [
                        "timeRotated",
                        {"add": ["Sun Yaw", "Actor Rotation.yaw"]},
                        [0.0, 0.0, 1.0],
                    ]
                },
            },
            "finalWrite": {"Normal": ["candidate", 0.0001]},
        },
        "sourceNodes": [
            "Normal",
            "Multiply_VectorFloat",
            "GreaterGreater_VectorRotator",
            "K2_GetActorRotation",
            "Real Sun Position",
            "MakeRotator",
            "RotateAngleAxis",
            "MakeVector",
            "Multiply_VectorVector",
        ],
        "status": "VERIFIED",
        "statusReason": (
            "writer selection, manual branch and real-Sun output path are exact; runtime "
            "activation/cadence and mutable instance values remain separate schedule evidence"
        ),
    },
    "Sun World Rotation": {
        "graph": "AP - Sun Root Vector",
        "equation": (
            "ForwardVector = Normal(Vector(GetCachedColor(NewEnumerator13)), 0.0001); "
            "SunWorldRotation = MakeRotFromXZ(ForwardVector, CachedSunZVector)"
        ),
        "ast": {
            "Forward Vector": {
                "Normal": [
                    {"Conv_LinearColorToVector": {"Get Cached Color": "NewEnumerator13"}},
                    0.0001,
                ]
            },
            "Sun World Rotation": {
                "MakeRotFromXZ": ["Forward Vector", "Cached Sun Z Vector"]
            },
        },
        "sourceNodes": [
            "Get Cached Color",
            "Conv_LinearColorToVector",
            "Normal",
            "MakeRotFromXZ",
            "Cached Sun Z Vector",
        ],
    },
    "Eclipse Percent": {
        "graph": "Cache Properties",
        "equation": "EclipsePercent = CurrentSolarEclipseValues().EclipsePercent",
        "ast": {"output": ["Current Solar Eclipse Values", "Eclipse Percent"]},
        "sourceNodes": ["Current Solar Eclipse Values"],
    },
    "Cached Solar Eclipse Tint": {
        "graph": "Cache Properties",
        "equation": "CachedSolarEclipseTint = CurrentSolarEclipseValues().TintColor",
        "ast": {"output": ["Current Solar Eclipse Values", "Tint Color"]},
        "sourceNodes": ["Current Solar Eclipse Values"],
    },
}

CALENDAR_INITIALIZATION_WRITER = {
    "parameter": "Calendar runtime-derived values",
    "sourceGraph": f"{BLUEPRINT_ASSET}.Static Properties - Calendar",
    "setNodes": [
        1574,
        1575,
        1576,
        1577,
        1578,
        1579,
        1580,
        1581,
        13038,
        13039,
        13040,
        13041,
        13042,
        13043,
    ],
}

PARAMETER_WRITER_SUPPORT_ROOTS = (
    "Directional Inscattering Multiplier",
    "Directional Light Dimming",
    "Get Cloud Coverage Local",
    "Get Inverted Global Occlusion",
    "Three Time Floats",
)

SUN_MATERIAL_ROOTS = (
    "/Game/UltraDynamicSky/Materials/Material_Functions/Sun_Disk",
    "/Game/UltraDynamicSky/Materials/Material_Functions/Sun_Centered_Gradient",
    "/Game/UltraDynamicSky/Materials/Material_Functions/Sun_Shine_Edges",
    "/Game/UltraDynamicSky/Materials/Material_Functions/Scale_Intensity_Around_Sun",
    "/Game/UltraDynamicSky/Materials/Material_Functions/Sky_Utilities/Active_Sun_or_Moon_Vector",
)

SUN_CURVES = (
    "/Game/UltraDynamicSky/Materials/Color_Curves/Sun_Disk_Color",
    "/Game/UltraDynamicSky/Materials/Color_Curves/Sun_Light_Color",
    "/Game/UltraDynamicSky/Materials/Float_Curves/Directional_Light_Intensity",
    "/Game/UltraDynamicSky/Materials/Float_Curves/Shine_Intensity",
    "/Game/UltraDynamicSky/Materials/Float_Curves/Sun_Highlight_Intensity",
    "/Game/UltraDynamicSky/Materials/Float_Curves/Sun_Highlight_Radius",
)

MATERIAL_SEMANTICS = {
    "/Game/UltraDynamicSky/Materials/Material_Functions/Base_Sky_Color": (
        "select and combine SkyAtmosphere view luminance or authored directional-scattering sky color",
        "base sky and cloud ambient color remain coupled to the active atmosphere path",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Cloud_Distribution": (
        "shape cloud distribution with coverage controls and Sun/Moon-centered gradients",
        "cloud mass and celestial-facing distribution change without alpha-only density",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Cloud_Layer": (
        "evaluate one cloud layer from mapped textures, filtering, shading gradients, colors and Sun edge shine",
        "one independent layer produces hard mask, blend alpha and directionally lit color",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Composite_Cloud_Layers": (
        "evaluate and combine the two authored dynamic cloud-layer branches",
        "two layers retain separate masks, colors and blend alphas before composition",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Composite_Static_Clouds": (
        "composite static cloud texture color/alpha against base sky and celestial gradients",
        "static clouds tint with sky, Sun and Moon rather than acting as a flat monochrome mask",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Filter_Clouds": (
        "remap a cloud signal around middle threshold and gradient width, then soften and saturate",
        "coverage changes cloud structure through thresholding instead of alpha alone",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Light_and_Dark_Cloud_Colors": (
        "derive light and dark cloud colors from base sky, ground illuminance and directional scattering",
        "cloud faces receive atmospheric/celestial color while shadowed regions remain distinct",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Map_Cloud_Textures": (
        "map and pan authored cloud texture channels using MPC offsets, layer selection and quality switches",
        "cloud formations remain spatially mapped and evolve through authored texture channels",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Moon": (
        "evaluate Moon texture, phase detail, orientation basis, brightness and disk mask",
        "Moon disk/phase is available to suppress overlapping Sun contribution and preserve celestial ordering",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Moon_Centered_Gradient": (
        "form a saturated radial field centered on the normalized Moon vector",
        "Moon-proximal cloud and sky effects stay world-direction anchored",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/SC_DirectionalScattering": (
        "sample authored directional-scattering textures from world/light angular coordinates",
        "atmosphere and ground illumination vary with light direction rather than a fixed tint",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Scale_Intensity_Around_Sun": (
        "scale an input signal around Sun UV coordinates with lit-intensity and shading-path branches",
        "Sun-local highlight intensity follows the authored radial mapping",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Scale_Radial_Gradient_Around_White": (
        "rescale distance from white in a saturated radial gradient",
        "Sun/Moon highlight widths change structurally rather than by global opacity",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Shading_Gradients": (
        "build wide, shine, Sun-glow and Moon-glow fields from centered gradients and authored scales",
        "cloud lighting receives separate broad, edge-shine and celestial glow terms",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Sky_Utilities/Active_Sun_or_Moon_Vector": (
        "select the active Sun or Moon direction and produce a horizon changeover mask",
        "directional sky effects hand over smoothly and vanish when neither source is above horizon",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Sun_Centered_Gradient": (
        "form a saturated radial field from world direction and normalized Sun vector",
        "Sun-facing glow and cloud lighting stay anchored to the world-space Sun direction",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Sun_Disk": (
        "evaluate angular Sun disk, softness, color, reflection/DBuffer branches and Moon/cloud occlusion",
        "Sun is a world-direction disk whose visibility is reduced by authored static/dynamic clouds and Moon mask",
    ),
    "/Game/UltraDynamicSky/Materials/Material_Functions/Sun_Shine_Edges": (
        "combine cloud edge gradient, Sun gradient, shine power and cloud light/dark colors",
        "Sun-facing cloud edges brighten while non-facing/shadow regions retain authored color separation",
    ),
}

RICH_CURVE_SOURCE = {
    "api": "https://dev.epicgames.com/documentation/en-us/unreal-engine/API/Runtime/Engine/Curves/FRichCurve/Eval?application_version=5.5",
    "evalSource": "Engine/Source/Runtime/Engine/Private/Curves/RichCurve.cpp",
    "evalSourceSha": "9ffaef5e38a980607024f8f0c7b255c47484153a",
    "segmentSource": "Engine/Source/Runtime/Engine/Private/Curves/CurveEvaluation.cpp",
    "segmentSourceSha": "1f38aaef5a4a8bd0308df8bf1d3b22b15b332633",
}


def read_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as stream:
        return json.load(stream)


def decoded_property(contract: dict[str, Any], name: str) -> dict[str, Any]:
    matches = [
        prop
        for export in contract.get("exports", [])
        for prop in export.get("properties", [])
        if prop.get("name") == name and prop.get("decode_status") in ("decoded", "decoded_native")
    ]
    if len(matches) != 1:
        raise AssertionError(f"expected one decoded {name!r} property, found {len(matches)}")
    return matches[0]


def astronomy_source_assets(dataset: Path) -> dict[str, Any]:
    """Publish only source-backed values needed by the decoded real-Sun path.

    Physical paths and raw offsets are deliberately excluded from the public contract. Calendar
    arrays are derived only through the decoded Static Properties - Calendar writer.
    """
    equation = read_json(dataset / EQUATION_OF_TIME_CONTRACT)
    gregorian = read_json(dataset / GREGORIAN_CALENDAR_CONTRACT)
    calendar = read_json(dataset / UDS_CALENDAR_CONTRACT)
    if equation.get("status") != "VERIFIED":
        raise AssertionError("Equation_of_Time curve is not exactly decoded")
    channels = equation.get("channels", [])
    if len(channels) != 1 or channels[0].get("status") != "VERIFIED":
        raise AssertionError("Equation_of_Time must contain one verified channel")
    curve_keys = [
        {key: value for key, value in item.items() if key != "provenance"}
        for item in channels[0].get("keys", [])
    ]
    if len(curve_keys) != 13:
        raise AssertionError("Equation_of_Time authored key count changed")

    months = decoded_property(gregorian, "Months")["value"]
    leap_year = decoded_property(calendar, "Uses Leap Year")["value"]
    leap_month = decoded_property(calendar, "Leap Year Month")["value"]
    winter_offset = decoded_property(calendar, "Winter Solstice Offset")["value"]
    month_lengths = [int(item["value"]) for item in months["entries"]]
    leap_month_lengths = list(month_lengths)
    if leap_year:
        leap_month_lengths[leap_month - 1] += 1

    def start_days(lengths: list[int]) -> list[int]:
        total = 0
        result = []
        for length in lengths:
            result.append(total)
            total += length
        return result

    regular_start_days = start_days(month_lengths)
    leap_start_days = start_days(leap_month_lengths)
    return {
        "status": "PARTIAL",
        "implementationStatus": "NOT_IMPLEMENTED",
        "equationOfTime": {
            "status": "VERIFIED",
            "sourceAsset": EQUATION_OF_TIME_ASSET,
            "sourceSha256": equation["source"]["sha256"],
            "channel": channels[0].get("name"),
            "property": channels[0].get("property"),
            "keyCount": len(curve_keys),
            "keys": curve_keys,
        },
        "calendar": {
            "status": "PARTIAL",
            "sourceAssets": [GREGORIAN_CALENDAR_ASSET, UDS_CALENDAR_ASSET],
            "sourceSha256": {
                GREGORIAN_CALENDAR_ASSET: gregorian["source"]["sha256"],
                UDS_CALENDAR_ASSET: calendar["source"]["sha256"],
            },
            "verifiedSerializedInputs": {
                "Months": months,
                "Uses Leap Year": leap_year,
                "Leap Year Month": leap_month,
                "Winter Solstice Offset": winter_offset,
            },
            "candidateDefaultRuntimeDerivedValues": {
                "status": "PARTIAL_LOCAL_DAY_COUNT_DEFAULT_NOT_YET_ENGINE_VERIFIED",
                "requiredInitialAccumulator": 0,
                "Month Lengths": month_lengths,
                "Month Lengths (Leap Year)": leap_month_lengths,
                "Day Count At Start of Each Month": regular_start_days,
                "Day Count At Start of Each Month (Leap Year)": leap_start_days,
                "Number of Days in Year": sum(month_lengths),
            },
            "derivationSource": f"{BLUEPRINT_ASSET}: Static Properties - Calendar",
            "derivationStatus": "VERIFIED_UDS_GRAPH_WITH_SYMBOLIC_INITIAL_ACCUMULATOR_D0",
            "unresolved": [
                "UE Blueprint VM initialization semantics for the empty serialized local Day Count default"
            ],
        },
    }


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def nested_field(value: Any, name: str) -> Any:
    if not isinstance(value, dict):
        return None
    for item in value.get("properties", []):
        if item.get("name") == name:
            return item.get("value")
    return None


def _raw_node_operation(node: dict[str, Any]) -> str | None:
    return nested_field(
        node.get("properties", {}).get("FunctionReference"), "MemberName"
    )


def _raw_node_pin(node: dict[str, Any], name: str) -> dict[str, Any]:
    pin = next((item for item in node.get("pins", []) if item.get("name") == name), None)
    if pin is None:
        raise AssertionError(f"missing pin {name!r} on Blueprint node {node.get('export_index')}")
    return pin


def _verified_exec_path(
    raw_graphs: dict[str, dict[str, Any]], graph_name: str, path: list[int]
) -> dict[str, Any]:
    graph = raw_graphs[graph_name]
    nodes = {node["export_index"]: node for node in graph.get("nodes", [])}
    missing = [index for index in path if index not in nodes]
    if missing:
        raise AssertionError(f"missing {graph_name} path nodes: {missing}")
    for source_index, target_index in zip(path, path[1:]):
        source = nodes[source_index]
        linked = any(
            link.get("owning_node_index") == target_index
            for pin in source.get("pins", [])
            if pin.get("direction") == "output"
            and pin.get("type", {}).get("category") == "exec"
            for link in pin.get("linked_to", [])
        )
        if not linked:
            raise AssertionError(
                f"broken exact exec path in {graph_name}: {source_index}->{target_index}"
            )
    return {
        "sourceGraph": graph["graph"],
        "status": "VERIFIED",
        "nodes": path,
        "operations": [
            {
                "node": index,
                "class": nodes[index].get("class"),
                "operation": _raw_node_operation(nodes[index]),
                "customEvent": nodes[index].get("properties", {}).get(
                    "CustomFunctionName"
                ),
            }
            for index in path
        ],
    }


def build_calendar_pre_first_use_audit(
    raw_graphs: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    apply_location = raw_graphs["Apply Location Preset"]
    apply_nodes = {node["export_index"]: node for node in apply_location["nodes"]}
    check_dst_call = apply_nodes[1980]
    if _raw_node_operation(check_dst_call) != "Check for Daylight Savings Time":
        raise AssertionError("pre-startup daylight-saving call changed")
    authored_hour = _raw_node_pin(check_dst_call, "Hour").get("default_value")

    check_dst = raw_graphs["Check for Daylight Savings Time"]
    check_nodes = {node["export_index"]: node for node in check_dst["nodes"]}
    hour_guard = check_nodes[7240]
    if _raw_node_operation(hour_guard) != "EqualEqual_IntInt":
        raise AssertionError("daylight-saving astronomy guard changed")
    astronomy_hour = _raw_node_pin(hour_guard, "B").get("default_value")
    if authored_hour != "0" or astronomy_hour != "100":
        raise AssertionError("pre-startup astronomy rejection constants changed")

    bindings = raw_graphs["Set Up Internal Bindings"]
    binding_nodes = {node["export_index"]: node for node in bindings["nodes"]}
    create_delegate = binding_nodes[4988]
    add_delegate = binding_nodes[1336]
    if (
        create_delegate.get("properties", {}).get("SelectedFunctionName")
        != "Find Real Sunset/Sunrise Times"
        or nested_field(
            add_delegate.get("properties", {}).get("DelegateReference"),
            "MemberName",
        )
        != "Date Changed"
    ):
        raise AssertionError("Date Changed astronomy binding changed")

    notify_paths = [
        _verified_exec_path(
            raw_graphs,
            "Notify Events",
            [entry, 3300, 3286, 6458, 12896, 1640],
        )
        for entry in (5003, 5004, 5005)
    ]
    payload = {
        "status": "PARTIAL",
        "implementationStatus": "NOT_IMPLEMENTED",
        "authoredStartupCalendarPath": _verified_exec_path(
            raw_graphs,
            "Update Static Variables",
            [5809, 5327, 4232],
        ),
        "preStartupConstructionAudit": {
            "status": "VERIFIED_AUTHORED_PATH_DOES_NOT_REACH_ASTRONOMY",
            "sourceCall": {
                "graph": apply_location["graph"],
                "node": 1980,
                "operation": "Check for Daylight Savings Time",
                "Hour": int(authored_hour),
            },
            "astronomyGuard": {
                "graph": check_dst["graph"],
                "node": 7240,
                "operation": "EqualEqual_IntInt",
                "requiredHour": int(astronomy_hour),
            },
            "reason": "the authored construction call supplies Hour=0 while the only decoded astronomy branch requires Hour==100",
        },
        "dateChangedBinding": {
            "status": "VERIFIED",
            "sourceGraph": bindings["graph"],
            "createDelegateNode": 4988,
            "boundFunction": "Find Real Sunset/Sunrise Times",
            "addDelegateNode": 1336,
            "delegate": "Date Changed",
            "bindingExecPath": _verified_exec_path(
                raw_graphs,
                "Set Up Internal Bindings",
                [5721, 5266, 5264, 5265, 5267, 1336],
            ),
            "broadcastPaths": notify_paths,
        },
        "externalPublicInvocationCoverage": {
            "status": "PARTIAL",
            "reason": "static UDS assets cannot constrain arbitrary external/public calls before authored Startup Sky",
        },
        "semanticLevel": "EXACT_AUTHORED_STARTUP_AND_DATE_CHANGED_PATHS_NOT_GLOBAL_EXTERNAL_CALL_ORDER",
    }
    payload["contractSha256"] = sha256_bytes(
        json.dumps(payload, ensure_ascii=False, sort_keys=True).encode("utf-8")
    )
    return payload


def build_sun_scheduling_contract(
    blueprint: dict[str, Any], raw_graphs: dict[str, dict[str, Any]]
) -> dict[str, Any]:
    bytecode_functions = {
        item["name"]: item for item in blueprint.get("bytecode", {}).get("functions", [])
    }
    bytecode_names = (
        "Bind Events to Tick",
        "Cache Properties",
        "Editor Tick",
        "Hard Reset Cache",
        "Increment Cache Timer",
        "Startup Sky",
        "Update Active Variables",
    )
    bytecode = []
    for name in bytecode_names:
        function = bytecode_functions[name]
        script = function["script"]
        if function.get("status") != "VERIFIED" or not all(
            script.get("validation", {}).values()
        ):
            raise AssertionError(f"scheduling bytecode is not verified: {name}")
        bytecode.append(
            {
                "function": name,
                "status": "VERIFIED",
                "exportIndex": function["export_index"],
                "storageSha256": script["storage_sha256"],
                "bytecodeBufferSize": script["bytecode_buffer_size"],
                "serializedScriptSize": script["serialized_script_size"],
                "expressionCount": script["expression_count"],
                "validation": script["validation"],
            }
        )

    def graph_nodes(name: str) -> dict[int, dict[str, Any]]:
        return {
            node["export_index"]: node for node in raw_graphs[name].get("nodes", [])
        }

    def expect_operation(graph: str, node_index: int, operation: str) -> dict[str, Any]:
        node = graph_nodes(graph)[node_index]
        if _raw_node_operation(node) != operation:
            raise AssertionError(
                f"scheduling operation changed: {graph}[{node_index}] != {operation}"
            )
        return node

    startup_nodes = graph_nodes("Startup Sky")
    startup_cache = expect_operation("Startup Sky", 3563, "Cache Properties")
    startup_cache_group = _raw_node_pin(startup_cache, "Cache Group").get(
        "default_value"
    )
    startup_fill = _raw_node_pin(startup_cache, "Starting Cache Fill").get(
        "default_value"
    )
    active_speed_node = startup_nodes[13026]
    active_speed_pin = _raw_node_pin(active_speed_node, "Active Update Speed")
    expect_operation("Startup Sky", 3544, "Update Active Variables")
    expect_operation("Startup Sky", 3569, "Bind Events to Tick")
    expect_operation("Startup Sky", 3580, "Size Cache Arrays")
    if (
        startup_cache_group != "-1"
        or startup_fill != "true"
        or active_speed_pin.get("default_value") != "4"
    ):
        raise AssertionError("authored startup cache constants changed")

    bind_nodes = graph_nodes("Bind Events to Tick")
    runtime_bindings = []
    for node in bind_nodes.values():
        if node.get("class") != "K2Node_CreateDelegate":
            continue
        selected = node.get("properties", {}).get("SelectedFunctionName")
        for pin in node.get("pins", []):
            if pin.get("direction") != "output" or pin.get("type", {}).get(
                "category"
            ) != "delegate":
                continue
            for link in pin.get("linked_to", []):
                consumer = bind_nodes[link["owning_node_index"]]
                delegate = nested_field(
                    consumer.get("properties", {}).get("DelegateReference"),
                    "MemberName",
                )
                if delegate == "Runtime Tick":
                    runtime_bindings.append(
                        {
                            "createDelegateNode": node["export_index"],
                            "function": selected,
                            "addDelegateNode": consumer["export_index"],
                            "delegate": delegate,
                        }
                    )
    runtime_bindings.sort(key=lambda item: item["createDelegateNode"])
    if len(runtime_bindings) != 17:
        raise AssertionError("Runtime Tick binding set changed")

    binding_nodes = graph_nodes("Set Apply Property Event Binding")
    add_delegate_by_node = {
        index: nested_field(
            node.get("properties", {}).get("DelegateReference"), "MemberName"
        )
        for index, node in binding_nodes.items()
        if node.get("class") == "K2Node_AddDelegate"
    }
    add_switch = binding_nodes[8060]
    update_group_mapping = {}
    for pin in add_switch.get("pins", []):
        if not pin.get("name", "").startswith("NewEnumerator"):
            continue
        links = pin.get("linked_to", [])
        update_group_mapping[pin["name"]] = (
            add_delegate_by_node.get(links[0]["owning_node_index"]) if links else None
        )
    expected_group_mapping = {
        **{
            f"NewEnumerator{index}": f"High Priority Updates {index + 1}"
            for index in range(4)
        },
        **{
            f"NewEnumerator{index + 4}": f"Low Priority Updates {index + 1}"
            for index in range(8)
        },
        "NewEnumerator13": None,
        "NewEnumerator14": "Max Priority Updates",
    }
    if update_group_mapping != expected_group_mapping:
        raise AssertionError("UDS update-group delegate mapping changed")

    cache_group_paths = {
        "0": {
            "role": "Sun/Moon orientation",
            "writers": [
                {
                    "function": "Cache Sun and Moon Orientation",
                    "path": _verified_exec_path(
                        raw_graphs, "Cache Properties", [8062, 2236]
                    ),
                }
            ],
        },
        "1": {
            "role": "Sun radius source cache",
            "writers": [
                {
                    "function": "Current Sun Radius -> Cached Sun Scale",
                    "path": _verified_exec_path(
                        raw_graphs,
                        "Cache Properties",
                        [8062, 6699, 5170, 12596, 12595, 12603],
                    ),
                }
            ],
        },
        "5": {
            "role": "Sun light, disk and radius callbacks",
            "writers": [
                {
                    "function": "Current Sun Light Intensity",
                    "cacheNode": 2240,
                    "path": _verified_exec_path(
                        raw_graphs, "Cache Properties", [8062, 6681, 2240]
                    ),
                    "property": "NewEnumerator41",
                    "tolerance": 0.00001,
                    "updateGroup": "NewEnumerator14",
                    "callback": "AP - Sun Light Intensity",
                },
                {
                    "function": "Current Sun Light Color",
                    "cacheNode": 2255,
                    "path": _verified_exec_path(
                        raw_graphs,
                        "Cache Properties",
                        [8062, 6681, 2240, 2384, 2368, 2256, 2389, 2255],
                    ),
                    "property": "NewEnumerator45",
                    "tolerance": 0.0001,
                    "updateGroup": "NewEnumerator2",
                    "callback": "AP - Sun Light Color",
                },
                {
                    "function": "Current Sun Disk Color (transitively Current Sun Disk Intensity)",
                    "cacheNode": 2367,
                    "path": _verified_exec_path(
                        raw_graphs,
                        "Cache Properties",
                        [
                            8062,
                            6681,
                            2240,
                            2384,
                            2368,
                            2256,
                            2389,
                            2255,
                            2397,
                            2367,
                        ],
                    ),
                    "property": "NewEnumerator34",
                    "tolerance": 0.005,
                    "updateGroup": "NewEnumerator9",
                    "callback": "AP - Sun Disk Color",
                },
                {
                    "function": "Cached Sun Scale",
                    "cacheNode": 2368,
                    "property": "NewEnumerator35",
                    "tolerance": 0.00001,
                    "updateGroup": "NewEnumerator9",
                    "callback": "AP - Sun Radius",
                },
            ],
        },
        "8": {
            "role": "Sun-height-dependent 2D cloud highlight callbacks",
            "sourceNodes": [2332, 2366, 2375],
            "status": "VERIFIED_GRAPH_TOPOLOGY",
        },
    }

    payload = {
        "schema": "solum.p63.10.uds-sun-scheduling-contract/v1",
        "status": "PARTIAL",
        "implementationStatus": "NOT_IMPLEMENTED",
        "bytecodeEvidence": bytecode,
        "startup": {
            "status": "VERIFIED_AUTHORED_BLUEPRINT_ORDER",
            "cacheCall": {
                "sourceGraph": raw_graphs["Startup Sky"]["graph"],
                "node": 3563,
                "cacheGroup": int(startup_cache_group),
                "startingCacheFill": True,
                "meaning": "Cache Properties iterates active groups 0..9 before first authored Update Active Variables",
            },
            "activeUpdateSpeed": {"node": 13026, "value": 4},
            "updateActiveVariablesNode": 3544,
            "bindEventsToTickNode": 3569,
            "firstCallbacks": {
                "status": "VERIFIED_WITHIN_AUTHORED_GRAPH",
                "behavior": (
                    "starting fill updates color/float caches, queues callbacks in Immediate and "
                    "Unrepeated Updates, then Cache Properties broadcasts that delegate before returning"
                ),
                "cacheColorNodes": [1317, 2164],
                "cacheFloatNodes": [1318, 2168],
                "broadcastNode": 1621,
            },
            "engineLifecycleOrder": {
                "status": "PARTIAL",
                "reason": "ReceiveBeginPlay/first ReceiveTick order is engine semantics outside extracted Blueprint bytecode",
            },
        },
        "runtimeTick": {
            "status": "PARTIAL_ENGINE_MULTICAST_ORDER",
            "graphFact": "Runtime Tick graph is an empty delegate signature, not an executable scheduling function",
            "bindings": runtime_bindings,
            "bindingConditionsSource": raw_graphs["Bind Events to Tick"]["graph"],
            "verifiedAuthoredBindingOrder": [
                "optional weather/system-time/time-animation/player-occlusion",
                "Get Runtime Camera Transform",
                "Camera Location Dependent Updates",
                "Update Common Derivatives",
                "Monitor for Changes",
                "optional time-specific modifiers",
                "Increment Cache Timer or Cinematic Runtime Update",
                "Update Active Variables",
                "optional Update Path Tracer Fog",
            ],
            "unknown": [
                "engine multicast callback invocation-order guarantee",
                "concrete runtime branch values",
                "external UDW message cadence",
            ],
        },
        "incrementCacheTimer": {
            "status": "VERIFIED_FORMULA_PARTIAL_INITIAL_STATE",
            "sourceGraph": raw_graphs["Increment Cache Timer"]["graph"],
            "changeSpeed": {
                "conditionalSpeed": {
                    "if": {
                        "or": [
                            "Transitioning Sky Light Intensity",
                            "Modifiers Animating",
                            "Fast Cache Toggle",
                        ]
                    },
                    "then": {
                        "select": ["Fast Cache Toggle", "Fast Cache Toggle Speed", 0.4]
                    },
                    "else": 0.0,
                },
                "result": {
                    "max": [
                        "Time of Day Change Speed",
                        "Composite Weather Change Speed",
                        "Composite Context Change Speed",
                        "conditionalSpeed",
                    ]
                },
                "sourceNodes": [3184, 3177, 4683, 4684],
            },
            "hardResetGate": {
                "if": {
                    "greater": [
                        {"multiply": ["changeSpeed", "Tick Delta Seconds"]},
                        "Hard Cache Reset Change Speed Threshold",
                    ]
                },
                "then": "Hard Reset Cache and return",
                "sourceNodes": [7547, 7534, 6426, 3176],
            },
            "adaptiveCadence": {
                "targetPeriodSeconds": {
                    "clamp": [
                        {"divide": [0.35, {"max": ["Delayed Change Speed", 0.000001]}]},
                        "Min Property Cache Period",
                        "Max Property Cache Period",
                    ]
                },
                "cacheStepsMultiplier": {
                    "clamp": [
                        {"subtract": [3, {"floor": {"divide": ["targetPeriodSeconds", 0.15]}}]},
                        1,
                        3,
                    ]
                },
                "currentCacheTimerSpeed": {
                    "divide": [
                        1.0,
                        {"multiply": ["targetPeriodSeconds", "cacheStepsMultiplier"]},
                    ]
                },
                "activeUpdateSpeed": {
                    "clamp": [
                        {
                            "round": {
                                "multiply": [
                                    "Delayed Change Speed",
                                    "Tick Delta Seconds",
                                    125.0,
                                ]
                            }
                        },
                        "Minimum Active Update Speed",
                        3,
                    ]
                },
            },
            "cacheGroupRotation": {
                "iterations": "Cache Steps Multiplier, exact ForLoop range 1..M",
                "newTimer9": {
                    "clamp": [
                        {
                            "add": [
                                "Cache Group Timers[8] after removing index 0",
                                {
                                    "multiply": [
                                        "Tick Delta Seconds",
                                        "Current Cache Timer Speed",
                                    ]
                                },
                            ]
                        },
                        0.0,
                        1.0,
                    ]
                },
                "cacheCall": (
                    "when Cache Properties Step <= 9, call Cache Properties(step,false), "
                    "then increment step"
                ),
            },
            "verifiedAuthoredDefaults": {
                "Cache Group Timers": [0.0] * 10,
                "Cache Group Timers Clear": [0.0] * 10,
                "Cache Steps Multiplier": 1,
                "Current Cache Timer Speed": 10.0,
                "Fast Cache Toggle Speed": 1.3,
                "Hard Cache Reset Change Speed Threshold": 0.35,
                "Min Property Cache Period seconds": 0.1,
                "Max Property Cache Period seconds": 1.5,
                "Change Speed Rolling Buffer resized length": 30,
            },
            "unresolvedInitialState": [
                "Cache Properties Step",
                "resized Change Speed Rolling Buffer element values",
                "Minimum Active Update Speed",
                "empty reset item added at node 1521",
            ],
        },
        "cacheGroups": cache_group_paths,
        "updateGroupDelegateMapping": {
            "status": "VERIFIED",
            "mapping": update_group_mapping,
            "sourceGraph": raw_graphs["Set Apply Property Event Binding"]["graph"],
            "addSwitchNode": 8060,
        },
        "updateActiveVariables": {
            "status": "VERIFIED",
            "sourceGraph": raw_graphs["Update Active Variables"]["graph"],
            "alwaysFirst": [
                "Update Cloud Coverage Material Parameters",
                "Max Priority Updates",
            ],
            "fullSpeed4": [
                "High Priority Updates 1..4",
                "Low Priority Updates 1..8",
                "High Priority Update Step=0",
                "Low Priority Set Toggle=true",
            ],
            "adaptive": {
                "iterations": "Active Update Speed + 1",
                "step0": "HP1 + (toggle ? LP1 : LP5)",
                "step1": "HP2 + (toggle ? LP2 : LP6)",
                "step2": "HP3 + (toggle ? LP3 : LP7)",
                "step3": "HP4 + (toggle ? LP4 : LP8), then invert toggle",
            },
        },
        "hardReset": {
            "status": "VERIFIED",
            "condition": "Run Context != NewEnumerator2",
            "actions": [
                "Cache Properties(-1,true)",
                "Cache Properties Step=0",
                "Cache Group Timers=Cache Group Timers Clear",
            ],
            "sourceNodes": [5112, 6425, 3164, 12835, 12836],
        },
        "editorTick": {
            "status": "PARTIAL_EXTERNAL_CADENCE",
            "condition": "!RuntimeOrInitializing && Level Editor Tick",
            "authoredOrder": [
                "Tick Delta Seconds=GetWorldDeltaSeconds",
                "Monitor for Changes",
                "Increment Cache Timer",
                "Update Active Variables",
            ],
            "unknown": "actual cadence comes from an external editor tick handler",
        },
        "completionBlockers": [
            "four timer initial/reset values remain source-unproven",
            "engine multicast invocation and actor lifecycle order are not yet source-backed",
            "external editor/UDW/public call cadence remains unconstrained",
        ],
        "prohibitedSimplifications": [
            "replace adaptive cache groups with an undocumented every-frame update",
            "skip startup Cache Properties(-1,true) and immediate callbacks",
            "merge Max/High/Low update groups",
            "assume unresolved initial values are zero",
        ],
    }
    payload["contractSha256"] = sha256_bytes(
        json.dumps(payload, ensure_ascii=False, sort_keys=True).encode("utf-8")
    )
    return payload


def compact_type(value: Any) -> dict[str, Any]:
    if not isinstance(value, dict):
        return {"category": "UNKNOWN", "subcategory": "UNKNOWN"}
    return {
        "category": value.get("category", "UNKNOWN"),
        "subcategory": value.get("subcategory", "UNKNOWN"),
        "object": value.get("subcategory_object"),
        "container": value.get("container", "none"),
    }


def normalize_node(node: dict[str, Any]) -> dict[str, Any]:
    properties = node.get("properties", {})
    operation = nested_field(properties.get("FunctionReference"), "MemberName")
    operation_parent = nested_field(properties.get("FunctionReference"), "MemberParent")
    variable = nested_field(properties.get("VariableReference"), "MemberName")
    result: dict[str, Any] = {
        "exportIndex": node.get("export_index"),
        "name": node.get("name"),
        "class": node.get("class"),
        "operation": operation,
        "operationParent": operation_parent,
        "variable": variable,
        "comment": properties.get("NodeComment"),
        "position": {
            "x": properties.get("NodePosX"),
            "y": properties.get("NodePosY"),
        },
        "pins": [],
    }
    for pin in node.get("pins", []):
        reference = pin.get("reference", {})
        links = []
        for link in pin.get("linked_to", []):
            links.append(
                {
                    "nodeIndex": link.get("owning_node_index"),
                    "node": link.get("owning_node"),
                    "pinId": link.get("pin_id"),
                }
            )
        result["pins"].append(
            {
                "id": reference.get("pin_id"),
                "name": pin.get("name"),
                "direction": pin.get("direction"),
                "type": compact_type(pin.get("type")),
                "default": pin.get("default_value"),
                "autogeneratedDefault": pin.get("autogenerated_default_value"),
                "defaultObject": pin.get("default_object"),
                "links": links,
            }
        )
    return result


def normalized_graph(
    graph: dict[str, Any], source_asset: str = BLUEPRINT_ASSET
) -> dict[str, Any]:
    nodes = [normalize_node(node) for node in graph.get("nodes", [])]
    edges = [
        {
            "fromNode": edge.get("from_node"),
            "fromPinId": edge.get("from_pin_id"),
            "fromPin": edge.get("from_pin"),
            "toNode": edge.get("to_node"),
            "toPinId": edge.get("to_pin_id"),
            "toPin": edge.get("to_pin"),
            "targetResolved": edge.get("target_resolved"),
        }
        for edge in graph.get("edges", [])
    ]
    canonical = json.dumps(
        {"nodes": nodes, "edges": edges}, ensure_ascii=False, sort_keys=True
    ).encode("utf-8")
    return {
        "sourceAsset": source_asset,
        "sourceName": graph["graph"].rsplit(".", 1)[-1],
        "sourceGraph": graph["graph"],
        "nodeCount": len(nodes),
        "edgeCount": len(edges),
        "topologySha256": sha256_bytes(canonical),
        "nodes": nodes,
        "edges": edges,
    }


def variable_reference(node: dict[str, Any]) -> str | None:
    return nested_field(node.get("properties", {}).get("VariableReference"), "MemberName")


def blueprint_value_type(pin_type: dict[str, Any]) -> str:
    category = pin_type.get("category")
    subcategory = pin_type.get("subcategory")
    if category == "bool":
        return "BoolProperty"
    if category == "real" and subcategory == "double":
        return "DoubleProperty"
    return f"K2Pin<{category}:{subcategory}>"


def parameter_writer_contract(
    raw_graph: dict[str, Any], parameter: str, formula: dict[str, Any]
) -> dict[str, Any]:
    by_index = {node["export_index"]: node for node in raw_graph.get("nodes", [])}
    setters = [
        node
        for node in raw_graph.get("nodes", [])
        if node.get("class") == "K2Node_VariableSet"
        and variable_reference(node) == parameter
    ]
    if not setters:
        raise KeyError(f"missing writer for derived Sun parameter: {parameter}")
    value_pins = [
        next(
            (
                pin
                for pin in setter.get("pins", [])
                if pin.get("direction") == "input" and pin.get("name") == parameter
            ),
            None,
        )
        for setter in setters
    ]
    if any(pin is None for pin in value_pins):
        raise KeyError(f"missing value pin for derived Sun parameter: {parameter}")
    source_indexes = sorted(
        {
            link.get("owning_node_index")
            for value_pin in value_pins
            for link in value_pin.get("linked_to", [])
            if isinstance(link.get("owning_node_index"), int)
        }
    )
    selected = {setter["export_index"] for setter in setters}
    queue = deque(source_indexes)
    while queue:
        index = queue.popleft()
        if index in selected or index not in by_index:
            continue
        selected.add(index)
        node = by_index[index]
        for pin in node.get("pins", []):
            if pin.get("direction") != "input":
                continue
            if pin.get("type", {}).get("category") in {"exec", "delegate", "object"}:
                continue
            queue.extend(
                link.get("owning_node_index")
                for link in pin.get("linked_to", [])
                if isinstance(link.get("owning_node_index"), int)
            )
    nodes = [normalize_node(by_index[index]) for index in sorted(selected)]
    edges = [
        {
            "fromNode": edge.get("from_node"),
            "fromPin": edge.get("from_pin"),
            "toNode": edge.get("to_node"),
            "toPin": edge.get("to_pin"),
            "targetResolved": edge.get("target_resolved"),
        }
        for edge in raw_graph.get("edges", [])
        if edge.get("from_node") in selected and edge.get("to_node") in selected
    ]
    source_operations = {
        node.get("operation") or node.get("variable") or node.get("class")
        for node in nodes
    }
    missing_formula_nodes = sorted(set(formula["sourceNodes"]) - source_operations)
    pin_type = compact_type(value_pins[0].get("type"))
    inconsistent_types = [
        compact_type(pin.get("type"))
        for pin in value_pins[1:]
        if compact_type(pin.get("type")) != pin_type
    ]
    topology_verified = (
        not missing_formula_nodes
        and not inconsistent_types
        and all(
            pin.get("linked_to") or pin.get("default_value") != ""
            for pin in value_pins
        )
        and not any(not edge["targetResolved"] for edge in edges)
    )
    formula_status = formula.get("status", "VERIFIED")
    payload = {
        "parameter": parameter,
        "status": formula_status if topology_verified else "PARTIAL",
        "topologyStatus": "VERIFIED" if topology_verified else "PARTIAL",
        "formulaStatus": formula_status,
        "formulaStatusReason": formula.get("statusReason"),
        "sourceGraph": raw_graph["graph"],
        "setNodes": [setter["export_index"] for setter in setters],
        "valueOrigins": [
            {
                "setNode": setter["export_index"],
                "serializedDefault": pin.get("default_value"),
                "autogeneratedDefault": pin.get("autogenerated_default_value"),
                "connectedSourceNodes": [
                    link.get("owning_node_index") for link in pin.get("linked_to", [])
                ],
            }
            for setter, pin in zip(setters, value_pins)
        ],
        "valueType": pin_type,
        "resolvedPropertyType": blueprint_value_type(pin_type),
        "connectedSourceNodes": source_indexes,
        "formula": formula["equation"],
        "ast": formula["ast"],
        "formulaSourceNodes": formula["sourceNodes"],
        "missingFormulaNodes": missing_formula_nodes,
        "inconsistentWriterTypes": inconsistent_types,
        "nodes": nodes,
        "edges": edges,
        "source": f"{BLUEPRINT_ASSET}: {formula['graph']} K2 writer slice",
        "implementationStatus": "NOT_IMPLEMENTED",
    }
    payload["topologySha256"] = sha256_bytes(
        json.dumps(
            {"nodes": nodes, "edges": edges}, ensure_ascii=False, sort_keys=True
        ).encode("utf-8")
    )
    return payload


def parameter_writer_support_formula_ledger() -> list[dict[str, Any]]:
    formulas = {
        "Dimming Directional Lights": {
            "equation": (
                "DimDirectionalLightsWithCloudCoverage || !UseCloudShadows || ForwardShading || "
                "AdjustForPathTracer || FeatureLevel == FeatureLevel::NewEnumerator1"
            ),
            "sourceNodes": ["BooleanOR", "Not_PreBool", "K2Node_EnumEquality"],
        },
        "Fog and Dust Shadow Value": {
            "equation": (
                "1 - max(pow(MapRangeClamped(Fog, 4, 10, 0, 1), 2) * FogShadows, "
                "DustAmount * DustShadows)"
            ),
            "sourceNodes": ["MapRangeClamped", "MultiplyMultiply_FloatFloat", "FMax", "Multiply_DoubleDouble", "Subtract_DoubleDouble"],
        },
        "Directional Light Dimming": {
            "equation": (
                "DimmingDirectionalLights() ? pow(min(MapRangeClamped(LocalCloudCoverage, "
                "DimmingRange.Lower.Value, DimmingRange.Upper.Value, 1, 0), "
                "FogAndDustShadowValue()), DimmingRangeExponent) : 1"
            ),
            "sourceNodes": ["Dimming Directional Lights", "Fog and Dust Shadow Value", "MapRangeClamped", "FMin", "MultiplyMultiply_FloatFloat"],
        },
        "Directional Inscattering Multiplier": {
            "equation": (
                "shape = MapRangeClamped(abs(CachedSunVector.z), 0.1, 0.7, 0.25, 0.35) * "
                "SelectFloat(1, 0.5, UsingVolumetricClouds); overcast = "
                "Lerp(1, shape, pow(LocalOvercast01, 2)); fog = "
                "MapRangeClamped(Fog, 5, 10, 1, shape * 0.5); return overcast * fog"
            ),
            "sourceNodes": ["BreakVector", "Abs", "MapRangeClamped", "SelectFloat", "Lerp", "MultiplyMultiply_FloatFloat", "Multiply_DoubleDouble"],
        },
        "Get Cloud Coverage Local": {
            "equation": (
                "height = MapRangeClamped(CameraCloudLayerNormalizedHeight, 0.2, 1, 1, 0); "
                "coverage = CloudCoverageTargetInUse ? Lerp(CloudCoverage03, "
                "CloudCoverageAfterPainting, PaintedCoverageAffectsGlobalValues) : CloudCoverage03; "
                "return coverage * height"
            ),
            "sourceNodes": ["MapRangeClamped", "Lerp", "Multiply_DoubleDouble", "K2Node_IfThenElse"],
        },
        "Get Inverted Global Occlusion": {
            "equation": (
                "enabled = (RuntimeOrInitializing() || LevelEditorTick) && ApplyInteriorAdjustments; "
                "return enabled ? 1 - CurrentGlobalOcclusion() : 0"
            ),
            "sourceNodes": ["Runtime Or Initializing", "Current Global Occlusion", "BooleanOR", "BooleanAND", "Subtract_DoubleDouble", "K2Node_IfThenElse"],
        },
        "Runtime Or Initializing": {
            "equation": "RunContext > 1",
            "sourceNodes": ["Greater_ByteByte"],
        },
        "Three Time Floats": {
            "equation": (
                "sun = SunHeight(Cached); twilight = Lerp(Day, DawnDusk, "
                "MapRangeClamped(sun, 0.505, 0.635, 1, 0)); nightAlpha = "
                "MapRangeClamped(sun, 0.5, 0.466, 1 - EclipsePercent, 1); "
                "return Lerp(twilight, Night, nightAlpha)"
            ),
            "sourceNodes": ["Sun Height", "MapRangeClamped", "Lerp", "Subtract_DoubleDouble"],
        },
    }
    return [
        {
            "function": name,
            "status": formula.get("status", "VERIFIED"),
            "statusReason": formula.get("statusReason"),
            "source": f"{BLUEPRINT_ASSET}: {name}",
            "implementationStatus": "NOT_IMPLEMENTED",
            "equation": formula["equation"],
            "sourceNodes": formula["sourceNodes"],
        }
        for name, formula in formulas.items()
    ]


def external_support_formula_ledger() -> list[dict[str, Any]]:
    return [
        {
            "function": "Current Global Occlusion",
            "status": "VERIFIED",
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{PLAYER_OCCLUSION_ASSET}: Current Global Occlusion",
            "equation": (
                "return Lerp(OldOcclusionA.x, TargetOcclusionA.x, "
                "TracePeriodTimer / OcclusionUpdatePeriod)"
            ),
            "ast": {
                "return": {
                    "Lerp": [
                        {"BreakVector.x": "Old Occlusion A"},
                        {"BreakVector.x": "Target Occlusion A"},
                        {
                            "divide": [
                                "Trace Period Timer",
                                "Occlusion Update Period",
                            ]
                        },
                    ]
                }
            },
            "sourceNodes": [
                "BreakVector",
                "Lerp",
                "Divide_DoubleDouble",
                "Old Occlusion A",
                "Target Occlusion A",
                "Trace Period Timer",
                "Occlusion Update Period",
            ],
        }
    ]


def celestial_orientation_formula_ledger() -> list[dict[str, Any]]:
    """Source equations for the Sun orientation/eclipsing slice.

    A PARTIAL row means the listed topology/equation is exact but a called local function has not
    yet been reduced to equations. The raw dependency graph is still published alongside it.
    """
    return [
        {
            "function": "Solar Eclipse Circle Mask",
            "status": "VERIFIED",
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: Solar Eclipse Circle Mask",
            "equation": (
                "angle = DegAcos(dot(MoonVector, CachedSunVector)); moonLarger = "
                "MoonAngularDiameter > SunAngularDiameter; inner = moonLarger ? "
                "MoonAngularDiameter-SunAngularDiameter : SunAngularDiameter-MoonAngularDiameter; "
                "outer = MoonAngularDiameter+SunAngularDiameter; fullyCovered = moonLarger ? 0 : "
                "1-SafeDivide(MoonAngularDiameter, SunAngularDiameter); return "
                "MapRangeClamped(angle, inner, outer, fullyCovered, 1)"
            ),
            "ast": {
                "angle": {"DegAcos": {"dot": ["Moon Vector", "Cached Sun Vector"]}},
                "moonLarger": {"greater": ["Moon Angular Diameter", "Sun Angular Diameter"]},
                "inner": {
                    "select": [
                        "moonLarger",
                        {"subtract": ["Moon Angular Diameter", "Sun Angular Diameter"]},
                        {"subtract": ["Sun Angular Diameter", "Moon Angular Diameter"]},
                    ]
                },
                "outer": {"add": ["Moon Angular Diameter", "Sun Angular Diameter"]},
                "fullyCovered": {
                    "select": [
                        "moonLarger",
                        0.0,
                        {
                            "subtract": [
                                1.0,
                                {"SafeDivide": ["Moon Angular Diameter", "Sun Angular Diameter"]},
                            ]
                        },
                    ]
                },
                "return": {
                    "MapRangeClamped": ["angle", "inner", "outer", "fullyCovered", 1.0]
                },
            },
            "sourceNodes": [
                "Dot_VectorVector",
                "DegAcos",
                "SafeDivide",
                "Greater_DoubleDouble",
                "Subtract_DoubleDouble",
                "Add_DoubleDouble",
                "SelectFloat",
                "MapRangeClamped",
            ],
        },
        {
            "function": "Current Solar Eclipse Values",
            "status": "VERIFIED",
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: Current Solar Eclipse Values",
            "equation": (
                "if !(SolarEclipse && (SpaceLayerActive || dot(CachedSunVector, "
                "CachedMoonVector)>0.9)) return (1, white); SunAngularDiameter = "
                "RadiansToDegrees(CachedSunScale)*1.05; multiply SunFractionShowing by the "
                "authored Moon circle mask when RenderMoon and Sun/Moon vectors match within "
                "0.3; then multiply it for every active non-ring space planet using its "
                "component direction and authored apparent angular diameter; return "
                "(Lerp(SolarEclipseIntensityMultiplier,1,SunFractionShowing), "
                "LinearColorLerp(SolarEclipseTintColor,white,SunFractionShowing))"
            ),
            "ast": {
                "active": {
                    "and": [
                        "Solar Eclipse",
                        {
                            "or": [
                                "Space Layer Active",
                                {
                                    "greater": [
                                        {"dot": ["Cached Sun Vector", "Cached Moon Vector"]},
                                        0.9,
                                    ]
                                },
                            ]
                        },
                    ]
                },
                "inactiveReturn": [1.0, [1.0, 1.0, 1.0, 1.0]],
                "sunAngularDiameter": {
                    "multiply": [{"RadiansToDegrees": "Cached Sun Scale"}, 1.05]
                },
                "moonContributionCondition": {
                    "and": [
                        {"equalVector": ["Cached Sun Vector", "Cached Moon Vector", 0.3]},
                        "Render Moon",
                    ]
                },
                "moonContribution": {
                    "multiplyInto": [
                        "Sun Fraction Showing",
                        {
                            "Solar Eclipse Circle Mask": [
                                "Sun Angular Diameter",
                                {"divide": [{"RadiansToDegrees": "Cached Moon Scale"}, 2.0]},
                                "Cached Moon Vector",
                            ]
                        },
                    ]
                },
                "spacePlanetLoop": {
                    "if": "Space Layer Active",
                    "forEach": "Planets/Moons",
                    "when": {"not": "Space Planet.RingAroundLocalPlanet"},
                    "planetLocation": {"Normal": "Space Planet Component Location"},
                    "planetAngularDiameter": {
                        "DegAcos": {
                            "DotProduct2D": [
                                {"Normal2D": [3200.0, "Space Planet.Scale_9"]},
                                {"Normal2D": [3200.0, 0.0]},
                            ]
                        }
                    },
                    "multiplyInto": [
                        "Sun Fraction Showing",
                        {
                            "Solar Eclipse Circle Mask": [
                                "Sun Angular Diameter",
                                "planetAngularDiameter",
                                {"multiply": ["Planet Location", -1.0]},
                            ]
                        },
                    ],
                },
                "return": [
                    {"Lerp": ["Solar Eclipse Intensity Multiplier", 1.0, "Sun Fraction Showing"]},
                    {
                        "LinearColorLerp": [
                            "Solar Eclipse Tint Color",
                            [1.0, 1.0, 1.0, 1.0],
                            "Sun Fraction Showing",
                        ]
                    },
                ],
            },
            "sourceNodes": [
                "BooleanAND",
                "BooleanOR",
                "Dot_VectorVector",
                "Greater_DoubleDouble",
                "EqualEqual_VectorVector",
                "RadiansToDegrees",
                "Multiply_DoubleDouble",
                "Solar Eclipse Circle Mask",
                "Lerp",
                "LinearColorLerp",
                "K2Node_MacroInstance",
                "Normal",
                "Normal2D",
                "DotProduct2D",
                "DegAcos",
            ],
        },
        {
            "function": "H/M/S/MS to Time of Day",
            "status": "VERIFIED",
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: H/M/S/MS to Time of Day",
            "equation": (
                "q=100/60; fractionalSeconds=Seconds+Milliseconds/1000; return Hours*100 + "
                "Minutes*q + MapRangeClamped(fractionalSeconds,0,60,0,q)"
            ),
            "ast": {
                "q": {"divide": [100.0, 60.0]},
                "fractionalSeconds": {
                    "add": ["Seconds", {"divide": ["Milliseconds", 1000.0]}]
                },
                "return": {
                    "add": [
                        {"multiply": ["Hours", 100.0]},
                        {"multiply": ["Minutes", "q"]},
                        {
                            "MapRangeClamped": [
                                "fractionalSeconds",
                                0.0,
                                60.0,
                                0.0,
                                "q",
                            ]
                        },
                    ]
                },
            },
            "sourceNodes": [
                "Divide_DoubleDouble",
                "Add_DoubleDouble",
                "Multiply_DoubleDouble",
                "MapRangeClamped",
            ],
        },
        {
            "function": "Check If Year is Leap Year",
            "status": "VERIFIED",
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: Check If Year is Leap Year",
            "equation": "Calendar.UsesLeapYear && IsLeapYear(Year)",
            "ast": {"and": ["Calendar.Uses Leap Year", {"IsLeapYear": "Year"}]},
            "sourceNodes": ["Uses Leap Year", "IsLeapYear", "BooleanAND"],
        },
        {
            "function": "Current Month Lengths",
            "status": "VERIFIED",
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: Current Month Lengths",
            "equation": (
                "Select(Calendar.MonthLengths, Calendar.MonthLengthsLeapYear, "
                "CheckIfYearIsLeapYear(Year))"
            ),
            "ast": {
                "select": [
                    "Calendar.Month Lengths",
                    "Calendar.Month Lengths (Leap Year)",
                    {"Check If Year is Leap Year": "Year"},
                ]
            },
            "sourceNodes": [
                "Month Lengths",
                "Month Lengths (Leap Year)",
                "Check If Year is Leap Year",
                "K2Node_Select",
            ],
        },
        {
            "function": "Day Count at the Start of a Month",
            "status": "VERIFIED",
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: Day Count at the Start of a Month",
            "equation": (
                "Select(Calendar.RegularStartDayArray, Calendar.LeapStartDayArray, "
                "CheckIfYearIsLeapYear(Year))[Month-1]"
            ),
            "ast": {
                "arrayItem": [
                    {
                        "select": [
                            "Calendar.Day Count At Start of Each Month",
                            "Calendar.Day Count At Start of Each Month (Leap Year)",
                            {"Check If Year is Leap Year": "Year"},
                        ]
                    },
                    {"subtract": ["Month", 1]},
                ]
            },
            "sourceNodes": [
                "Day Count At Start of Each Month",
                "Day Count At Start of Each Month (Leap Year)",
                "Check If Year is Leap Year",
                "K2Node_Select",
                "Subtract_IntInt",
                "K2Node_GetArrayItem",
            ],
        },
        {
            "function": "Number of Days in a Year",
            "status": "VERIFIED",
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: Number of Days in a Year",
            "equation": (
                "N=Calendar.NumberOfDaysInYear; SelectInt(A=N+1,B=N,"
                "bPickA=CheckIfYearIsLeapYear(Year))"
            ),
            "ast": {
                "select": [
                    {"add": ["Calendar.Number of Days in Year", 1]},
                    "Calendar.Number of Days in Year",
                    {"Check If Year is Leap Year": "Year"},
                ]
            },
            "sourceNodes": [
                "Number of Days in Year",
                "Check If Year is Leap Year",
                "Add_IntInt",
                "SelectInt",
            ],
        },
        {
            "function": "Offset Date by a Number of Days",
            "status": "VERIFIED",
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: Offset Date by a Number of Days",
            "equation": (
                "m,d,y=inputs; forward=Offset>=0; repeat abs(Offset): s=sign(Offset); if "
                "d+s is within current month then d+=s, else advance/wrap month, advance year "
                "when wrapped, set d to first/last day by direction, and skip year zero"
            ),
            "ast": {
                "initial": {"month": "Month", "day": "Day", "year": "Year"},
                "forward": {"greaterEqual": ["Offset", 0]},
                "loop": {
                    "firstIndex": 1,
                    "lastIndex": {"abs": "Offset"},
                    "step": {"SignOfInteger": "Offset"},
                    "sameMonth": {
                        "InRangeInclusive": [
                            {"add": ["day", "step"]},
                            1,
                            {"CurrentMonthLengths": ["year", {"subtract": ["month", 1]}]},
                        ]
                    },
                    "monthWrap": {
                        "whenNextMonthOutOfRange": {
                            "year": {"add": ["year", "step"]},
                            "month": {
                                "select": [1, "Calendar.MonthLengths.length", "forward"]
                            },
                        },
                        "day": {
                            "select": [
                                1,
                                {"CurrentMonthLengths": ["year", "month-1"]},
                                "forward",
                            ]
                        },
                        "skipYearZero": {
                            "if": {"equal": ["year", 0]},
                            "then": {"year": "step"},
                        },
                    },
                },
                "return": ["month", "day", "year"],
            },
            "sourceNodes": [
                "GreaterEqual_IntInt",
                "Abs_Int",
                "SignOfInteger",
                "InRange_IntInt",
                "Current Month Lengths",
                "Array_Length",
                "Add_IntInt",
                "SelectInt",
                "EqualEqual_IntInt",
                "K2Node_MacroInstance",
            ],
        },
        {
            "function": "Static Properties - Calendar",
            "status": "PARTIAL",
            "statusReason": (
                "graph/bytecode order and all operations are exact; the first regular-prefix "
                "accumulator has an empty serialized local default and no explicit zero writer"
            ),
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: Static Properties - Calendar",
            "equation": (
                "if Calendar is invalid, assign the authored Gregorian_Calendar default; clear "
                "both month-length and both start-day arrays; set CalendarDataSaved=true; "
                "MonthLengths=MapValues(Calendar.Months); MonthLengthsLeap[i]=MonthLengths[i]+"
                "SelectInt(1,0,i+1==Calendar.LeapYearMonth); build regular and leap start-day "
                "arrays from the running DayCount; NumberOfDaysInYear=sum(MonthLengths); then "
                "ForceValidDay()"
            ),
            "ast": {
                "calendarFallback": {
                    "if": {"not": {"IsValid": "Calendar"}},
                    "then": {"set": ["Calendar", GREGORIAN_CALENDAR_ASSET]},
                },
                "clear": [
                    "Calendar.Month Lengths",
                    "Calendar.Month Lengths (Leap Year)",
                    "Calendar.Day Count At Start of Each Month",
                    "Calendar.Day Count At Start of Each Month (Leap Year)",
                ],
                "calendarDataSaved": True,
                "monthValues": {"Map_Values": "Calendar.Months"},
                "monthLoop": {
                    "forEach": "monthValues",
                    "Month Lengths.add": "Array Element",
                    "Month Lengths (Leap Year).add": {
                        "add": [
                            "Array Element",
                            {
                                "SelectInt": [
                                    1,
                                    0,
                                    {"equal": [{"add": ["Array Index", 1]}, "Leap Year Month"]},
                                ]
                            },
                        ]
                    },
                },
                "regularStartDayLoop": {
                    "Day Count": "D0 (empty serialized local default; engine initialization pending)",
                    "forEach": "Month Lengths",
                    "Day Count At Start of Each Month.add": "Day Count",
                    "Day Count.next": {"add": ["Day Count", "Array Element"]},
                    "onCompleted": {"Number of Days in Year": "Day Count"},
                },
                "leapStartDayLoop": {
                    "Day Count": 0,
                    "forEach": "Month Lengths (Leap Year)",
                    "Day Count At Start of Each Month (Leap Year).add": "Day Count",
                    "Day Count.next": {"add": ["Day Count", "Array Element"]},
                },
                "afterInitialization": {"call": "Force Valid Day"},
            },
            "sourceNodes": [
                "Calendar",
                "Calendar Data Saved",
                "Map_Values",
                "Array_Clear",
                "Array_Add",
                "Add_IntInt",
                "EqualEqual_IntInt",
                "SelectInt",
                "Month Lengths",
                "Month Lengths (Leap Year)",
                "Day Count At Start of Each Month",
                "Day Count At Start of Each Month (Leap Year)",
                "Number of Days in Year",
                "Force Valid Day",
                "K2Node_MacroInstance",
                "K2Node_ExecutionSequence",
            ],
        },
        {
            "function": "Force Valid Day",
            "status": "VERIFIED",
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: Force Valid Day",
            "equation": (
                "if Year==0 set Year=1; Month=Clamp(Month,1,len(Calendar.MonthLengths)); "
                "Day=Clamp(Day,1,CurrentMonthLengths(Year)[Month-1])"
            ),
            "ast": {
                "sequence": [
                    {
                        "if": {"equal": ["Year", 0]},
                        "then": {"setByReference": ["Year", 1]},
                    },
                    {
                        "setByReference": [
                            "Month",
                            {"Clamp": ["Month", 1, {"Array_Length": "Calendar.Month Lengths"}]},
                        ]
                    },
                    {
                        "setByReference": [
                            "Day",
                            {
                                "Clamp": [
                                    "Day",
                                    1,
                                    {
                                        "arrayItem": [
                                            {"Current Month Lengths": "Year"},
                                            {"subtract": ["Month", 1]},
                                        ]
                                    },
                                ]
                            },
                        ]
                    },
                ]
            },
            "sourceNodes": [
                "Year",
                "Month",
                "Day",
                "EqualEqual_IntInt",
                "Clamp",
                "Array_Length",
                "Current Month Lengths",
                "Subtract_IntInt",
                "K2Node_GetArrayItem",
                "K2Node_VariableSetRef",
                "K2Node_ExecutionSequence",
            ],
        },
        {
            "function": "Simulation Horizon Compensation",
            "status": "VERIFIED",
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: Simulation Horizon Compensation",
            "equation": (
                "dz=CompensateSimulationForFlatFogHorizon ? "
                "MapRangeClamped(abs(In.z),0,0.15,-0.02,0) : 0; return In+(0,0,dz)"
            ),
            "ast": {
                "dz": {
                    "select": [
                        "Compensate Simulation for Flat Fog Horizon",
                        {"MapRangeClamped": [{"abs": "In.z"}, 0.0, 0.15, -0.02, 0.0]},
                        0.0,
                    ]
                },
                "return": {"add": ["In", [0.0, 0.0, "dz"]]},
            },
            "sourceNodes": [
                "BreakVector",
                "Abs",
                "MapRangeClamped",
                "SelectFloat",
                "Add_VectorVector",
            ],
        },
        {
            "function": "AP - Sun Root Vector",
            "status": "PARTIAL",
            "statusReason": (
                "Sun World Rotation value path is exact; cache enum semantics, degree-threshold "
                "helper and atlas update side effects remain dependency graphs rather than equations"
            ),
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: AP - Sun Root Vector",
            "equation": (
                "ForwardVector = Normal(Vector(GetCachedColor(NewEnumerator13)),0.0001); "
                "SunWorldRotation = MakeRotFromXZ(ForwardVector,CachedSunZVector); write Sun Vector "
                "to MPC; update component rotation only through exact forced/periodic/angle-threshold "
                "branches; update periodic timestamp and atlas vectors on their decoded branches"
            ),
            "sourceNodes": [
                "Get Cached Color",
                "Normal",
                "MakeRotFromXZ",
                "SetVectorParameterValue",
                "K2_SetWorldRotation",
                "Lights Update Degree Threshold Test",
                "Update Atlas Light Vectors",
                "K2Node_IfThenElse",
            ],
        },
        {
            "function": "Cache Sun and Moon Orientation",
            "status": "PARTIAL",
            "statusReason": (
                "exact branch/write order and Calendar initialization are published and the "
                "real-Sun value path and adaptive scheduler formulas are reduced; engine "
                "lifecycle and four timer initial/reset values remain open"
            ),
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: Cache Sun and Moon Orientation",
            "equation": (
                "SetTimeCycleDegrees; optionally compute real Sun/Moon/stars; update dawn/dusk "
                "multiplier; select manual-target, simulated-real or authored manual-orbit Sun "
                "candidate; normalize CachedSunVector; cache it as color enum NewEnumerator13; "
                "then derive night filter and Sun Z vector"
            ),
            "sourceNodes": [
                "Set Time Cycle Degrees",
                "Approximate Real Sun Moon and Stars",
                "K2Node_ExecutionSequence",
                "K2Node_IfThenElse",
                "Normal",
                "Cache Color",
                "Night Filter",
                "Sun Z Vector",
            ],
        },
        {
            "function": "Approximate Real Sun Moon and Stars",
            "status": "PARTIAL",
            "statusReason": (
                "Sun-only topology, constants and seven local functions are exact; Calendar "
                "runtime-derived arrays remain unresolved, while Moon/stars branches are outside "
                "this checkpoint"
            ),
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: Approximate Real Sun Moon and Stars",
            "sunOutputPathStatus": "VERIFIED",
            "graphEvidence": {
                "nodeCount": 284,
                "edgeCount": 286,
                "graphStatus": "VERIFIED",
                "bytecodeStatus": "VERIFIED",
                "bytecodeInstructionsDecoded": 538,
                "bytecodeInstructionCount": 538,
                "unsupportedBytecodeInstructions": 0,
            },
            "equation": (
                "Resolve UTC date/time from system UTC or authored local time/DST/timezone; derive "
                "dayIndex, seasonalTime and authored Equation_of_Time offset; compose latitude/"
                "longitude, solar yaw and 23.439199-degree axial tilt; construct Earth-center and "
                "viewer vectors; unrotate into local direction, rotate by NorthYaw+90+actor yaw, "
                "apply SimulationHorizonCompensation and normalize to SunVector"
            ),
            "ast": {
                "dst100": {"SelectFloat": [100.0, 0.0, "Daylight Savings Time"]},
                "timeOffset": {
                    "add": [
                        {"subtract": ["Time of Day", "dst100"]},
                        {"multiply": ["Time Zone", -100.0]},
                    ]
                },
                "utcTimeOfDay": {
                    "Percent_FloatFloat": [{"add": ["timeOffset", 2400.0]}, 2400.0]
                },
                "dateCarry": {
                    "condition": {
                        "or": [
                            {"less": ["timeOffset", 0.0]},
                            {"greaterEqual": ["timeOffset", 2400.0]},
                        ]
                    },
                    "dayDelta": {
                        "SelectInt": [1, -1, {"greaterEqual": ["timeOffset", 2400.0]}]
                    },
                    "call": "Offset Date by a Number of Days",
                },
                "dayIndex": {
                    "add": [
                        {"Day Count at the Start of a Month": ["UTC Year", "UTC Month"]},
                        {"subtract": ["UTC Day", 1]},
                    ]
                },
                "utcTimeOfYear": {
                    "add": ["dayIndex", {"divide": ["UTC Time of Day", 2400.0]}]
                },
                "daysInYear": {"Number of Days in a Year": "UTC Year"},
                "seasonalTime": {
                    "MapRangeUnclamped": [
                        {
                            "Percent_FloatFloat": [
                                {
                                    "subtract": [
                                        "UTC Time of Year",
                                        "Calendar.Winter Solstice Offset",
                                    ]
                                },
                                "Days in This Year",
                            ]
                        },
                        0.0,
                        "Days in This Year",
                        0.0,
                        360.0,
                    ]
                },
                "curveTime": {
                    "MapRangeUnclamped": [
                        {"Percent_FloatFloat": ["UTC Time of Year", "Days in This Year"]},
                        0.0,
                        "Days in This Year",
                        1.0,
                        13.0,
                    ]
                },
                "equationOfTimeOffset": {
                    "GetFloatValue": [EQUATION_OF_TIME_ASSET, "curveTime"]
                },
                "localGeocoordinate": {
                    "ComposeRotators": [
                        "serialized rotator 0.000000,0.000000,90.000000",
                        {
                            "MakeRotator": {
                                "roll": {"multiply": ["Latitude", -1.0]},
                                "pitch": 0.0,
                                "yaw": {"multiply": ["Longitude", -1.0]},
                            }
                        },
                    ]
                },
                "solarYaw": {
                    "Percent_FloatFloat": [
                        {
                            "add": [
                                {
                                    "multiply": [
                                        {"MapRangeClamped": ["UTC Time of Day", 0, 2400, 0, 360]},
                                        -1.0,
                                    ]
                                },
                                {"multiply": ["Seasonal Time", -1.0]},
                                {
                                    "multiply": [
                                        {"divide": ["Equation of Time Offset", 8.0]},
                                        -1.0,
                                    ]
                                },
                            ]
                        },
                        360.0,
                    ]
                },
                "zenith": {
                    "ComposeRotators": [
                        {
                            "ComposeRotators": [
                                "Local Geocoordinate",
                                {"MakeRotator": {"yaw": "solarYaw"}},
                            ]
                        },
                        {"MakeRotator": {"roll": 23.439199}},
                    ]
                },
                "earthCenter": {
                    "multiply": [
                        {
                            "GreaterGreater_VectorRotator": [
                                [0.0, 1.0, 0.0],
                                {"MakeRotator": {"yaw": {"multiply": ["Seasonal Time", -1.0]}}},
                            ]
                        },
                        23244.0,
                    ]
                },
                "viewerPosition": {
                    "add": [{"Conv_RotatorToVector": "Zenith Direction"}, "Earth Center"]
                },
                "localDirection": {
                    "Normal": [
                        {"LessLess_VectorRotator": ["Viewer Position on Earth", "Zenith Direction"]},
                        0.0001,
                    ]
                },
                "northYaw": {"add": ["North Yaw", 90.0, "Actor Rotation.yaw"]},
                "oriented": {
                    "GreaterGreater_VectorRotator": [
                        "localDirection",
                        {"MakeRotator": {"yaw": "northYaw"}},
                    ]
                },
                "sunVector": {
                    "Normal": [
                        {"Simulation Horizon Compensation": "oriented"},
                        0.0001,
                    ]
                },
            },
            "verifiedLocalDefaults": {
                "UTC Month": 1,
                "UTC Day": 1,
                "UTC Year": 2000,
                "Axial Tilt": 23.439199,
                "Sun Direction": [0.0, 0.0, 0.0],
                "Equation of Time Curve": EQUATION_OF_TIME_ASSET,
            },
            "excludedFromSunDependencyPath": ["Days Since J2000"],
            "unresolved": [
                "runtime UtcNow, Actor Rotation and mutable instance values",
                "Moon and stars output branches",
            ],
            "sourceNodes": [
                "UtcNow",
                "BreakDateTime",
                "H/M/S/MS to Time of Day",
                "SelectFloat",
                "Percent_FloatFloat",
                "Offset Date by a Number of Days",
                "Day Count at the Start of a Month",
                "Number of Days in a Year",
                "MapRangeUnclamped",
                "GetFloatValue",
                "ComposeRotators",
                "MakeRotator",
                "Conv_RotatorToVector",
                "GreaterGreater_VectorRotator",
                "LessLess_VectorRotator",
                "Simulation Horizon Compensation",
                "Normal",
            ],
        },
    ]


def internal_function_closure(
    functions: dict[str, dict[str, Any]], roots: tuple[str, ...]
) -> list[str]:
    result: set[str] = set()
    queue = deque(roots)
    while queue:
        name = queue.popleft()
        if name in result:
            continue
        function = functions.get(name)
        if function is None:
            raise KeyError(f"missing decoded UDS function: {name}")
        result.add(name)
        queue.extend(call for call in function.get("calls", []) if call in functions)
    return sorted(result)


def material_closure(
    material_functions: dict[str, dict[str, Any]], roots: tuple[str, ...]
) -> list[str]:
    result: set[str] = set()
    queue = deque(roots)
    while queue:
        asset = queue.popleft()
        if asset in result:
            continue
        function = material_functions.get(asset)
        if function is None:
            raise KeyError(f"missing decoded UDS Material Function: {asset}")
        result.add(asset)
        for call in function.get("calls", []):
            if not isinstance(call, str) or not call.startswith("/Game/UltraDynamicSky/"):
                continue
            called_asset = call.rsplit(".", 1)[0] if "." in call else call
            if called_asset in material_functions:
                queue.append(called_asset)
    return sorted(result)


def material_contract_path(dataset: Path, source_asset: str) -> Path:
    relative = source_asset.removeprefix("/Game/UltraDynamicSky/").replace("/", "__")
    return dataset / "contracts" / f"{relative}.material.json"


def decompile_material_slice(dataset: Path, material_assets: list[str]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    programs = []
    slice_assets = set(material_assets)
    for asset in material_assets:
        contract = read_json(material_contract_path(dataset, asset))
        programs.append(build_expression_program(asset, contract))
    resolve_inter_function_defaults(programs)
    ledger = []
    for program in programs:
        asset = program["sourceAsset"]
        missing_calls = sorted(set(program["calledFunctions"]) - slice_assets)
        behavior, visual_effect = MATERIAL_SEMANTICS[asset]
        status = "VERIFIED" if program["status"] == "VERIFIED" and not missing_calls else "PARTIAL"
        ledger.append(
            {
                "sourceAsset": asset,
                "status": status,
                "sourceDecompilationStatus": program["status"],
                "implementationStatus": "NOT_IMPLEMENTED",
                "prohibitedRuntimeUse": True,
                "programSha256": program["programSha256"],
                "outputCount": len(program["outputs"]),
                "calledFunctions": program["calledFunctions"],
                "missingCalledFunctions": missing_calls,
                "udsBehavior": behavior,
                "visualEffect": visual_effect,
                "semanticLevel": program["semanticLevel"],
                "source": f"{asset}: verified serialized UMaterialExpression graph",
            }
        )
    return programs, ledger


def evaluate_unweighted_rich_curve_channel(channel: dict[str, Any], time: float) -> float:
    keys = sorted(channel["keys"], key=lambda item: item["time"])
    if not keys:
        default = channel.get("default_value")
        if default is None:
            raise ValueError("curve channel has no keys or default")
        return float(default)
    if time < keys[0]["time"] or len(keys) == 1:
        return float(keys[0]["value"])
    if time >= keys[-1]["time"]:
        return float(keys[-1]["value"])
    upper = next(index for index, key in enumerate(keys) if key["time"] > time)
    left = keys[upper - 1]
    right = keys[upper]
    delta = float(right["time"] - left["time"])
    if delta <= 0.0:
        return float(left["value"])
    alpha = float((time - left["time"]) / delta)
    mode = left["interp_mode"]["name"]
    if mode == "constant":
        return float(left["value"])
    if mode == "linear":
        return float(left["value"] + (right["value"] - left["value"]) * alpha)
    if mode != "cubic":
        raise ValueError(f"unsupported RichCurve interpolation: {mode}")
    if left["tangent_weight_mode"]["name"] != "none" or right["tangent_weight_mode"]["name"] != "none":
        raise ValueError("weighted RichCurve segment requires UE weighted evaluator")
    one_third = 1.0 / 3.0
    p0 = float(left["value"])
    p1 = p0 + float(left["leave_tangent"]) * delta * one_third
    p3 = float(right["value"])
    p2 = p3 - float(right["arrive_tangent"]) * delta * one_third
    one_minus = 1.0 - alpha
    return float(
        one_minus * one_minus * one_minus * p0
        + 3.0 * one_minus * one_minus * alpha * p1
        + 3.0 * one_minus * alpha * alpha * p2
        + alpha * alpha * alpha * p3
    )


def build_curve_fixture(curve: dict[str, Any]) -> dict[str, Any]:
    channels = curve["channels"]
    sample_times = set()
    for channel in channels:
        keys = sorted(channel["keys"], key=lambda item: item["time"])
        sample_times.update(float(key["time"]) for key in keys)
        sample_times.update(
            float((left["time"] + right["time"]) * 0.5)
            for left, right in zip(keys, keys[1:])
        )
    samples = []
    for time in sorted(sample_times):
        samples.append(
            {
                "input": time,
                "outputs": {
                    channel["name"]: evaluate_unweighted_rich_curve_channel(channel, time)
                    for channel in channels
                },
            }
        )
    fixture = {
        "sourceAsset": curve["sourceAsset"],
        "status": "VERIFIED",
        "evaluator": "UE_FRICH_CURVE_UNWEIGHTED_CONSTANT_LINEAR_CUBIC_BEZIER",
        "evaluatorSource": RICH_CURVE_SOURCE,
        "prePostInfinityPolicy": "serialized null/default maps to constant endpoint for these assets",
        "samples": samples,
    }
    fixture["fixtureSha256"] = sha256_bytes(
        json.dumps(fixture, ensure_ascii=False, sort_keys=True).encode("utf-8")
    )
    return fixture


def formula_ledger() -> list[dict[str, Any]]:
    """Node-by-node Blueprint formulas; VERIFIED here means source decompilation, not runtime."""
    formulas = {
        "Sun Height": {
            "equation": (
                "selected = Cached ? CachedSunVector : ForwardVector(SunWorldRotation); "
                "return selected.z * -0.5 + 0.5"
            ),
            "ast": {
                "selectVector": {
                    "condition": "Cached",
                    "true": "Cached Sun Vector",
                    "false": {"GetForwardVector": "Sun World Rotation"},
                },
                "return": {"add": [{"multiply": ["selected.z", -0.5]}, 0.5]},
            },
            "sourceNodes": ["SelectVector", "GetForwardVector", "BreakVector", "Multiply_DoubleDouble", "Add_DoubleDouble"],
        },
        "Scaled Directional Balance": {
            "equation": "return CurrentSceneLightingBrightnessScale * DirectionalBalance",
            "ast": {
                "return": {
                    "multiply": [
                        "Current Scene Lighting Brightness Scale",
                        "Directional Balance",
                    ]
                }
            },
            "sourceNodes": ["Multiply_DoubleDouble"],
        },
        "Current Sun Radius": {
            "equation": (
                "base = DegreesToRadians(SunScale); if NearHorizonScale == 1 return base; "
                "return base * MapRangeClamped(CachedSunVector.z, 0, -0.5, NearHorizonScale, 1)"
            ),
            "ast": {
                "base": {"DegreesToRadians": "Sun Scale"},
                "return": {
                    "if": {"equal": ["Scale Sun Radius as it Nears Horizon", 1.0]},
                    "then": "base",
                    "else": {
                        "multiply": [
                            "base",
                            {
                                "MapRangeClamped": [
                                    "Cached Sun Vector.z",
                                    0.0,
                                    -0.5,
                                    "Scale Sun Radius as it Nears Horizon",
                                    1.0,
                                ]
                            },
                        ]
                    },
                },
            },
            "sourceNodes": ["DegreesToRadians", "BreakVector", "MapRangeClamped", "Multiply_DoubleDouble", "EqualEqual_DoubleDouble"],
        },
        "Adjust Base Sun Light Intensity": {
            "equation": (
                "cap = min(SafeDivide(5, SunLightIntensity), 1); alpha = "
                "pow(MapRangeClamped(SunVector.z, 0, 0.11, 1, 0), 8); "
                "fade = Lerp(cap, 1, alpha); multiplier = FadeDownHighIntensity ? fade : 1; "
                "Intensity = SunLightIntensity * multiplier"
            ),
            "ast": {
                "cap": {"min": [{"SafeDivide": [5.0, "Sun Light Intensity"]}, 1.0]},
                "alpha": {
                    "pow": [
                        {"MapRangeClamped": ["Sun Vector.z", 0.0, 0.11, 1.0, 0.0]},
                        8.0,
                    ]
                },
                "fade": {"lerp": ["cap", 1.0, "alpha"]},
                "multiplier": {
                    "select": [
                        "Fade Down High Sun Light Intensity Below Horizon",
                        "fade",
                        1.0,
                    ]
                },
                "Intensity": {"multiply": ["Sun Light Intensity", "multiplier"]},
            },
            "sourceNodes": ["SafeDivide", "FMin", "MapRangeClamped", "MultiplyMultiply_FloatFloat", "Lerp", "SelectFloat", "Multiply_DoubleDouble"],
        },
        "Current Sun Disk Intensity": {
            "equation": (
                "coverageA = ApplyFlatCloudiness ? 1 : 1.5; coverageB = ApplyFlatCloudiness ? 1.8 : 2.4; "
                "coverageOutB = ApplyFlatCloudiness ? 0 : (CloudPaintCanSubtractCoverage ? 0.7 : 0); "
                "cloud = pow(MapRangeClamped(LocalCloudCoverage, coverageA, coverageB, 1, coverageOutB), 2); "
                "fog = MapRangeClamped(Fog, 6, 9, 1, 0); return SunDiskIntensity * 43.010753 * "
                "cloud * ScaledDirectionalBalance * SunLightIntensity * fog"
            ),
            "ast": {
                "coverageA": {"select": ["Apply Flat Cloudiness", 1.0, 1.5]},
                "coverageB": {"select": ["Apply Flat Cloudiness", 1.8, 2.4]},
                "coverageOutB": {
                    "select": [
                        "Apply Flat Cloudiness",
                        0.0,
                        {"select": ["Cloud Paint Can Subtract Coverage", 0.7, 0.0]},
                    ]
                },
                "cloud": {
                    "pow": [
                        {
                            "MapRangeClamped": [
                                "Local Cloud Coverage",
                                "coverageA",
                                "coverageB",
                                1.0,
                                "coverageOutB",
                            ]
                        },
                        2.0,
                    ]
                },
                "fog": {"MapRangeClamped": ["Fog", 6.0, 9.0, 1.0, 0.0]},
                "return": {
                    "multiply": [
                        "Sun Disk Intensity",
                        43.010753,
                        "cloud",
                        "Scaled Directional Balance",
                        "Sun Light Intensity",
                        "fog",
                    ]
                },
            },
            "sourceNodes": ["SelectFloat", "MapRangeClamped", "MultiplyMultiply_FloatFloat", "Scaled Directional Balance", "Multiply_DoubleDouble"],
        },
        "Current Sun Light Intensity": {
            "equation": (
                "if UsingSpaceMode return SunLightIntensity * EclipsePercent; "
                "if UsingSkyAtmosphere unscaled = MapRangeClamped(SunZ, 0, 0.15, SunLightIntensity, 0); "
                "else unscaled = MapRangeClamped(SunZ, 0.157, 0.113, 0, SunLightIntensity) * DirectionalIntensityCurve(SunZ); "
                "interior = Lerp(1, SunLightIntensityMultiplierInInteriors, CachedInvertedGlobalOcclusion); "
                "return unscaled * ScaledDirectionalBalance * EclipsePercent * CachedDirectionalLightDimming * "
                "CachedDirectionalInscatteringMultiplier * interior * AdjustBaseMultiplier"
            ),
            "ast": {
                "spaceReturn": {"multiply": ["Sun Light Intensity", "Eclipse Percent"]},
                "skyAtmosphereUnscaled": {
                    "MapRangeClamped": ["Cached Sun Vector.z", 0.0, 0.15, "Sun Light Intensity", 0.0]
                },
                "legacyUnscaled": {
                    "multiply": [
                        {"MapRangeClamped": ["Cached Sun Vector.z", 0.157, 0.113, 0.0, "Sun Light Intensity"]},
                        {"Directional Intensity Curve.GetFloatValue": "Cached Sun Vector.z"},
                    ]
                },
                "interior": {
                    "lerp": [
                        1.0,
                        "Sun Light Intensity Multiplier in Interiors",
                        "Cached Inverted Global Occlusion",
                    ]
                },
                "normalReturn": {
                    "multiply": [
                        "unscaled",
                        "Scaled Directional Balance",
                        "Eclipse Percent",
                        "Cached Directional Light Dimming",
                        "Cached Directional Inscattering Multiplier",
                        "interior",
                        "Adjust Base Sun Light Intensity.Multiplier",
                    ]
                },
            },
            "sourceNodes": ["Adjust Base Sun Light Intensity", "MapRangeClamped", "Scaled Directional Balance", "GetFloatValue", "Lerp", "Multiply_DoubleDouble"],
        },
        "Current Sun Light Color": {
            "equation": (
                "base = (UsingSkyAtmosphere || UsingSpaceMode) ? SunLightColor : "
                "SunLightColor * DirectionalLightScatteringCurve(SunHeight(true)); "
                "if Saturation != 1 return Desaturate(base, 1 - Saturation); "
                "return base * CachedSolarEclipseTint"
            ),
            "ast": {
                "base": {
                    "if": {"or": ["Using Sky Atmosphere", "Using Space Mode"]},
                    "then": "Sun Light Color",
                    "else": {
                        "multiply": [
                            "Sun Light Color",
                            {"Directional Light Scattering Curve.GetClampedLinearColorValue": "Sun Height(true)"},
                        ]
                    },
                },
                "return": {
                    "if": {"notEqual": ["Saturation", 1.0]},
                    "then": {"Desaturate": ["base", {"subtract": [1.0, "Saturation"]}]},
                    "else": {"multiply": ["base", "Cached Solar Eclipse Tint"]},
                },
            },
            "sourceNodes": ["BooleanOR", "Sun Height", "GetClampedLinearColorValue", "Multiply_LinearColorLinearColor", "Subtract_DoubleDouble", "LinearColor_Desaturated", "NotEqual_DoubleDouble"],
        },
        "Current Sun Disk Color": {
            "equation": (
                "if UsingSpaceMode unscaled = LinearColor(SunDiskIntensity * SunLightIntensity * 43); "
                "else unscaled = SunDiskColorCurve(Lerp(0.5, SunHeight(true), EclipsePercent)) * CurrentSunDiskIntensity; "
                "eclipseEdge = clamp(EclipsePercent * 80, 0, 1) * (SunSoftness / 4); "
                "return unscaled * SunLightColor * CachedSolarEclipseTint * eclipseEdge * SunDiskTint"
            ),
            "ast": {
                "spaceUnscaled": {
                    "LinearColor": {"multiply": ["Sun Disk Intensity", "Sun Light Intensity", 43.0]}
                },
                "normalUnscaled": {
                    "multiply": [
                        {"Sun Disk Color Curve.GetClampedLinearColorValue": {"lerp": [0.5, "Sun Height(true)", "Eclipse Percent"]}},
                        "Current Sun Disk Intensity",
                    ]
                },
                "eclipseEdge": {
                    "multiply": [
                        {"clamp": [{"multiply": ["Eclipse Percent", 80.0]}, 0.0, 1.0]},
                        {"divide": ["Sun Softness", 4.0]},
                    ]
                },
                "return": {
                    "multiply": [
                        "unscaled",
                        "Sun Light Color",
                        "Cached Solar Eclipse Tint",
                        "eclipseEdge",
                        "Sun Disk Tint",
                    ]
                },
            },
            "sourceNodes": ["Current Sun Disk Intensity", "Sun Height", "Lerp", "GetClampedLinearColorValue", "Multiply_LinearColorFloat", "FClamp", "Conv_DoubleToLinearColor", "Multiply_LinearColorLinearColor"],
        },
    }
    return [
        {
            "function": name,
            "status": "VERIFIED",
            "implementationStatus": "NOT_IMPLEMENTED",
            "source": f"{BLUEPRINT_ASSET}: {name}",
            "prohibitedRuntimeUse": True,
            **formulas[name],
        }
        for name in EXPECTED_FUNCTION_SLICE
    ]


def build(dataset: Path, system_contract_path: Path, output: Path) -> dict[str, Any]:
    system = read_json(system_contract_path)
    blueprint_path = dataset / BLUEPRINT_CONTRACT
    blueprint = read_json(blueprint_path)
    if blueprint.get("status") != "RAW_VERIFIED":
        raise ValueError("UDS Blueprint contract is not RAW_VERIFIED")
    player_occlusion = read_json(dataset / PLAYER_OCCLUSION_CONTRACT)
    if player_occlusion.get("status") != "RAW_VERIFIED":
        raise ValueError("UDS Player Occlusion Blueprint contract is not RAW_VERIFIED")
    function_rows = {
        item["sourceName"]: item
        for item in system["functions"]
        if item["sourceAsset"] == BLUEPRINT_ASSET
    }
    function_slice = internal_function_closure(function_rows, ROOT_FUNCTIONS)
    if tuple(function_slice) != EXPECTED_FUNCTION_SLICE:
        raise AssertionError(
            f"Sun values dependency slice changed: {function_slice}"
        )
    raw_graphs = {
        graph["graph"].rsplit(".", 1)[-1]: graph
        for graph in blueprint.get("graph", {}).get("graphs", [])
    }
    graphs = [normalized_graph(raw_graphs[name]) for name in function_slice]
    referenced_parameters = sorted(
        set().union(
            *(set(function_rows[name].get("reads", [])) for name in function_slice),
            *(set(function_rows[name].get("modifies", [])) for name in function_slice),
        )
    )
    parameter_rows = {
        item["sourceName"]: item
        for item in system["parameters"]
        if item["sourceAsset"] == BLUEPRINT_ASSET
    }
    parameters = [dict(parameter_rows[name]) for name in referenced_parameters if name in parameter_rows]
    local_graph_symbols = sorted(set(referenced_parameters) - set(parameter_rows))
    parameter_writer_contracts = [
        parameter_writer_contract(
            raw_graphs[formula["graph"]], parameter, formula
        )
        for parameter, formula in DERIVED_SUN_PARAMETER_WRITERS.items()
    ]
    parameter_writer_schedule = build_writer_schedule_contract(
        blueprint, parameter_writer_contracts
    )
    celestial_writer_contracts = [
        parameter_writer_contract(
            raw_graphs[formula["graph"]], parameter, formula
        )
        for parameter, formula in CELESTIAL_SUN_PARAMETER_WRITERS.items()
    ]
    celestial_writer_schedule = build_writer_schedule_contract(
        blueprint, celestial_writer_contracts
    )
    calendar_initialization_schedule = build_writer_schedule_contract(
        blueprint, [CALENDAR_INITIALIZATION_WRITER]
    )
    calendar_startup_order = build_operation_order_contract(
        blueprint,
        "Startup Sky",
        [
            "Update Static Variables",
            "Find Real Sunset/Sunrise Times",
            "Cache Properties",
        ],
        sequence_semantics_source=UE_SEQUENCE_SOURCE,
    )
    calendar_binding_startup_order = build_operation_order_contract(
        blueprint,
        "Startup Sky",
        ["Update Static Variables", "Set Up Internal Bindings"],
        sequence_semantics_source=UE_SEQUENCE_SOURCE,
    )
    construction_startup_order = build_operation_order_contract(
        blueprint,
        "UserConstructionScript",
        ["Apply Location Preset", "Startup Sky"],
        sequence_semantics_source=UE_SEQUENCE_SOURCE,
    )
    calendar_pre_first_use_audit = build_calendar_pre_first_use_audit(raw_graphs)
    sun_scheduling = build_sun_scheduling_contract(blueprint, raw_graphs)
    coordinate_mapping = coordinate_mapping_contract()
    writer_by_parameter = {
        item["parameter"]: item for item in parameter_writer_contracts
    }
    for parameter in parameters:
        writer = writer_by_parameter.get(parameter["sourceName"])
        if writer is None:
            continue
        parameter["type"] = writer["resolvedPropertyType"]
        parameter["derivedRuntimeValue"] = {
            "status": writer["status"],
            "sourceGraph": writer["sourceGraph"],
            "setNodes": writer["setNodes"],
            "formula": writer["formula"],
            "writerContractTopologySha256": writer["topologySha256"],
        }
        parameter["missingEvidence"] = [
            item
            for item in parameter.get("missingEvidence", [])
            if item != "property type"
        ]
        if parameter.get("default") in ("", None) and parameter["type"] in {
            "BoolProperty",
            "DoubleProperty",
        }:
            parameter["default"] = (
                False if parameter["type"] == "BoolProperty" else 0.0
            )
            parameter["defaultEvidence"] = {
                "status": "VERIFIED_ENGINE_ZERO_INIT_PLUS_NO_CDO_OVERRIDE",
                "udsEvidence": (
                    "Blueprint NewVariables default is empty and the decoded UDS CDO contains "
                    "no override for this Blueprint-owned property"
                ),
                "engineSource": UE_OBJECT_ZERO_INIT_SOURCE,
            }
            parameter["missingEvidence"] = [
                item
                for item in parameter["missingEvidence"]
                if item != "typed CDO default"
            ]
        parameter["extractionStatus"] = "PARTIAL"

    celestial_writer_by_parameter = {
        item["parameter"]: item for item in celestial_writer_contracts
    }
    for parameter in parameters:
        writer = celestial_writer_by_parameter.get(parameter["sourceName"])
        if writer is None:
            continue
        parameter["type"] = writer["resolvedPropertyType"]
        parameter["celestialRuntimeValue"] = {
            "status": writer["status"],
            "topologyStatus": writer["topologyStatus"],
            "sourceGraph": writer["sourceGraph"],
            "setNodes": writer["setNodes"],
            "formula": writer["formula"],
            "writerContractTopologySha256": writer["topologySha256"],
        }
        parameter["missingEvidence"] = [
            item
            for item in parameter.get("missingEvidence", [])
            if item != "property type"
        ]
        parameter["extractionStatus"] = "PARTIAL"

    parameter_by_name = {item["sourceName"]: item for item in parameters}
    cached_sun_vector = parameter_by_name["Cached Sun Vector"]
    cached_sun_vector["units"] = {
        "value": "DIRECTION_VECTOR_DIMENSIONLESS",
        "evidence": "UDS writer and Sun Height graph plus UE vector semantics",
        "status": "PARTIAL",
        "note": "branch-by-branch normalization is part of the remaining trajectory closure",
    }
    cached_sun_vector["coordinateSpace"] = {
        "value": "UNREAL_ENGINE_WORLD_DIRECTIONAL_LIGHT_RAY_DIRECTION",
        "evidence": (
            "UDS Cache Sun and Moon Orientation / Sun Height graphs; UE world-vector semantics; "
            "see coordinateMapping"
        ),
        "status": "VERIFIED_ENGINE_SEMANTICS",
        "targetConversion": "filament = (ue.y, ue.z, -ue.x)",
    }
    cached_sun_vector["missingEvidence"] = [
        item
        for item in cached_sun_vector.get("missingEvidence", [])
        if item not in {"explicit units", "explicit coordinate space"}
    ]
    cached_sun_vector["missingEvidence"].append(
        "branch-by-branch unit-length guarantee and exact trajectory inputs"
    )

    sun_world_rotation = parameter_by_name["Sun World Rotation"]
    sun_world_rotation["units"] = {
        "value": "Degrees",
        "evidence": "UE FRotator engine semantics",
        "status": "VERIFIED_ENGINE_SEMANTICS",
    }
    sun_world_rotation["coordinateSpace"] = {
        "value": "UNREAL_ENGINE_WORLD_ROTATION_OF_SUNLIGHT",
        "evidence": (
            "UDS property tooltip and AP - Sun Root Vector writer; UE FRotator semantics; "
            "see coordinateMapping.rotationRule"
        ),
        "status": "VERIFIED_ENGINE_SEMANTICS",
        "targetConversion": "convert UE basis vectors; never permute Euler components directly",
    }
    sun_world_rotation["missingEvidence"] = [
        item
        for item in sun_world_rotation.get("missingEvidence", [])
        if item not in {"explicit units", "explicit coordinate space"}
    ]
    sun_world_rotation["missingEvidence"].append(
        "complete AP - Sun Root Vector value/control-flow closure"
    )

    parameter_writer_support_names = internal_function_closure(
        function_rows, PARAMETER_WRITER_SUPPORT_ROOTS
    )
    parameter_writer_support_graphs = [
        normalized_graph(raw_graphs[name])
        for name in parameter_writer_support_names
    ]
    parameter_writer_support_ledger = parameter_writer_support_formula_ledger()
    player_occlusion_graphs = {
        graph["graph"].rsplit(".", 1)[-1]: graph
        for graph in player_occlusion.get("graph", {}).get("graphs", [])
    }
    external_support_graphs = [
        normalized_graph(
            player_occlusion_graphs["Current Global Occlusion"],
            PLAYER_OCCLUSION_ASSET,
        )
    ]
    external_support_ledger = external_support_formula_ledger()
    orientation_function_names = internal_function_closure(
        function_rows, ORIENTATION_ROOT_FUNCTIONS
    )
    if tuple(orientation_function_names) != EXPECTED_ORIENTATION_FUNCTION_SLICE:
        raise AssertionError(
            f"Sun orientation dependency slice changed: {orientation_function_names}"
        )
    orientation_graphs = [
        normalized_graph(raw_graphs[name]) for name in orientation_function_names
    ]
    scheduling_graphs = [
        normalized_graph(raw_graphs[name]) for name in SUN_SCHEDULING_FUNCTION_SLICE
    ]
    orientation_formula_ledger = celestial_orientation_formula_ledger()
    astronomy_assets = astronomy_source_assets(dataset)

    material_functions = {
        item["sourceAsset"]: item
        for item in system["functions"]
        if item.get("functionKind") == "MATERIAL_GRAPH"
    }
    material_assets = material_closure(material_functions, SUN_MATERIAL_ROOTS)
    material_programs, material_formula_ledger = decompile_material_slice(
        dataset, material_assets
    )
    curves_by_asset = {item["sourceAsset"]: item for item in system["curves"]}
    curves = [curves_by_asset[asset] for asset in SUN_CURVES]
    curve_fixtures = [build_curve_fixture(curve) for curve in curves]
    ledger = formula_ledger()
    contract = {
        "schema": "solum.p63.10.uds-sun-values-contract/v1",
        "status": "PARTIAL",
        "completionEligible": False,
        "completionBlockers": [
            "runtime values/defaults have not been changed",
            "Calendar local D0 and four timer initial/reset values remain PARTIAL",
            "engine tick/delegate lifecycle and external editor/UDW cadence remain PARTIAL",
            "independent read-only re-review is required after coordinate, orientation and occlusion closure",
            "user visual QA is pending",
        ],
        "source": {
            "blueprintAsset": BLUEPRINT_ASSET,
            "blueprintStatus": blueprint.get("status"),
            "blueprintSha256": blueprint.get("source", {}).get("sha256"),
            "playerOcclusionBlueprintAsset": PLAYER_OCCLUSION_ASSET,
            "playerOcclusionStatus": player_occlusion.get("status"),
            "playerOcclusionSha256": player_occlusion.get("source", {}).get("sha256"),
            "systemContractSchema": system.get("schema"),
        },
        "rootFunctions": list(ROOT_FUNCTIONS),
        "functionSlice": function_slice,
        "graphs": graphs,
        "parameters": parameters,
        "parameterWriterContracts": parameter_writer_contracts,
        "parameterWriterSchedule": parameter_writer_schedule,
        "celestialWriterContracts": celestial_writer_contracts,
        "celestialWriterSchedule": celestial_writer_schedule,
        "calendarInitializationSchedule": calendar_initialization_schedule,
        "calendarStartupOrder": calendar_startup_order,
        "calendarBindingStartupOrder": calendar_binding_startup_order,
        "constructionStartupOrder": construction_startup_order,
        "calendarPreFirstUseAudit": calendar_pre_first_use_audit,
        "sunScheduling": sun_scheduling,
        "parameterWriterSupportGraphs": parameter_writer_support_graphs,
        "parameterWriterSupportFormulaLedger": parameter_writer_support_ledger,
        "externalSupportGraphs": external_support_graphs,
        "externalSupportFormulaLedger": external_support_ledger,
        "orientationFunctionSlice": orientation_function_names,
        "orientationGraphs": orientation_graphs,
        "schedulingFunctionSlice": list(SUN_SCHEDULING_FUNCTION_SLICE),
        "schedulingGraphs": scheduling_graphs,
        "orientationFormulaLedger": orientation_formula_ledger,
        "astronomySourceAssets": astronomy_assets,
        "localGraphSymbols": local_graph_symbols,
        "curves": curves,
        "curveFixtures": curve_fixtures,
        "materialFunctionSlice": [material_functions[asset] for asset in material_assets],
        "materialExpressionPrograms": material_programs,
        "materialFormulaLedger": material_formula_ledger,
        "formulaLedger": ledger,
        "coordinateMapping": coordinate_mapping,
        "semanticMapping": {
            "udsBehavior": "source-backed current Sun radius/light/disk color and intensity evaluation",
            "requiredVisualResult": "UDS angular size, light/disk curves, horizon scaling and directional coupling without guessed constants",
            "proposedSolumFilamentImplementation": (
                "blocked until remaining writer initialization/lifecycle evidence passes independent "
                "review"
            ),
            "currentDifferences": "current runtime defaults/adapters and authored formulas are not UDS authority",
            "estimatedGpuCost": "low per sky fragment after CPU-side values; exact split still under review",
            "estimatedCpuCost": "low per adaptive UDS cache/update-group step; engine callback cadence remains PARTIAL",
            "riskOfBehaviorLoss": "critical",
            "verification": "node equations, curve evaluator fixtures, synchronized material/light runtime dump and device captures",
            "prohibitedSimplifications": [
                "visually tune Sun values before formula closure",
                "treat angular diameter as radius",
                "replace authored curves with a generic sunset gradient",
                "claim graph presence as runtime implementation",
            ],
            "status": "NOT_IMPLEMENTED",
        },
    }
    write_json(output / "P63_10_UDS_SUN_VALUES_CONTRACT.json", contract)
    checkpoint = f"""# P63.10 Checkpoint 01 — Sun contract and runtime values

Status: **PARTIAL**. Runtime edits are blocked.

- Blueprint function slice: {len(function_slice)} exact graphs, {sum(item['nodeCount'] for item in graphs)} nodes, {sum(item['edgeCount'] for item in graphs)} edges.
- Referenced Blueprint parameters: {len(parameters)}; local input/output symbols: {len(local_graph_symbols)}.
- Derived Sun parameter writers: {len(parameter_writer_contracts)} exact K2 slices; support functions:
  {len(parameter_writer_support_graphs)} exact graphs.
- Celestial orientation/eclipse writers: {len(celestial_writer_contracts)} exact K2 slices across
  {len(celestial_writer_schedule['controlFlow'])} control-flow graphs.
- Orientation dependency slice: {len(orientation_function_names)} exact graphs; formula ledger:
  {sum(item['status'] == 'VERIFIED' for item in orientation_formula_ledger)} `VERIFIED`,
  {sum(item['status'] == 'PARTIAL' for item in orientation_formula_ledger)} `PARTIAL`,
  {sum(item['status'] == 'NOT_DECOMPILED' for item in orientation_formula_ledger)} `NOT_DECOMPILED`.
- Real-Sun path: exact UTC/date/seasonal/equation-of-time/geographic graph and seven local
  functions published. The authored Equation-of-Time curve has
  {astronomy_assets['equationOfTime']['keyCount']} exact keys. Calendar scalar/map inputs,
  operations and write order are source-backed; the candidate numeric prefix arrays remain
  `PARTIAL` until UE Blueprint VM initialization of the empty local `Day Count` default is proven.
- Calendar initialization control-flow graphs:
  {len(calendar_initialization_schedule['controlFlow'])}; direct activation through
  `Update Static Variables` is published, while concrete first-run/cadence remains `PARTIAL`.
- Writer control-flow graphs: {len(parameter_writer_schedule['controlFlow'])}; runtime schedule remains
  `PARTIAL` until first-update, timer and delegate behavior is closed.
- Scheduling source graphs preserved losslessly at graph/topology level:
  {len(scheduling_graphs)}; semantic cadence ledger remains in progress.
- UE world direction -> SOLUM/Filament world direction: exact orthonormal handedness conversion
  published; Sun body/light-vector sign is source-backed, runtime remains `NOT_IMPLEMENTED`.
- Authored curves: {len(curves)}.
- Material Function dependency slice: {len(material_assets)}.
- Blueprint formula ledger: {sum(item['status'] == 'VERIFIED' for item in ledger)} `VERIFIED`,
  {sum(item['status'] == 'NOT_DECOMPILED' for item in ledger)} `NOT_DECOMPILED`.
- Material formula ledger: {sum(item['status'] == 'VERIFIED' for item in material_formula_ledger)} `VERIFIED`,
  {sum(item['status'] != 'VERIFIED' for item in material_formula_ledger)} `PARTIAL`.
- Curve evaluator fixtures: {len(curve_fixtures)} exact UE unweighted RichCurve fixtures.

No render behavior was changed. The previously missing UDS_PlayerOcclusion dependency and authored
Equation-of-Time curve are now exact and source-backed. Calendar startup ordering is exact; runtime
work remains blocked on exact first-update/cadence evidence and a fresh independent read-only review.
UE rotation/vector operators and the serialized 90-degree yaw literal now have official engine
semantics in `coordinateMapping`. See CHECKPOINT_01_REVIEW.md.
"""
    (output / "CHECKPOINT_01_SUN_VALUES.md").write_text(checkpoint, encoding="utf-8")
    return {
        "functions": len(function_slice),
        "nodes": sum(item["nodeCount"] for item in graphs),
        "edges": sum(item["edgeCount"] for item in graphs),
        "parameters": len(parameters),
        "parameterWriters": len(parameter_writer_contracts),
        "celestialWriters": len(celestial_writer_contracts),
        "parameterWriterSupportFunctions": len(parameter_writer_support_graphs),
        "parameterWriterControlFlowGraphs": len(parameter_writer_schedule["controlFlow"]),
        "celestialWriterControlFlowGraphs": len(celestial_writer_schedule["controlFlow"]),
        "calendarInitializationControlFlowGraphs": len(
            calendar_initialization_schedule["controlFlow"]
        ),
        "orientationFunctions": len(orientation_function_names),
        "schedulingFunctions": len(scheduling_graphs),
        "orientationFormulaStatuses": {
            status: sum(item["status"] == status for item in orientation_formula_ledger)
            for status in sorted({item["status"] for item in orientation_formula_ledger})
        },
        "localGraphSymbols": local_graph_symbols,
        "curves": len(curves),
        "curveFixtures": len(curve_fixtures),
        "materialFunctions": len(material_assets),
        "materialFormulaStatuses": {
            status: sum(item["status"] == status for item in material_formula_ledger)
            for status in sorted({item["status"] for item in material_formula_ledger})
        },
        "formulaStatuses": {
            status: sum(item["status"] == status for item in ledger)
            for status in sorted({item["status"] for item in ledger})
        },
    }


def self_test(output: Path, summary: dict[str, Any]) -> None:
    if summary["functions"] != len(EXPECTED_FUNCTION_SLICE):
        raise AssertionError("Sun function dependency slice is incomplete")
    if tuple(summary["localGraphSymbols"]) != EXPECTED_LOCAL_GRAPH_SYMBOLS:
        raise AssertionError(
            f"unexpected Sun local graph symbols: {summary['localGraphSymbols']}"
        )
    if summary["parameterWriters"] != len(DERIVED_SUN_PARAMETER_WRITERS):
        raise AssertionError("derived Sun parameter writer slice is incomplete")
    if summary["celestialWriters"] != len(CELESTIAL_SUN_PARAMETER_WRITERS):
        raise AssertionError("celestial Sun parameter writer slice is incomplete")
    if summary["calendarInitializationControlFlowGraphs"] != 1:
        raise AssertionError("Calendar initialization control-flow slice is incomplete")
    if summary["orientationFunctions"] != len(EXPECTED_ORIENTATION_FUNCTION_SLICE):
        raise AssertionError("Sun orientation dependency slice is incomplete")
    if summary["schedulingFunctions"] != len(SUN_SCHEDULING_FUNCTION_SLICE):
        raise AssertionError("Sun scheduling dependency slice is incomplete")
    expected_writer_graphs = {
        item["graph"] for item in DERIVED_SUN_PARAMETER_WRITERS.values()
    }
    if summary["parameterWriterControlFlowGraphs"] != len(expected_writer_graphs):
        raise AssertionError("derived Sun writer control-flow slice is incomplete")
    if summary["curves"] != len(SUN_CURVES):
        raise AssertionError("Sun curve slice is incomplete")
    if summary["curveFixtures"] != len(SUN_CURVES):
        raise AssertionError("Sun curve fixture slice is incomplete")
    payload = read_json(output / "P63_10_UDS_SUN_VALUES_CONTRACT.json")
    if payload.get("status") != "PARTIAL" or payload.get("completionEligible") is not False:
        raise AssertionError("Sun checkpoint must remain explicitly incomplete")
    rendered = json.dumps(payload, ensure_ascii=False)
    forbidden = ("/storage/emulated/", "/data/data/com.termux/", "udsRoot")
    leaks = [token for token in forbidden if token in rendered]
    if leaks:
        raise AssertionError(f"private physical path leak: {leaks}")
    if tuple(payload["schedulingFunctionSlice"]) != SUN_SCHEDULING_FUNCTION_SLICE:
        raise AssertionError("scheduling function slice changed")
    scheduling_graphs = {
        item["sourceName"]: item for item in payload["schedulingGraphs"]
    }
    if set(scheduling_graphs) != set(SUN_SCHEDULING_FUNCTION_SLICE):
        raise AssertionError("scheduling graph coverage is incomplete")
    if any(
        item["nodeCount"] <= 0 or item["topologySha256"] == ""
        for item in scheduling_graphs.values()
    ):
        raise AssertionError("scheduling graph topology is empty")
    coordinate_mapping = payload["coordinateMapping"]
    if coordinate_mapping["status"] != "VERIFIED_ENGINE_COORDINATE_CONVERSION":
        raise AssertionError("UE-to-Filament coordinate conversion lost verified status")
    if coordinate_mapping["implementationStatus"] != "NOT_IMPLEMENTED":
        raise AssertionError("coordinate contract must not claim runtime implementation")
    matrix = coordinate_mapping["vectorTransform"]["rowMajorMatrix"]
    determinant = (
        matrix[0][0] * (matrix[1][1] * matrix[2][2] - matrix[1][2] * matrix[2][1])
        - matrix[0][1] * (matrix[1][0] * matrix[2][2] - matrix[1][2] * matrix[2][0])
        + matrix[0][2] * (matrix[1][0] * matrix[2][1] - matrix[1][1] * matrix[2][0])
    )
    if abs(determinant + 1.0) > 1.0e-9:
        raise AssertionError("coordinate mapping does not change handedness exactly once")

    def transform(vector: list[float]) -> list[float]:
        return [
            sum(row[column] * vector[column] for column in range(3))
            for row in matrix
        ]

    axis_fixtures = coordinate_mapping["vectorTransform"]["axisFixtures"]
    expected_axis_mappings = {
        "ueForward(+X)": ([1.0, 0.0, 0.0], [0.0, 0.0, -1.0]),
        "ueRight(+Y)": ([0.0, 1.0, 0.0], [1.0, 0.0, 0.0]),
        "ueUp(+Z)": ([0.0, 0.0, 1.0], [0.0, 1.0, 0.0]),
    }
    for name, (source_axis, target_axis) in expected_axis_mappings.items():
        if transform(source_axis) != target_axis or axis_fixtures[name] != target_axis:
            raise AssertionError(f"coordinate axis fixture mismatch: {name}")
    rotation_semantics = coordinate_mapping["rotationRule"]["engineSemantics"]
    if (
        rotation_semantics["rotator"]["intrinsicOrder"] != ["Yaw", "Pitch", "Roll"]
        or rotation_semantics["serializedTuple"]["mapping"]
        != {"X": "Roll", "Y": "Pitch", "Z": "Yaw"}
        or rotation_semantics["serializedTuple"]["decodedLiteral"]["yaw"] != 90.0
        or rotation_semantics["ComposeRotators(A,B)"]["order"]
        != "apply A first, then B"
        or rotation_semantics["GreaterGreater_VectorRotator"]["meaning"]
        != "Rotator.RotateVector(Vector)"
        or rotation_semantics["LessLess_VectorRotator"]["meaning"]
        != "Rotator.UnrotateVector(Vector)"
        or rotation_semantics["coordinateConjugation"]["equation"]
        != "R_filament = M * R_ue * transpose(M)"
    ):
        raise AssertionError("UE rotation/operator semantics changed")
    sun_direction_semantics = coordinate_mapping["directionSemantics"]
    if (
        sun_direction_semantics["visualSunBodyDirection"]["equation"]
        != "observerToSun = -filamentDirectionalLightDirection"
    ):
        raise AssertionError("Sun body/light sign relationship changed")
    if any(item["status"] != "VERIFIED" for item in payload["formulaLedger"]):
        raise AssertionError("all base Blueprint formulas must be source-decompiled")
    if any(item["implementationStatus"] != "NOT_IMPLEMENTED" for item in payload["formulaLedger"]):
        raise AssertionError("source decompilation must not claim runtime implementation")
    writer_contracts = payload["parameterWriterContracts"]
    if {item["parameter"] for item in writer_contracts} != set(DERIVED_SUN_PARAMETER_WRITERS):
        raise AssertionError("derived Sun parameter writer contracts changed")
    if any(item["topologyStatus"] != "VERIFIED" for item in writer_contracts):
        raise AssertionError("derived Sun parameter writer topology is unresolved")
    celestial_writer_contracts = payload["celestialWriterContracts"]
    if {item["parameter"] for item in celestial_writer_contracts} != set(
        CELESTIAL_SUN_PARAMETER_WRITERS
    ):
        raise AssertionError("celestial Sun writer contracts changed")
    if any(item["topologyStatus"] != "VERIFIED" for item in celestial_writer_contracts):
        raise AssertionError("celestial Sun writer topology is unresolved")
    expected_celestial_formula_status = {
        name: formula.get("status", "VERIFIED")
        for name, formula in CELESTIAL_SUN_PARAMETER_WRITERS.items()
    }
    if any(
        item["formulaStatus"] != expected_celestial_formula_status[item["parameter"]]
        for item in celestial_writer_contracts
    ):
        raise AssertionError("celestial Sun writer formula status changed")
    parameter_by_name = {item["sourceName"]: item for item in payload["parameters"]}
    if (
        parameter_by_name["Cached Sun Vector"]["coordinateSpace"]["status"]
        != "VERIFIED_ENGINE_SEMANTICS"
        or parameter_by_name["Sun World Rotation"]["coordinateSpace"]["status"]
        != "VERIFIED_ENGINE_SEMANTICS"
    ):
        raise AssertionError("Sun world-space parameter semantics are unresolved")
    if any(
        parameter_by_name[name]["extractionStatus"] != "PARTIAL"
        for name in ("Cached Sun Vector", "Sun World Rotation")
    ):
        raise AssertionError("coordinate proof must not overclaim complete trajectory extraction")
    for name in DERIVED_SUN_PARAMETER_WRITERS:
        parameter = parameter_by_name[name]
        if parameter["type"] == "UNKNOWN":
            raise AssertionError(f"derived Sun parameter type is unresolved: {name}")
        if parameter["derivedRuntimeValue"]["status"] not in {"VERIFIED", "PARTIAL"}:
            raise AssertionError(f"derived Sun parameter formula is unresolved: {name}")
        if parameter["extractionStatus"] == "UNKNOWN":
            raise AssertionError(f"derived Sun parameter remained UNKNOWN: {name}")
        if parameter["type"] in {"BoolProperty", "DoubleProperty"}:
            if (
                parameter.get("default") in ("", None)
                or parameter.get("defaultEvidence", {}).get("status")
                != "VERIFIED_ENGINE_ZERO_INIT_PLUS_NO_CDO_OVERRIDE"
                or not parameter["defaultEvidence"]["engineSource"].startswith(
                    "https://dev.epicgames.com/"
                )
                or "typed CDO default" in parameter.get("missingEvidence", [])
            ):
                raise AssertionError(
                    f"derived UObject property zero default is not source-backed: {name}"
                )
    support_graphs = {
        item["sourceName"]: item
        for item in payload["parameterWriterSupportGraphs"]
    }
    for formula in payload["parameterWriterSupportFormulaLedger"]:
        graph = support_graphs[formula["function"]]
        decoded_operations = {
            node.get("operation") or node.get("variable") or node.get("class")
            for node in graph["nodes"]
        }
        missing_operations = sorted(set(formula["sourceNodes"]) - decoded_operations)
        if missing_operations:
            raise AssertionError(
                f"writer support formula {formula['function']} cites absent nodes: {missing_operations}"
            )
    partial_support = [
        item["function"]
        for item in payload["parameterWriterSupportFormulaLedger"]
        if item["status"] != "VERIFIED"
    ]
    if partial_support:
        raise AssertionError(f"unexpected writer support blockers: {partial_support}")
    external_graphs = {
        item["sourceName"]: item for item in payload["externalSupportGraphs"]
    }
    for formula in payload["externalSupportFormulaLedger"]:
        graph = external_graphs[formula["function"]]
        decoded_operations = {
            node.get("operation") or node.get("variable") or node.get("class")
            for node in graph["nodes"]
        }
        missing_operations = sorted(set(formula["sourceNodes"]) - decoded_operations)
        if missing_operations:
            raise AssertionError(
                f"external support formula {formula['function']} cites absent nodes: "
                f"{missing_operations}"
            )
        if formula["status"] != "VERIFIED":
            raise AssertionError(
                f"external support formula remained partial: {formula['function']}"
            )
    schedule = payload["parameterWriterSchedule"]
    if schedule["status"] != "PARTIAL":
        raise AssertionError("writer schedule must not overclaim runtime frequency")
    if any(item["status"] != "VERIFIED" for item in schedule["controlFlow"]):
        raise AssertionError("writer exec topology is unresolved")
    if any(
        path["shortestExecPathFromEntry"] is None
        for graph in schedule["controlFlow"]
        for path in graph["writerPaths"]
    ):
        raise AssertionError("writer has no source-backed exec path from FunctionEntry")
    schedule_by_graph = {item["writerGraph"]: item for item in schedule["schedules"]}
    if set(schedule_by_graph) != expected_writer_graphs:
        raise AssertionError("writer schedule graph set changed")
    if any(not item["callSites"] for item in schedule_by_graph.values()):
        raise AssertionError("writer graph has no decoded call site")
    celestial_schedule = payload["celestialWriterSchedule"]
    expected_celestial_graphs = {
        item["graph"] for item in CELESTIAL_SUN_PARAMETER_WRITERS.values()
    }
    if celestial_schedule["status"] != "PARTIAL":
        raise AssertionError("celestial writer schedule must not overclaim runtime frequency")
    if summary["celestialWriterControlFlowGraphs"] != len(expected_celestial_graphs):
        raise AssertionError("celestial writer control-flow slice is incomplete")
    if any(item["status"] != "VERIFIED" for item in celestial_schedule["controlFlow"]):
        raise AssertionError("celestial writer exec topology is unresolved")
    if any(
        path["shortestExecPathFromEntry"] is None
        for graph in celestial_schedule["controlFlow"]
        for path in graph["writerPaths"]
    ):
        raise AssertionError("celestial writer has no source-backed exec path from entry")
    celestial_schedule_by_graph = {
        item["writerGraph"]: item for item in celestial_schedule["schedules"]
    }
    if set(celestial_schedule_by_graph) != expected_celestial_graphs:
        raise AssertionError("celestial writer schedule graph set changed")
    if any(
        not item["callSites"] and not item["delegateBindings"]
        for item in celestial_schedule_by_graph.values()
    ):
        raise AssertionError("celestial writer graph has no decoded call/delegate activation")
    if any(
        item["knownTopology"] != "VERIFIED"
        for item in celestial_schedule_by_graph.values()
    ):
        raise AssertionError("celestial writer activation topology is unresolved")
    ap_bindings = celestial_schedule_by_graph["AP - Sun Root Vector"]["delegateBindings"]
    if len(ap_bindings) != 1 or not (
        ap_bindings[0]["bindingGraph"] == "Cache Sun and Moon Orientation"
        and ap_bindings[0]["consumerOperation"] == "Cache Color"
        and ap_bindings[0]["consumerPin"] == "Event"
        and ap_bindings[0]["execPathFromBindingGraphEntryToConsumer"] is not None
    ):
        raise AssertionError("AP - Sun Root Vector delegate binding changed")
    calendar_schedule = payload["calendarInitializationSchedule"]
    if (
        calendar_schedule["status"] != "PARTIAL"
        or len(calendar_schedule["controlFlow"]) != 1
        or calendar_schedule["controlFlow"][0]["status"] != "VERIFIED"
        or calendar_schedule["schedules"][0]["knownTopology"] != "VERIFIED"
        or calendar_schedule["schedules"][0]["directCallers"] != ["Update Static Variables"]
        or not calendar_schedule["schedules"][0]["callSites"]
        or calendar_schedule["schedules"][0]["callSites"][0]["execPathFromCallerEntry"] is None
    ):
        raise AssertionError("Calendar initialization activation topology changed")
    calendar_startup_order = payload["calendarStartupOrder"]
    sequence_order = calendar_startup_order["orderPaths"][0]["sequenceOrder"]
    if (
        calendar_startup_order["status"] != "VERIFIED"
        or len(calendar_startup_order["orderPaths"]) != 2
        or any(
            item["status"] != "VERIFIED"
            for item in calendar_startup_order["orderPaths"]
        )
        or sequence_order is None
        or sequence_order["fromBranch"] != "then_0"
        or sequence_order["toBranch"] != "then_1"
        or sequence_order["ordered"] is not True
        or not calendar_startup_order["sequenceSemanticsSource"].startswith(
            "https://dev.epicgames.com/"
        )
    ):
        raise AssertionError("Calendar-before-Sun startup ordering changed")
    binding_startup_order = payload["calendarBindingStartupOrder"]
    binding_sequence = binding_startup_order["orderPaths"][0]["sequenceOrder"]
    if (
        binding_startup_order["status"] != "VERIFIED"
        or binding_sequence is None
        or binding_sequence["fromBranch"] != "then_0"
        or binding_sequence["toBranch"] != "then_1"
        or binding_sequence["ordered"] is not True
    ):
        raise AssertionError("Calendar-before-DateChanged-binding order changed")
    construction_order = payload["constructionStartupOrder"]
    if (
        construction_order["status"] != "VERIFIED"
        or construction_order["orderPaths"][0]["directPath"]
        != [4285, 4283, 4282, 4284]
    ):
        raise AssertionError("UserConstructionScript startup order changed")
    first_use = payload["calendarPreFirstUseAudit"]
    if (
        first_use["status"] != "PARTIAL"
        or first_use["authoredStartupCalendarPath"]["nodes"]
        != [5809, 5327, 4232]
        or first_use["preStartupConstructionAudit"]["status"]
        != "VERIFIED_AUTHORED_PATH_DOES_NOT_REACH_ASTRONOMY"
        or first_use["preStartupConstructionAudit"]["sourceCall"]["Hour"] != 0
        or first_use["preStartupConstructionAudit"]["astronomyGuard"][
            "requiredHour"
        ]
        != 100
        or first_use["dateChangedBinding"]["status"] != "VERIFIED"
        or first_use["dateChangedBinding"]["bindingExecPath"]["nodes"]
        != [5721, 5266, 5264, 5265, 5267, 1336]
        or {
            tuple(item["nodes"])
            for item in first_use["dateChangedBinding"]["broadcastPaths"]
        }
        != {
            (5003, 3300, 3286, 6458, 12896, 1640),
            (5004, 3300, 3286, 6458, 12896, 1640),
            (5005, 3300, 3286, 6458, 12896, 1640),
        }
        or first_use["externalPublicInvocationCoverage"]["status"] != "PARTIAL"
    ):
        raise AssertionError("Calendar pre-first-use audit changed")
    orientation_graphs = {
        item["sourceName"]: item for item in payload["orientationGraphs"]
    }
    if tuple(payload["orientationFunctionSlice"]) != EXPECTED_ORIENTATION_FUNCTION_SLICE:
        raise AssertionError("orientation function slice changed")
    if set(orientation_graphs) != set(EXPECTED_ORIENTATION_FUNCTION_SLICE):
        raise AssertionError("orientation graph coverage is incomplete")
    calendar_graph = orientation_graphs["Static Properties - Calendar"]
    calendar_fallback = next(
        node
        for node in calendar_graph["nodes"]
        if node["exportIndex"] == 13043
    )
    calendar_pin = next(
        pin for pin in calendar_fallback["pins"] if pin["name"] == "Calendar"
    )
    if calendar_pin["defaultObject"] != (
        "/Game/UltraDynamicSky/Blueprints/System/Calendars/"
        "Gregorian_Calendar.Gregorian_Calendar"
    ):
        raise AssertionError("authored Gregorian Calendar fallback changed")
    for formula in payload["orientationFormulaLedger"]:
        graph = orientation_graphs[formula["function"]]
        decoded_operations = {
            node.get("operation") or node.get("variable") or node.get("class")
            for node in graph["nodes"]
        }
        missing_operations = sorted(set(formula["sourceNodes"]) - decoded_operations)
        if missing_operations:
            raise AssertionError(
                f"orientation formula {formula['function']} cites absent nodes: "
                f"{missing_operations}"
            )
    expected_orientation_statuses = {
        "Solar Eclipse Circle Mask": "VERIFIED",
        "Current Solar Eclipse Values": "VERIFIED",
        "H/M/S/MS to Time of Day": "VERIFIED",
        "Check If Year is Leap Year": "VERIFIED",
        "Current Month Lengths": "VERIFIED",
        "Day Count at the Start of a Month": "VERIFIED",
        "Number of Days in a Year": "VERIFIED",
        "Offset Date by a Number of Days": "VERIFIED",
        "Static Properties - Calendar": "PARTIAL",
        "Force Valid Day": "VERIFIED",
        "Simulation Horizon Compensation": "VERIFIED",
        "AP - Sun Root Vector": "PARTIAL",
        "Cache Sun and Moon Orientation": "PARTIAL",
        "Approximate Real Sun Moon and Stars": "PARTIAL",
    }
    if {
        item["function"]: item["status"]
        for item in payload["orientationFormulaLedger"]
    } != expected_orientation_statuses:
        raise AssertionError("orientation formula status ledger changed")
    astronomy = payload["astronomySourceAssets"]
    if (
        astronomy["status"] != "PARTIAL"
        or astronomy["implementationStatus"] != "NOT_IMPLEMENTED"
        or astronomy["equationOfTime"]["status"] != "VERIFIED"
        or astronomy["equationOfTime"]["keyCount"] != 13
        or astronomy["calendar"]["status"] != "PARTIAL"
        or astronomy["calendar"]["candidateDefaultRuntimeDerivedValues"]
        != {
            "status": "PARTIAL_LOCAL_DAY_COUNT_DEFAULT_NOT_YET_ENGINE_VERIFIED",
            "requiredInitialAccumulator": 0,
            "Month Lengths": [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31],
            "Month Lengths (Leap Year)": [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31],
            "Day Count At Start of Each Month": [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334],
            "Day Count At Start of Each Month (Leap Year)": [0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335],
            "Number of Days in Year": 365,
        }
    ):
        raise AssertionError("real-Sun source-asset coverage changed")
    if any(
        "path" in key.lower() or "physical" in key.lower() or "provenance" in key.lower()
        for key in astronomy["equationOfTime"]["keys"][0]
    ):
        raise AssertionError("private physical curve provenance leaked into checkpoint")
    graphs = {item["sourceName"]: item for item in payload["graphs"]}
    for formula in payload["formulaLedger"]:
        graph = graphs[formula["function"]]
        decoded_operations = {
            node.get("operation") or node.get("variable") or node.get("class")
            for node in graph["nodes"]
        }
        missing_operations = sorted(set(formula["sourceNodes"]) - decoded_operations)
        if missing_operations:
            raise AssertionError(
                f"formula {formula['function']} cites absent nodes: {missing_operations}"
            )
    if set(MATERIAL_SEMANTICS) != {
        item["sourceAsset"] for item in payload["materialFormulaLedger"]
    }:
        raise AssertionError("Sun Material Function semantics do not cover exact dependency slice")
    if any(item["status"] != "VERIFIED" for item in payload["materialFormulaLedger"]):
        raise AssertionError("Sun Material Function expression program is not closed")
    if any(item["implementationStatus"] != "NOT_IMPLEMENTED" for item in payload["materialFormulaLedger"]):
        raise AssertionError("source Material decompilation must not claim runtime implementation")
    programs = payload["materialExpressionPrograms"]
    if len(programs) != len(payload["materialFormulaLedger"]):
        raise AssertionError("Material expression programs do not cover formula ledger")
    program_assets = {item["sourceAsset"] for item in programs}
    for program in programs:
        if program["status"] != "VERIFIED":
            raise AssertionError(f"Material expression program is partial: {program['sourceAsset']}")
        if (
            program["unknownExpressionClasses"]
            or program["unresolvedInputs"]
            or program["unresolvedEngineDefaults"]
            or program["unresolvedCallBindings"]
            or program["unresolvedOutputs"]
        ):
            raise AssertionError(f"Material expression program is unresolved: {program['sourceAsset']}")
        for node in program["nodes"]:
            if node["operation"] != "MATERIAL_FUNCTION_CALL":
                continue
            if any(
                not binding["status"].startswith("VERIFIED_")
                for binding in node.get("resolvedCallBindings", [])
            ):
                raise AssertionError(
                    f"Material function call default is unresolved: {program['sourceAsset']}"
                )
            if any(item.get("valueResolved") is not True for item in node["inputs"]):
                raise AssertionError(
                    f"Material function call input has no exact value: {program['sourceAsset']}"
                )
        if set(program["calledFunctions"]) - program_assets:
            raise AssertionError(f"Material expression calls leave dependency slice: {program['sourceAsset']}")
    for fixture in payload["curveFixtures"]:
        if fixture["status"] != "VERIFIED" or not fixture["samples"]:
            raise AssertionError(f"Sun curve fixture is not evaluable: {fixture['sourceAsset']}")
        source_curve = next(
            curve for curve in payload["curves"] if curve["sourceAsset"] == fixture["sourceAsset"]
        )
        sample_by_time = {sample["input"]: sample for sample in fixture["samples"]}
        for channel in source_curve["channels"]:
            for key in channel["keys"]:
                actual = sample_by_time[float(key["time"])]["outputs"][channel["name"]]
                if abs(actual - float(key["value"])) > 1.0e-6:
                    raise AssertionError(
                        f"curve key fixture mismatch: {fixture['sourceAsset']} {channel['name']}"
                    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset", type=Path, default=DEFAULT_DATASET)
    parser.add_argument("--system-contract", type=Path, default=DEFAULT_SYSTEM_CONTRACT)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    summary = build(args.dataset, args.system_contract, args.output)
    if args.self_test:
        self_test(args.output, summary)
    print(json.dumps({"status": "PASS", **summary}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
