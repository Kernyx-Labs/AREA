#!/usr/bin/env bash
set -euo pipefail

# Builds the Tauri desktop app and copies resulting bundles into /bin/<os>.
# Run this script on each target OS (Linux/macOS/Windows via Git Bash).

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WEB_DIR="$ROOT_DIR/web"
TAURI_DIR="$WEB_DIR/src-tauri"

if [[ ! -d "$TAURI_DIR" ]]; then
  echo "error: expected Tauri project at $TAURI_DIR" >&2
  exit 1
fi

OS="${1:-}"
if [[ -z "$OS" ]]; then
  case "$(uname -s)" in
    Linux) OS="linux";;
    Darwin) OS="macos";;
    MINGW*|MSYS*|CYGWIN*) OS="windows";;
    *)
      echo "error: unsupported OS $(uname -s). Pass one of: linux|macos|windows" >&2
      exit 1
      ;;
  esac
fi

case "$OS" in
  linux|macos|windows) ;;
  *)
    echo "error: invalid OS '$OS'. Use: linux|macos|windows" >&2
    exit 1
    ;;
esac

OUT_DIR="$ROOT_DIR/bin/$OS"
STAGING_DIR="$OUT_DIR/_staging"

mkdir -p "$OUT_DIR" "$STAGING_DIR"

echo "[1/3] Building web + Tauri for $OS…"
(
  cd "$WEB_DIR"
  npm run -s tauri:build
)

# Tauri outputs bundles under src-tauri/target/release/bundle
BUNDLE_DIR="$TAURI_DIR/target/release/bundle"
if [[ ! -d "$BUNDLE_DIR" ]]; then
  echo "error: expected bundle output at $BUNDLE_DIR" >&2
  exit 1
fi

echo "[2/3] Copying bundles into $OUT_DIR…"
rm -rf "$STAGING_DIR"/*

# Copy everything to keep it simple (dmg/appimage/msi/deb/etc depending on platform).
# Users can pick what they want.
cp -R "$BUNDLE_DIR"/* "$STAGING_DIR"/ 2>/dev/null || true

# Promote staging into a versioned folder if possible
VERSION=""
CONF_JSON="$TAURI_DIR/tauri.conf.json"
if command -v node >/dev/null 2>&1; then
  VERSION="$(node -e "const fs=require('fs'); const p=process.argv[1]; const c=JSON.parse(fs.readFileSync(p,'utf8')); process.stdout.write(String(c.version||''));" "$CONF_JSON" 2>/dev/null || true)"
fi

DEST="$OUT_DIR/latest"
rm -rf "$DEST"
mkdir -p "$DEST"
cp -R "$STAGING_DIR"/* "$DEST"/ 2>/dev/null || true

if [[ -n "$VERSION" ]]; then
  VERSIONED="$OUT_DIR/$VERSION"
  rm -rf "$VERSIONED"
  mkdir -p "$VERSIONED"
  cp -R "$STAGING_DIR"/* "$VERSIONED"/ 2>/dev/null || true
fi

echo "[3/3] Done. Artifacts:" 
find "$DEST" -maxdepth 2 -type f 2>/dev/null | sed 's#^# - #' || true

echo "\nTip: run this script on each OS to populate bin/linux, bin/macos, bin/windows."