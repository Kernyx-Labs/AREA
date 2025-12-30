import fs from 'node:fs'
import path from 'node:path'
import { execSync } from 'node:child_process'

// Icon generator for Tauri.
// We derive icons from the web logo to keep branding consistent:
//   web/public/AREA.png
//
// Requirements:
// - Output PNGs must be valid and decodeable by GTK/GDK (Linux runtime window icon)
// - Prefer RGBA to satisfy Tauri/icon expectations
//
// Implementation:
// - Uses ImageMagick's `convert` if available (recommended)
// - Falls back to copying the source image (warning) if ImageMagick is missing

const root = path.resolve(new URL('.', import.meta.url).pathname, '..')
const tauriDir = path.resolve(root, 'src-tauri')
const iconsDir = path.resolve(tauriDir, 'icons')
const srcPng = path.resolve(root, 'public', 'AREA.png')

fs.mkdirSync(iconsDir, { recursive: true })

if (!fs.existsSync(srcPng)) {
  console.error(`[tauri-icons] missing source icon: ${srcPng}`)
  process.exit(1)
}

const outputs = [
  { name: '32x32.png', size: 32 },
  { name: '128x128.png', size: 128 }
]

function which(cmd) {
  try {
    execSync(`command -v ${cmd}`, {
      stdio: 'ignore',
      shell: true
    })
    return true
  } catch {
    return false
  }
}

// ImageMagick v7 prefers `magick`, but many distros also ship `convert`.
const imagemagickCmd = which('magick') ? 'magick' : which('convert') ? 'convert' : null

// Generate PNG icons
for (const { name, size } of outputs) {
  const outPath = path.resolve(iconsDir, name)

  if (imagemagickCmd) {
    // -resize: generate target size
    // -background none -alpha on: ensure alpha channel exists
    // -define png:color-type=6: force RGBA
    // -strip: remove profiles/metadata that occasionally confuse older tooling
    const args = [
      imagemagickCmd,
      // For v7, `magick` uses subcommand `convert`; for legacy `convert`, no subcommand.
      ...(imagemagickCmd === 'magick' ? ['convert'] : []),
      srcPng,
      '-resize',
      `${size}x${size}`,
      '-background',
      'none',
      '-alpha',
      'on',
      '-define',
      'png:color-type=6',
      '-strip',
      outPath
    ]
    execSync(args.map((a) => JSON.stringify(a)).join(' '), {
      stdio: 'inherit',
      shell: true
    })
  } else {
    // Fallback: copy source image; Tauri config references only these files.
    // It may work, but it's better to install ImageMagick for proper resizing.
    fs.copyFileSync(srcPng, outPath)
  }
}

console.log(`[tauri-icons] wrote ${outputs.length} icons to ${iconsDir} (source: ${srcPng})`)
