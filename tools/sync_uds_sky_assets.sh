#!/usr/bin/env bash
set -u

SRC_DIR="${1:-/storage/emulated/0/Download/SOLUM_UDS_USER_SKY}"
DEST_DIR="apps/engine/src/main/assets/private_premium/uds_sky"
MANIFEST="$DEST_DIR/uds_sky_manifest.local.json"

FILES=(
  "Moon_Color.png"
  "Moon_PhaseNormal.png"
  "Real_Stars.png"
  "Tiling_Stars.png"
  "Sun_Atmosphere_LUT.png"
  "Sun_Atmosphere_LUT_Volume.png"
  "Prime_Flare.png"
)

size_bytes() {
  if stat -c '%s' "$1" >/dev/null 2>&1; then
    stat -c '%s' "$1"
  else
    wc -c < "$1" | tr -d ' '
  fi
}

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

mkdir -p "$DEST_DIR"

echo "UDS sky asset sync"
echo "source=$SRC_DIR"
echo "dest=$DEST_DIR"
echo "private_premium_git_policy=ignored_by_repo_gitignore"

copied=0
missing=0
copied_names=()
missing_names=()

for name in "${FILES[@]}"; do
  src="$SRC_DIR/$name"
  dst="$DEST_DIR/$name"
  if [ -f "$src" ]; then
    cp "$src" "$dst"
    bytes="$(size_bytes "$dst")"
    copied=$((copied + 1))
    copied_names+=("$name:$bytes")
    echo "copied $name bytes=$bytes"
  else
    missing=$((missing + 1))
    missing_names+=("$name")
    echo "missing_optional $name"
  fi
done

copy_time="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
tmp="$MANIFEST.tmp"
{
  echo "{"
  echo "  \"schema\": \"solum_uds_sky_private_asset_manifest\","
  echo "  \"schemaVersion\": 1,"
  echo "  \"sourceFolder\": \"$(json_escape "$SRC_DIR")\","
  echo "  \"destinationFolder\": \"$(json_escape "$DEST_DIR")\","
  echo "  \"copyTimeUtc\": \"$(json_escape "$copy_time")\","
  echo "  \"privateAssetPolicy\": \"local_only_private_premium_folder_gitignored\","
  echo "  \"files\": ["
  first=1
  for name in "${FILES[@]}"; do
    path="$DEST_DIR/$name"
    [ "$first" -eq 0 ] && echo ","
    first=0
    if [ -f "$path" ]; then
      bytes="$(size_bytes "$path")"
      printf '    {"fileName":"%s","status":"copied_or_present","sizeBytes":%s}' "$(json_escape "$name")" "$bytes"
    else
      printf '    {"fileName":"%s","status":"missing_optional","sizeBytes":0}' "$(json_escape "$name")"
    fi
  done
  echo
  echo "  ],"
  echo "  \"summary\": {\"copiedCount\": $copied, \"missingOptionalCount\": $missing}"
  echo "}"
} > "$tmp"
mv "$tmp" "$MANIFEST"

echo "manifest=$MANIFEST"
echo "copied_count=$copied"
echo "missing_optional_count=$missing"
exit 0
