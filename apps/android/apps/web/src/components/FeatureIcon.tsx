import type { ReactElement } from "react";

/** 功能宫格图标：中性灰描边（颜色纪律——橙只给可交互元素与光源） */
const ICONS: Record<string, ReactElement> = {
	dns: (
		<g>
			<circle cx="12" cy="12" r="9" />
			<ellipse cx="12" cy="12" rx="4" ry="9" />
			<path d="M3.4 12h17.2" />
		</g>
	),
	analytics: (
		<g>
			<path d="M3 20h18" />
			<path d="M5 16.5v-4M9.7 16.5V8M14.3 16.5v-6M19 16.5V4.5" />
		</g>
	),
	tail: (
		<g>
			<path d="M13 2.5 5.5 12.6h4.7L9 21.5l7.5-10.1h-4.7L13 2.5Z" />
		</g>
	),
	storage: (
		<g>
			<ellipse cx="12" cy="5.2" rx="8" ry="2.8" />
			<path d="M4 5.2v13.6c0 1.5 3.6 2.8 8 2.8s8-1.3 8-2.8V5.2" />
			<path d="M4 12c0 1.5 3.6 2.8 8 2.8s8-1.3 8-2.8" />
		</g>
	),
	waf: (
		<g>
			<path d="M12 2.8 4.5 5.6v6.1c0 4.6 3.2 8 7.5 9.5 4.3-1.5 7.5-4.9 7.5-9.5V5.6L12 2.8Z" />
			<path d="M9 12.2l2.1 2.1 4-4.3" />
		</g>
	),
	tunnel: (
		<g>
			<path d="M4 20v-7a8 8 0 0 1 16 0v7" />
			<path d="M9 20v-6.6a3 3 0 0 1 6 0V20" />
		</g>
	),
	widget: (
		<g>
			<rect x="3.2" y="3.2" width="7.6" height="7.6" rx="2.2" />
			<rect x="13.2" y="3.2" width="7.6" height="7.6" rx="2.2" />
			<rect x="3.2" y="13.2" width="7.6" height="7.6" rx="2.2" />
			<circle cx="17" cy="17" r="3.8" />
		</g>
	),
	accounts: (
		<g>
			<circle cx="9" cy="8.5" r="3.4" />
			<path d="M3.2 19.5c0-3.2 2.6-5.2 5.8-5.2s5.8 2 5.8 5.2" />
			<path d="M15.5 5.6a3.4 3.4 0 0 1 0 5.9M17.6 14.6c2 .8 3.2 2.6 3.2 4.9" />
		</g>
	),
};

export default function FeatureIcon({ name }: { name: string }) {
	return (
		<svg
			width="24"
			height="24"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			strokeWidth="1.6"
			strokeLinecap="round"
			strokeLinejoin="round"
			aria-hidden="true"
		>
			{ICONS[name]}
		</svg>
	);
}
