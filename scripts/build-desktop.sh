#!/usr/bin/env bash
set -euo pipefail

# Builds the Tauri desktop app and copies resulting bundles into /bin/<os>.
# Run this script on each target OS (Linux/macOS/Windows via Git Bash).

usage() {
  cat <<'EOF'
Usage:
  ./scripts/build-desktop.sh [linux|macos|windows] [--install-deps] [--dry-run]

Options:
  --install-deps  Best-effort install of Tauri system dependencies for your OS/distro.
                 Requires sudo on Linux and Homebrew on macOS.
  --dry-run       Print what would be installed (only with --install-deps).

Examples:
  ./scripts/build-desktop.sh
  ./scripts/build-desktop.sh --install-deps
  ./scripts/build-desktop.sh linux --install-deps --dry-run
EOF
}

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WEB_DIR="$ROOT_DIR/web"
TAURI_DIR="$WEB_DIR/src-tauri"

if [[ ! -d "$TAURI_DIR" ]]; then
  echo "error: expected Tauri project at $TAURI_DIR" >&2
  exit 1
fi

OS=""
INSTALL_DEPS="false"
DRY_RUN="false"

for arg in "$@"; do
  case "$arg" in
    linux|macos|windows)
      OS="$arg"
      ;;
    --install-deps)
      INSTALL_DEPS="true"
      ;;
    --dry-run)
      DRY_RUN="true"
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "error: unknown argument '$arg'" >&2
      usage >&2
      exit 1
      ;;
  esac
done

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

case "$OS" in
  linux) TAURI_BUNDLE_TARGETS_VALUE="appimage,deb,rpm" ;;
  macos) TAURI_BUNDLE_TARGETS_VALUE="app,dmg" ;;
  windows) TAURI_BUNDLE_TARGETS_VALUE="msi,nsis" ;;
esac

mkdir -p "$OUT_DIR" "$STAGING_DIR"

require_cmd() {
  local cmd="$1"
  local hint="$2"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "error: '$cmd' is required but was not found in PATH." >&2
    if [[ -n "$hint" ]]; then
      echo "hint: $hint" >&2
    fi
    exit 1
  fi
}

require_cmd node "Install Node.js (see web/package.json engines)."
require_cmd npm "Install npm (usually comes with Node.js)."
require_cmd cargo "Install Rust (rustup) so Tauri can compile native binaries."
require_cmd rustc "Install Rust (rustup) so Tauri can compile native binaries."

detect_linux_distro() {
  # outputs an ID like: ubuntu, debian, fedora, arch
  if [[ -r /etc/os-release ]]; then
    . /etc/os-release
    echo "${ID:-unknown}"
    return 0
  fi
  echo "unknown"
}

install_deps_linux() {
  local distro
  distro="$(detect_linux_distro)"

  # Tauri v2 (wry/webkit2gtk) common deps. Package names differ per distro.
  case "$distro" in
    ubuntu|debian|linuxmint|pop)
      local pkgs=(
        pkg-config
        libglib2.0-dev
        libgtk-3-dev
        libwebkit2gtk-4.1-dev
        libappindicator3-dev
        librsvg2-dev
        patchelf
      )
      if [[ "$DRY_RUN" == "true" ]]; then
        echo "dry-run: sudo apt update" >&2
        echo "dry-run: sudo apt install -y ${pkgs[*]}" >&2
        return 0
      fi
      sudo apt update
      sudo apt install -y "${pkgs[@]}"
      ;;
    fedora)
      local pkgs=(
        pkgconf-pkg-config
        glib2-devel
        gtk3-devel
        webkit2gtk4.1-devel
        libappindicator-gtk3-devel
        librsvg2-devel
        patchelf
      )
      if [[ "$DRY_RUN" == "true" ]]; then
        echo "dry-run: sudo dnf install -y ${pkgs[*]}" >&2
        return 0
      fi
      sudo dnf install -y "${pkgs[@]}"
      ;;
    arch|manjaro)
      local pkgs=(
        pkgconf
        glib2
        gtk3
        webkit2gtk-4.1
        libappindicator-gtk3
        librsvg
        patchelf
      )
      if [[ "$DRY_RUN" == "true" ]]; then
        echo "dry-run: sudo pacman -S --needed ${pkgs[*]}" >&2
        return 0
      fi
      sudo pacman -S --needed "${pkgs[@]}"
      ;;
    *)
      echo "error: unsupported/unknown Linux distro '$distro' for auto-install." >&2
      echo "hint: install pkg-config + glib/gtk/webkit2gtk dev packages for your distro." >&2
      return 1
      ;;
  esac
}

install_deps_macos() {
  if ! command -v brew >/dev/null 2>&1; then
    echo "error: Homebrew is required for --install-deps on macOS." >&2
    echo "hint: https://brew.sh" >&2
    return 1
  fi
  # For most setups, Xcode Command Line Tools are also required for building.
  local pkgs=(
    pkg-config
  )
  if [[ "$DRY_RUN" == "true" ]]; then
    echo "dry-run: brew install ${pkgs[*]}" >&2
    echo "dry-run: xcode-select --install (if not already installed)" >&2
    return 0
  fi
  brew install "${pkgs[@]}" || true
  if ! xcode-select -p >/dev/null 2>&1; then
    echo "xcode command line tools missing; running xcode-select --install" >&2
    xcode-select --install || true
  fi
}

install_deps_windows() {
  # Windows install automation is messy; we do best-effort guidance.
  echo "Windows auto-install is best-effort." >&2
  echo "Recommended: Git Bash + Rust (rustup) + MSVC Build Tools." >&2
  echo "Then run: cd web && npm install && npm run tauri:build" >&2
  return 0
}


if [[ "$INSTALL_DEPS" == "true" ]]; then
  echo "Installing Tauri system dependencies for ${OS}…" >&2
  case "$OS" in
    linux) install_deps_linux ;;
    macos) install_deps_macos ;;
    windows) install_deps_windows ;;
  esac
  if [[ "$DRY_RUN" == "true" ]]; then
    echo "dry-run: skipping build." >&2
    exit 0
  fi
fi

echo "[1/3] Building web + Tauri for ${OS}…"
(
  cd "$WEB_DIR"
  echo "   using TAURI_BUNDLE_TARGETS=${TAURI_BUNDLE_TARGETS_VALUE}" >&2
  TAURI_BUNDLE_TARGETS="$TAURI_BUNDLE_TARGETS_VALUE" npm run -s tauri:build
)

# Tauri outputs bundles under src-tauri/target/release/bundle
BUNDLE_DIR="$TAURI_DIR/target/release/bundle"
if [[ ! -d "$BUNDLE_DIR" ]]; then
  echo "error: expected bundle output at $BUNDLE_DIR" >&2
  exit 1
fi

echo "[2/3] Copying bundles into ${OUT_DIR}…"
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