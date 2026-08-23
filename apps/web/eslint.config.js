import js from "@eslint/js";
import astro from "eslint-plugin-astro";
import globals from "globals";
import tseslint from "typescript-eslint";

export default [
  {
    ignores: ["dist", ".astro", "node_modules", "src/lib/api/openapi.generated.ts"]
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...astro.configs.recommended,
  {
    files: ["**/*.{js,mjs,ts,tsx,astro}"],
    languageOptions: {
      globals: { ...globals.browser, ...globals.node }
    },
    rules: {
      "@typescript-eslint/consistent-type-imports": "error"
    }
  }
];
