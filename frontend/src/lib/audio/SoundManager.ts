/**
 * SoundManager - Howler.js wrapper for game audio
 * 
 * Usage:
 *   import { sounds } from '$lib/audio/SoundManager';
 *   sounds.play('hit');
 *   sounds.play('miss');
 *   sounds.play('crit');
 */

import { Howl } from 'howler';

// Sound effect definitions
// Replace placeholder paths with actual audio files when available
const soundDefs: Record<string, { src: string[]; volume?: number }> = {
	hit: { src: ['/audio/hit.mp3', '/audio/hit.ogg'], volume: 0.6 },
	miss: { src: ['/audio/miss.mp3', '/audio/miss.ogg'], volume: 0.4 },
	crit: { src: ['/audio/crit.mp3', '/audio/crit.ogg'], volume: 0.8 },
	victory: { src: ['/audio/victory.mp3', '/audio/victory.ogg'], volume: 0.7 },
	defeat: { src: ['/audio/defeat.mp3', '/audio/defeat.ogg'], volume: 0.6 },
	select: { src: ['/audio/select.mp3', '/audio/select.ogg'], volume: 0.3 },
	correct: { src: ['/audio/correct.mp3', '/audio/correct.ogg'], volume: 0.5 },
	wrong: { src: ['/audio/wrong.mp3', '/audio/wrong.ogg'], volume: 0.5 },
};

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

// Singleton instance
export const sounds = new SoundManager();
