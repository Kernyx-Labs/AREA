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
// - Falls back to `tauri icon` (via @tauri-apps/cli) to ensure RGBA output when ImageMagick is missing

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

if (imagemagickCmd) {
  // Generate PNG icons via ImageMagick when available for consistent cross-platform output.
  for (const { name, size } of outputs) {
    const outPath = path.resolve(iconsDir, name)
    const args = [
      imagemagickCmd,
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
  }

  console.log(`[tauri-icons] wrote ${outputs.length} icons to ${iconsDir} (source: ${srcPng})`)
  process.exit(0)
}

// Fallback: leverage the local Tauri CLI to generate PNGs with guaranteed RGBA channels.
const pngFlags = outputs.map(({ size }) => `-p ${size}`).join(' ')
const tauriIconCmd = `npx tauri icon ${JSON.stringify(srcPng)} ${pngFlags} -o ${JSON.stringify(iconsDir)}`

try {
  execSync(tauriIconCmd, {
    stdio: 'inherit',
    cwd: root,
    shell: true
  })
  console.log(`[tauri-icons] wrote ${outputs.length} icons to ${iconsDir} via tauri icon (source: ${srcPng})`)
} catch (error) {
  console.error('[tauri-icons] unable to generate icons.')
  console.error('Install ImageMagick (magick/convert) or ensure the Tauri CLI is available locally.')
  throw error
}
