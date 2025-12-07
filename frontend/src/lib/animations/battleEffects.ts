/**
 * Battle Effects - GSAP animations for combat 
 */

import gsap from 'gsap';

/**
 * Screen shake effect - shakes the entire container
 * @param element - The container element to shake
 * @param intensity - Shake intensity in pixels (default: 8)
 * @param duration - Total duration in seconds (default: 0.4)
 */
export function screenShake(
	element: HTMLElement | null,
	intensity: number = 8,
	duration: number = 0.4
): gsap.core.Timeline | null {
	if (!element) return null;

	const tl = gsap.timeline();
	const shakes = 8; // More shakes for more impact
	const shakeDuration = duration / shakes;

	for (let i = 0; i < shakes; i++) {
		// Diminishing intensity for more natural feel
		const diminish = 1 - (i / shakes) * 0.5;
		const xOffset = (Math.random() - 0.5) * 2 * intensity * diminish;
		const yOffset = (Math.random() - 0.5) * 2 * intensity * diminish;
		tl.to(element, {
			x: xOffset,
			y: yOffset,
			duration: shakeDuration,
			ease: 'power2.inOut'
		});
	}

	// Return to original position
	tl.to(element, { x: 0, y: 0, duration: shakeDuration / 2, ease: 'power2.out' });

	return tl;
}

/**
 * Knockback effect - pushes sprite in a direction then returns
 * @param element - The sprite element to knock back
 * @param direction - Direction of knockback: 'left' | 'right'
 * @param distance - Knockback distance in pixels (default: 30)
 * @param duration - Duration in seconds (default: 0.3)
 */
export function knockback(
	element: HTMLElement | null,
	direction: 'left' | 'right' = 'left',
	distance: number = 30,
	duration: number = 0.3
): gsap.core.Timeline | null {
	if (!element) return null;

	const xOffset = direction === 'left' ? -distance : distance;
	const tl = gsap.timeline();

	// Impact with slight rotation and scale
	tl.to(element, {
		x: xOffset,
		rotation: direction === 'left' ? -8 : 8,
		scale: 0.95,
		duration: duration / 2,
		ease: 'power3.out'
	}).to(element, {
		x: 0,
		rotation: 0,
		scale: 1.0,
		duration: duration / 2,
		ease: 'elastic.out(1, 0.4)'
	});

	return tl;
}

/**
 * Flash/tint effect - briefly tints element with a color
 * @param element - Element to flash
 * @param color - CSS color for the flash (default: 'red')
 * @param duration - Flash duration in seconds (default: 0.15)
 * @param repeat - Number of flashes (default: 2)
 */
export function flashElement(
	element: HTMLElement | null,
	color: string = 'red',
	duration: number = 0.15,
	repeat: number = 2
): gsap.core.Tween | null {
	if (!element) return null;

	const filterMap: Record<string, string> = {
		red: 'brightness(1.2) sepia(1) saturate(5) hue-rotate(-10deg)',
		white: 'brightness(2) saturate(0)',
		blue: 'brightness(1.2) sepia(1) saturate(3) hue-rotate(180deg)',
		green: 'brightness(1.2) sepia(1) saturate(3) hue-rotate(90deg)',
		yellow: 'brightness(1.3) sepia(1) saturate(4) hue-rotate(30deg)'
	};

	const filter = filterMap[color] || filterMap['red'];

	return gsap.to(element, {
		filter: filter,
		duration: duration,
		ease: 'power1.inOut',
		yoyo: true,
		repeat: repeat * 2 - 1, // yoyo counts as half
		onComplete: () => {
			gsap.set(element, { filter: 'none' });
		}
	});
}

/**
 * Attack lunge animation - sprite lunges forward then returns
 * @param element - The attacking sprite element
 * @param direction - Direction of lunge: 'left' | 'right'
 * @param distance - Lunge distance in pixels (default: 50)
 */
export function attackLunge(
	element: HTMLElement | null,
	direction: 'left' | 'right' = 'right',
	distance: number = 50
): gsap.core.Timeline | null {
	if (!element) return null;

	const xOffset = direction === 'right' ? distance : -distance;

	const tl = gsap.timeline();
	tl.to(element, {
		x: xOffset,
		duration: 0.15,
		ease: 'power2.in'
	}).to(element, {
		x: 0,
		duration: 0.25,
		ease: 'elastic.out(1, 0.5)'
	});

	return tl;
}

/**
 * Victory pose animation - bounce and scale up
 * @param element - The victorious sprite element
 */
export function victoryPose(element: HTMLElement | null): gsap.core.Timeline | null {
	if (!element) return null;

	const tl = gsap.timeline();
	tl.to(element, {
		y: -20,
		scale: 1.1,
		duration: 0.3,
		ease: 'power2.out'
	}).to(element, {
		y: 0,
		scale: 1.0,
		duration: 0.2,
		ease: 'bounce.out'
	});

	return tl;
}

/**
 * Defeat animation - shake and fade/shrink
 * @param element - The defeated sprite element
 */
export function defeatAnimation(element: HTMLElement | null): gsap.core.Timeline | null {
	if (!element) return null;

	const tl = gsap.timeline();

	// Shake violently
	for (let i = 0; i < 4; i++) {
		tl.to(element, {
			x: (i % 2 === 0 ? 1 : -1) * 15,
			rotation: (i % 2 === 0 ? 1 : -1) * 5,
			duration: 0.08
		});
	}

	// Fall and fade
	tl.to(element, {
		y: 30,
		opacity: 0.3,
		scale: 0.8,
		rotation: -10,
		duration: 0.4,
		ease: 'power2.in'
	});

	return tl;
}

/**
 * HP bar damage flash - pulses the HP bar when taking damage
 * @param element - The HP bar element
 */
export function hpBarDamageFlash(element: HTMLElement | null): gsap.core.Tween | null {
	if (!element) return null;

	return gsap.to(element, {
		filter: 'brightness(1.5) saturate(1.5)',
		duration: 0.1,
		yoyo: true,
		repeat: 3,
		onComplete: () => {
			gsap.set(element, { filter: 'none' });
		}
	});
}

/**
 * Enemy attack lunge - enemy lunges toward player
 * @param element - The enemy sprite element
 * @param attackType - Type of attack animation: 'lunge' | 'pulse' | 'spin'
 */
export function enemyAttack(
	element: HTMLElement | null,
	attackType: 'lunge' | 'pulse' | 'spin' = 'lunge'
): gsap.core.Timeline | null {
	if (!element) return null;

	const tl = gsap.timeline();

	switch (attackType) {
		case 'lunge':
			// Powerful forward lunge with windup
			tl.to(element, {
				x: 15,
				scale: 1.05,
				duration: 0.12,
				ease: 'power2.in'
			}).to(element, {
				x: -90,
				scale: 1.1,
				filter: 'brightness(1.3)',
				duration: 0.18,
				ease: 'power3.out'
			}).to(element, {
				x: 0,
				scale: 1.0,
				filter: 'none',
				duration: 0.3,
				ease: 'elastic.out(1, 0.4)'
			});
			break;

		case 'pulse':
			// Intense pulsing energy attack with glow
			tl.to(element, {
				scale: 1.25,
				filter: 'brightness(1.5) saturate(1.5)',
				duration: 0.12,
				ease: 'power2.out'
			}).to(element, {
				scale: 0.9,
				filter: 'brightness(1.8) saturate(2)',
				duration: 0.08,
				ease: 'power2.in'
			}).to(element, {
				scale: 1.15,
				filter: 'brightness(1.5)',
				duration: 0.1,
				ease: 'power2.out'
			}).to(element, {
				scale: 1.0,
				filter: 'none',
				duration: 0.2,
				ease: 'elastic.out(1, 0.3)'
			});
			break;

		case 'spin':
			// Powerful rotation attack with momentum
			tl.to(element, {
				rotation: 15,
				x: 10,
				scale: 1.05,
				duration: 0.1,
				ease: 'power2.in'
			}).to(element, {
				rotation: -25,
				x: -50,
				scale: 1.12,
				filter: 'brightness(1.3)',
				duration: 0.15,
				ease: 'power3.out'
			}).to(element, {
				rotation: 0,
				x: 0,
				scale: 1.0,
				filter: 'none',
				duration: 0.25,
				ease: 'back.out(2)'
			});
			break;
	}

	return tl;
}
