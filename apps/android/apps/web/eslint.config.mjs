import { defineConfig } from "eslint/config";
import nextCoreWebVitals from "eslint-config-next/core-web-vitals";
import nextTypescript from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
	...nextCoreWebVitals,
	...nextTypescript,
	{
		ignores: [".next/**", ".open-next/**", "out/**", "build/**", "next-env.d.ts", "cloudflare-env.d.ts"],
	},
]);

export default eslintConfig;
