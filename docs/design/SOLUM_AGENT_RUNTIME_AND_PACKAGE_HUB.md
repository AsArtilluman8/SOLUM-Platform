# SOLUM Agent Runtime and Package Hub

Status: strategic future design.

This document describes a long-term architecture direction. It is not a claim that these systems already exist.

SOLUM should eventually support AI-assisted game creation through an Agent Console and a package ecosystem for reusable mechanics, assets, materials, VFX, animations, UI kits, audio, and templates.

## Vision

Users should be able to connect a local or cloud agent that can help inspect a project, suggest changes, create mechanics, configure scenes, run tests, explain errors, and prepare packages under explicit permission rules.

The goal is not to give an agent unlimited shell access by default. The goal is a controlled, understandable, auditable workflow.

## Agent Console

The Agent Console may eventually allow users to connect:

- local models;
- cloud AI services;
- code agents;
- project-specific assistants;
- testing/build agents;
- package authoring agents.

Potential agent capabilities:

- read project structure;
- inspect scene and asset metadata;
- suggest patches;
- create mechanic packages;
- configure render/scene settings;
- run tests and builds;
- explain failures;
- generate documentation;
- create package manifests.

## Permission Levels

Agents should operate under explicit permissions:

| Level | Description |
|---|---|
| Read Only | Agent can inspect project files and explain state |
| Suggest Patch | Agent can propose changes, user applies manually |
| Edit Project | Agent can edit project files within project scope |
| Build/Test | Agent can run approved build/test commands |
| Package Author | Agent can create package manifests and test scenes |
| Advanced Dev | Explicit high-trust mode for experienced users |

Dangerous unrestricted shell access should not be the default UX.

## Package Types

Future package types may include:

- `assetpack`
- `materialpack`
- `vfxpack`
- `mechanicpack`
- `animationpack`
- `uipack`
- `audiopack`
- `scene-template`
- `agent-skill`

## Package Manifest

Each package should include a manifest with:

```json
{
  "name": "Example Package",
  "type": "mechanicpack",
  "version": "1.0.0",
  "author": "creator-name",
  "license": "MIT OR CC0 OR CC-BY-4.0",
  "permissions": ["read_scene", "add_components"],
  "dependencies": ["solum.scene", "solum.animation"],
  "entrypoints": [],
  "preview": [],
  "docs": []
}
```

The exact schema will evolve, but the platform should support:

- author;
- version;
- license;
- dependencies;
- permissions;
- preview assets;
- documentation;
- test scene;
- compatibility info.

## Mechanic Packages

Mechanic packages should make common systems reusable:

- third-person camera;
- inventory;
- dialogue;
- climbing;
- swimming;
- lock-on combat;
- quest system;
- save system;
- vehicle controller;
- AI behaviors;
- interaction prompts;
- item pickup;
- ability system.

Users should be able to install a mechanic package, assign it to scene objects, configure values, and inspect what the package changes.

## Agent + Package Workflow

Example future workflow:

1. User asks for a climbing mechanic.
2. Agent searches installed packages or remote Package Hub.
3. Agent explains package permissions.
4. User approves installation.
5. Agent adds package to project.
6. Agent creates a test scene.
7. Agent runs diagnostics/build.
8. User tunes values through UI/nodes/API.

## Safety Principles

- show what the agent wants to change;
- require permissions;
- keep project backups/checkpoints;
- log changes;
- expose package dependencies;
- distinguish generated code from user code;
- do not hide license requirements.

## Future Connection To Labs

Labs can produce packages:

- Material Lab -> `materialpack`
- VFX Lab -> `vfxpack`
- Animation Lab -> `animationpack`
- UI Lab -> `uipack`
- Agent Console -> `agent-skill`
- Scene Workspace -> `scene-template`

The engine/editor should consume those packages through common APIs.
