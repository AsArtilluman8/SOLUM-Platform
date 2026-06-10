# Asset Policy

SOLUM public GitHub repositories may contain only:

- original SOLUM code;
- generated assets created for SOLUM;
- CC0 or permissive assets with clear license notes.

Purchased Unreal Marketplace, Fab, or Marketplace-style asset packs must stay local and private. They may be used for local builds, experiments, visual reference, or architecture study, but paid source files must not be committed to GitHub.

Forbidden in the public repo:

- raw paid asset files;
- Unreal native asset files such as `.uasset`, `.umap`, `.uexp`, `.ubulk`, and `.uplugin`;
- copied Marketplace/Fab content folders;
- private local asset exports that include paid source data.

Private assets should use the local private asset pipeline and ignored folders such as `private_assets/`, `_private_assets/`, `local_assets/`, or private app asset folders. Public code that consumes private assets must remain original SOLUM code and must not depend on committed paid content.

Ultra Dynamic Sky may be used locally as a reference for architecture and behavior. Public SOLUM code must be original and must not copy UDS source assets, blueprints, materials, textures, maps, or paid content.
