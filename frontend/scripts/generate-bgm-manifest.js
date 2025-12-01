#!/usr/bin/env node
/**
 * Generate BGM manifest for custom tracks
 * 
 * This script scans the bgm/custom folder for audio files and creates
 * a manifest.json that the app uses to load custom background music.
 * 
 * Usage: node scripts/generate-bgm-manifest.js
 */

import { readdir, writeFile } from 'fs/promises';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const CUSTOM_BGM_DIR = join(__dirname, '../static/bgm/custom');
const MANIFEST_PATH = join(CUSTOM_BGM_DIR, 'manifest.json');

// Supported audio file extensions
const AUDIO_EXTENSIONS = ['.mp3', '.wav', '.ogg', '.m4a', '.flac'];

async function generateManifest() {
  try {
    console.log('📁 Scanning bgm/custom folder...');
    
    const files = await readdir(CUSTOM_BGM_DIR);
    
    // Filter for audio files only
    const audioFiles = files.filter(file => {
      const ext = file.substring(file.lastIndexOf('.')).toLowerCase();
      return AUDIO_EXTENSIONS.includes(ext);
    }).sort(); // Sort alphabetically for consistency
    
    console.log(`🎵 Found ${audioFiles.length} audio file(s):`);
    audioFiles.forEach(file => console.log(`   - ${file}`));
    
    // Create manifest
    const manifest = {
      tracks: audioFiles,
      generated: new Date().toISOString(),
      note: "This file is auto-generated. Add MP3 files to bgm/custom/ and run: npm run generate-bgm"
    };
    
    // Write manifest
    await writeFile(
      MANIFEST_PATH,
      JSON.stringify(manifest, null, 2) + '\n',
      'utf8'
    );
    
    console.log('✅ manifest.json generated successfully!');
    console.log(`📝 Manifest location: ${MANIFEST_PATH}`);
    
  } catch (error) {
    console.error('❌ Error generating manifest:', error);
    process.exit(1);
  }
}

generateManifest();
