/**
 * SoundManager - Howler.js wrapper for game audio
 * 
 * Usage:
 *   import { sounds, bgm } from '$lib/audio/SoundManager';
 *   sounds.play('hit');
 *   bgm.play('main_menu');
 *   bgm.playForTopic('ai');
 * 
 * BGM Naming Convention:
 *   - main_menu_* : Main menu tracks (e.g., main_menu_Driftveil.mp3)
 *   - battle_[topic]_* : Battle tracks for built-in topics (e.g., battle_ai_1.mp3)
 *   - custom_* : Default tracks for custom loaded questions (e.g., custom_1.mp3)
 */

import { Howl } from 'howler';

// Uniform volume level for all BGM (normalized)
// Lowered to 0.15 so SFX (hit, damage, etc.) can be heard clearly
const BGM_VOLUME = 0.15;

// Gap between loops in milliseconds (10 seconds)
const LOOP_GAP_MS = 10000;

// Sound effect definitions
const soundDefs: Record<string, { src: string[]; volume?: number }> = {
	hit: { src: ['sfx/hit.mp3'], volume: 0.6 },
	miss: { src: ['sfx/damage.mp3'], volume: 0.4 },
	crit: { src: ['sfx/crit.mp3'], volume: 0.8 },
	victory: { src: ['sfx/victory.mp3'], volume: 0.7 },
	defeat: { src: ['sfx/defeat.mp3'], volume: 0.6 },
	select: { src: ['sfx/select.mp3'], volume: 0.3 },
	correct: { src: ['sfx/correct.mp3'], volume: 0.5 },
	wrong: { src: ['sfx/wrong.mp3'], volume: 0.5 },
	encounter: { src: ['sfx/select.mp3'], volume: 0.5 }, // Reuse select for now
};

/**
 * BGM Registry - Organized by category for future expansion
 * 
 * Categories:
 *   - main_menu: Tracks for the main menu (can have multiple for randomization)
 *   - battle_ai, battle_cs, battle_philosophy: Topic-specific battle tracks
 *   - custom: Dynamically loaded from bgm/custom/manifest.json
 * 
 * Note: Custom tracks are loaded at runtime from manifest.json
 * Users can add MP3 files to bgm/custom/ and update the manifest
 */
const bgmRegistry: Record<string, string[]> = {
	// Main menu tracks (currently 1, expandable)
	main_menu: [
		'bgm/main_menu_Driftveil.mp3',
		'bgm/main_menu_fukashigi.mp3',
	],
	// Battle tracks per topic (indexed, expandable)
	battle_ai: [
		'bgm/main_menu_fukashigi.mp3',
	],
	battle_cs: [
		'bgm/bg_2_N_Battle.mp3',
	],
	battle_philosophy: [
		'bgm/bg_3_Cynthia_Battle.mp3',
	],
	// Custom tracks - will be populated dynamically from manifest
	// Fallback included in case manifest hasn't loaded yet
	custom: [
		'bgm/main_menu_fukashigi.mp3',
	],
};

/**
 * Load custom BGM tracks from manifest.json
 * Only runs in browser (not during SSR)
 */
async function loadCustomTracks(): Promise<void> {
	// Skip during SSR - audio only works in browser
	if (typeof window === 'undefined') {
		return;
	}
	
	try {
		const response = await fetch('/bgm/custom/manifest.json');
		if (response.ok) {
			const manifest = await response.json();
			if (manifest.tracks && Array.isArray(manifest.tracks)) {
				bgmRegistry.custom = manifest.tracks.map((filename: string) => 
					`bgm/custom/${filename}`
				);
				console.log(`[BGM] Loaded ${bgmRegistry.custom.length} custom tracks from manifest`);
			}
		}
	} catch (error) {
		console.warn('[BGM] Failed to load custom tracks manifest, using fallback:', error);
		// Fallback to a default track if manifest fails
		bgmRegistry.custom = ['bgm/main_menu_fukashigi.mp3'];
	}
}

// Load custom tracks on module initialization
loadCustomTracks();

// Built-in topics that have dedicated battle music
const BUILT_IN_TOPICS = ['ai', 'cs', 'philosophy'];

class SoundManager {
	private sounds: Map<string, Howl> = new Map();
	private muted: boolean = false;
	private volume: number = 1.0;

	constructor() {
		// Lazy-load sounds on first play to avoid blocking initial load
	}

	/**
	 * Initialize a sound if not already loaded
	 */
	private ensureLoaded(name: string): Howl | null {
		if (this.sounds.has(name)) {
			return this.sounds.get(name)!;
		}

		const def = soundDefs[name];
		if (!def) {
			console.warn(`[SoundManager] Unknown sound: ${name}`);
			return null;
		}

		try {
			const howl = new Howl({
				src: def.src,
				volume: (def.volume ?? 1.0) * this.volume,
				preload: true,
				onloaderror: (_id: number, err: unknown) => {
					console.warn(`[SoundManager] Failed to load ${name}:`, err);
				}
			});
			this.sounds.set(name, howl);
			return howl;
		} catch (e) {
			console.warn(`[SoundManager] Error creating sound ${name}:`, e);
			return null;
		}
	}

	/**
	 * Play a sound effect by name
	 */
	play(name: string): void {
		if (this.muted) return;

		const sound = this.ensureLoaded(name);
		if (sound) {
			sound.play();
		}
	}

	/**
	 * Stop a specific sound
	 */
	stop(name: string): void {
		const sound = this.sounds.get(name);
		if (sound) {
			sound.stop();
		}
	}

	/**
	 * Stop all sounds
	 */
	stopAll(): void {
		this.sounds.forEach(sound => sound.stop());
	}

	/**
	 * Toggle mute state
	 */
	toggleMute(): boolean {
		this.muted = !this.muted;
		return this.muted;
	}

	/**
	 * Set mute state
	 */
	setMuted(muted: boolean): void {
		this.muted = muted;
	}

	/**
	 * Get mute state
	 */
	isMuted(): boolean {
		return this.muted;
	}

	/**
	 * Set master volume (0.0 - 1.0)
	 */
	setVolume(vol: number): void {
		this.volume = Math.max(0, Math.min(1, vol));
		// Update all loaded sounds
		this.sounds.forEach((howl, name) => {
			const def = soundDefs[name];
			howl.volume((def?.volume ?? 1.0) * this.volume);
		});
	}

	/**
	 * Get master volume
	 */
	getVolume(): number {
		return this.volume;
	}
}

/**
 * BGMManager - Background music manager with loop gap and category support
 * 
 * Features:
 *   - Uniform volume across all tracks
 *   - 10-second gap between loops
 *   - Category-based organization (main_menu, battle_*, custom)
 *   - Random track selection within categories
 *   - Proper stop/start to prevent overlap
 */
class BGMManager {
	private currentHowl: Howl | null = null;
	private currentCategory: string | null = null;
	private currentTrackIndex: number = 0;
	private muted: boolean = false;
	private volume: number = 1.0;
	private fadeTime: number = 1000; // ms
	private loopTimeoutId: ReturnType<typeof setTimeout> | null = null;
	private isPlaying: boolean = false;

	/**
	 * Get a random track index from a category
	 */
	private getRandomTrackIndex(category: string): number {
		const tracks = bgmRegistry[category];
		if (!tracks || tracks.length === 0) return 0;
		return Math.floor(Math.random() * tracks.length);
	}

	/**
	 * Create a Howl instance for a specific track
	 */
	private createHowl(src: string): Howl {
		return new Howl({
			src: [src],
			volume: BGM_VOLUME * this.volume,
			loop: false, // We handle looping manually with gap
			preload: true,
			onend: () => this.onTrackEnd(),
			onloaderror: (_id: number, err: unknown) => {
				console.warn(`[BGMManager] Failed to load ${src}:`, err);
			}
		});
	}

	/**
	 * Called when a track finishes playing
	 */
	private onTrackEnd(): void {
		if (!this.isPlaying || this.muted) return;
		
		// Schedule next loop after gap
		this.loopTimeoutId = setTimeout(() => {
			if (this.isPlaying && this.currentCategory) {
				this.playNextInCategory();
			}
		}, LOOP_GAP_MS);
	}

	/**
	 * Play the next track in the current category (or same track if only one)
	 */
	private playNextInCategory(): void {
		if (!this.currentCategory || this.muted) return;
		
		const tracks = bgmRegistry[this.currentCategory];
		if (!tracks || tracks.length === 0) return;

		// If multiple tracks, pick a different one randomly; otherwise replay same
		if (tracks.length > 1) {
			let newIndex = this.currentTrackIndex;
			while (newIndex === this.currentTrackIndex) {
				newIndex = this.getRandomTrackIndex(this.currentCategory);
			}
			this.currentTrackIndex = newIndex;
		}

		const src = tracks[this.currentTrackIndex];
		
		// Stop old howl completely
		if (this.currentHowl) {
			this.currentHowl.stop();
			this.currentHowl.unload();
		}

		// Create and play new howl
		this.currentHowl = this.createHowl(src);
		this.currentHowl.volume(0);
		this.currentHowl.play();
		this.currentHowl.fade(0, BGM_VOLUME * this.volume, this.fadeTime);
	}

	/**
	 * Play BGM by category name (e.g., 'main_menu', 'battle_ai', 'custom')
	 */
	play(category: string): void {
		// If already playing this category, do nothing
		if (this.currentCategory === category && this.isPlaying) {
			return;
		}

		// Validate category exists
		if (!bgmRegistry[category]) {
			console.warn(`[BGMManager] Unknown category: ${category}`);
			return;
		}

		// Check if category has tracks
		const tracks = bgmRegistry[category];
		if (!tracks || tracks.length === 0) {
			console.warn(`[BGMManager] No tracks in category: ${category}`);
			return;
		}

		// Stop any existing playback completely
		this.stopInternal(false);

		// Set up new category
		this.currentCategory = category;
		this.currentTrackIndex = this.getRandomTrackIndex(category);
		this.isPlaying = true;

		if (this.muted) {
			console.log(`[BGMManager] Not playing ${category} - muted`);
			return;
		}

		// Start playing
		const src = tracks[this.currentTrackIndex];
		
		console.log(`[BGMManager] Playing ${category}: ${src}`);
		
		this.currentHowl = this.createHowl(src);
		this.currentHowl.volume(0);
		this.currentHowl.play();
		this.currentHowl.fade(0, BGM_VOLUME * this.volume, this.fadeTime);
	}

	/**
	 * Play battle music based on topic
	 */
	playForTopic(topic: string): void {
		const normalizedTopic = topic.toLowerCase();
		
		// Check if it's a built-in topic with dedicated music
		if (BUILT_IN_TOPICS.includes(normalizedTopic)) {
			this.play(`battle_${normalizedTopic}`);
		} else {
			// Custom topic - use custom category
			this.play('custom');
		}
	}

	/**
	 * Internal stop (with option to clear state)
	 */
	private stopInternal(clearState: boolean): void {
		// Clear any pending loop timeout
		if (this.loopTimeoutId) {
			clearTimeout(this.loopTimeoutId);
			this.loopTimeoutId = null;
		}

		// Stop and unload current howl
		if (this.currentHowl) {
			this.currentHowl.fade(this.currentHowl.volume(), 0, this.fadeTime);
			const howlToUnload = this.currentHowl;
			setTimeout(() => {
				howlToUnload.stop();
				howlToUnload.unload();
			}, this.fadeTime);
			this.currentHowl = null;
		}

		this.isPlaying = false;
		
		if (clearState) {
			this.currentCategory = null;
			this.currentTrackIndex = 0;
		}
	}

	/**
	 * Stop current BGM with fade out
	 */
	stop(): void {
		this.stopInternal(true);
	}

	/**
	 * Pause current BGM
	 */
	pause(): void {
		if (this.loopTimeoutId) {
			clearTimeout(this.loopTimeoutId);
			this.loopTimeoutId = null;
		}
		if (this.currentHowl) {
			this.currentHowl.pause();
		}
		this.isPlaying = false;
	}

	/**
	 * Resume paused BGM
	 */
	resume(): void {
		if (this.muted) return;
		if (this.currentHowl) {
			this.currentHowl.play();
			this.isPlaying = true;
		}
	}

	/**
	 * Toggle mute state
	 */
	toggleMute(): boolean {
		this.muted = !this.muted;
		if (this.muted) {
			this.pause();
		} else {
			this.resume();
		}
		return this.muted;
	}

	/**
	 * Set mute state
	 */
	setMuted(muted: boolean): void {
		this.muted = muted;
		if (muted) {
			this.pause();
		} else {
			this.resume();
		}
	}

	/**
	 * Get mute state
	 */
	isMuted(): boolean {
		return this.muted;
	}

	/**
	 * Set master volume (0.0 - 1.0)
	 */
	setVolume(vol: number): void {
		this.volume = Math.max(0, Math.min(1, vol));
		if (this.currentHowl) {
			this.currentHowl.volume(BGM_VOLUME * this.volume);
		}
	}

	/**
	 * Get master volume
	 */
	getVolume(): number {
		return this.volume;
	}

	/**
	 * Get currently playing category
	 */
	getCurrentCategory(): string | null {
		return this.currentCategory;
	}
}

// Singleton instances
export const sounds = new SoundManager();
export const bgm = new BGMManager();
