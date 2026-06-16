import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
	plugins: [react(), tailwindcss()],
	server: {
		// dev server proxies relative /api calls to the local gateway so the same
		// relative paths work both in `npm run dev` and behind nginx in production.
		proxy: {
			"/api": "http://localhost:8080",
		},
	},
});
