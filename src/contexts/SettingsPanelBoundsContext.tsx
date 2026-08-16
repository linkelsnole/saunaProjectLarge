import { createContext, useContext, useRef, type ReactNode } from "react";

type SettingsPanelBoundsContextValue = {
	boundsRef: React.RefObject<HTMLDivElement | null>;
};

const SettingsPanelBoundsContext = createContext<SettingsPanelBoundsContextValue | null>(null);

export function SettingsPanelBoundsProvider({ children }: { children: ReactNode }) {
	const boundsRef = useRef<HTMLDivElement>(null);
	return (
		<SettingsPanelBoundsContext.Provider value={{ boundsRef }}>
			<div
				ref={boundsRef}
				className="relative flex min-h-0 flex-1 flex-col overflow-hidden rounded-2xl"
			>
				{children}
			</div>
		</SettingsPanelBoundsContext.Provider>
	);
}

export function useSettingsPanelBounds() {
	const ctx = useContext(SettingsPanelBoundsContext);
	if (!ctx) {
		throw new Error("useSettingsPanelBounds must be used within SettingsPanelBoundsProvider");
	}
	return ctx;
}
