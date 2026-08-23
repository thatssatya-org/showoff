import react from "@astrojs/react";
import sitemap from "@astrojs/sitemap";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "astro/config";

const configuredSite = process.env.PUBLIC_SITE_URL;
const localApiOrigin = process.env.PORTFOLIO_API_ORIGIN;

const localApiProxy = localApiOrigin
  ? {
      "/api": {
        target: localApiOrigin,
        changeOrigin: true
      }
    }
  : undefined;

export default defineConfig({
  site: configuredSite || "https://thatssatya.github.io",
  integrations: [react(), sitemap({ filter: (page) => !new URL(page).pathname.startsWith("/operator/") })],
  vite: {
    plugins: [tailwindcss()],
    server: { proxy: localApiProxy },
    preview: { proxy: localApiProxy },
    test: {
      environment: "node",
      include: ["src/**/*.test.ts"]
    }
  }
});
