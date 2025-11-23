import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [tailwindcss(), sveltekit()],
	server: {
		host: true,
		port: 5173,
		proxy: {
			'/api': {
				target: 'http://127.0.0.1:7070',
				changeOrigin: true,
				secure: false
			},
			'/health': {
				target: 'http://127.0.0.1:7070',
				changeOrigin: true,
				secure: false
			}
		}
	}
});
