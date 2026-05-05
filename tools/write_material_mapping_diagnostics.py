#!/usr/bin/env python3
import json
from pathlib import Path

root = Path('/storage/emulated/0/SOLUMCreative/diagnostics/latest')
root.mkdir(parents=True, exist_ok=True)

material = {
    'schema': 'solum.runtime_material_state',
    'schemaVersion': 2,
    'status': 'mapping_ready_not_rendering_pbr_yet',
    'standard': 'glTF 2.0 metallic-roughness',
    'importerDecision': 'cgltf_first',
    'reason': 'P12B defines correct glTF material mapping before PBR shader implementation.',
    'textureSlots': {
        'baseColor': {
            'requiredColorSpace': 'sRGB',
            'gltfFields': ['baseColorFactor', 'baseColorTexture']
        },
        'normal': {
            'requiredColorSpace': 'linear',
            'requiresTangents': True,
            'gltfFields': ['normalTexture']
        },
        'metallicRoughness': {
            'requiredColorSpace': 'linear',
            'roughnessChannel': 'G',
            'metallicChannel': 'B',
            'gltfFields': ['metallicFactor', 'roughnessFactor', 'metallicRoughnessTexture']
        },
        'occlusion': {
            'requiredColorSpace': 'linear',
            'channel': 'R',
            'gltfFields': ['occlusionTexture']
        },
        'emissive': {
            'requiredColorSpace': 'sRGB',
            'gltfFields': ['emissiveFactor', 'emissiveTexture']
        },
        'alpha': {
            'modes': ['OPAQUE', 'MASK', 'BLEND'],
            'gltfFields': ['alphaMode', 'alphaCutoff', 'doubleSided']
        }
    },
    'forbiddenShortcuts': [
        'baseColor-only material pretending to be PBR',
        'normal map without tangent handling',
        'texture atlas as replacement for glTF material mapping',
        'manual color tweaks to imitate author intent',
        'BLEND material inside permanent opaque pass'
    ],
    'next': 'P13 glTF/GLB import probe + texture slot diagnostics'
}

texture = {
    'schema': 'solum.runtime_texture_state',
    'schemaVersion': 1,
    'status': 'texture_slots_declared_not_uploaded_yet',
    'slots': list(material['textureSlots'].keys()),
    'requiredBeforePbr': [
        'image decode path',
        'sampler creation',
        'image upload path',
        'descriptor set layout',
        'sRGB vs linear VkFormat selection'
    ]
}

(root / 'runtime_material_state.json').write_text(json.dumps(material, indent=2), encoding='utf-8')
(root / 'runtime_texture_state.json').write_text(json.dumps(texture, indent=2), encoding='utf-8')
print('Material mapping diagnostics written:')
print(root / 'runtime_material_state.json')
print(root / 'runtime_texture_state.json')
