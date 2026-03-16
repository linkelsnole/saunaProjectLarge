import { createContext, useContext, useState, useCallback, type ReactNode } from "react";

type SettingsSheetContextValue = {
  open: boolean;
  setOpen: (v: boolean) => void;
  openSettings: () => void;
};

const SettingsSheetContext = createContext<SettingsSheetContextValue | null>(null);

export const SettingsSheetProvider = ({ children }: { children: ReactNode }) => {
  const [open, setOpen] = useState(false);
  const openSettings = useCallback(() => setOpen(true), []);
  return (
    <SettingsSheetContext.Provider value={{ open, setOpen, openSettings }}>
      {children}
    </SettingsSheetContext.Provider>
  );
};

export const useSettingsSheet = () => {
  const ctx = useContext(SettingsSheetContext);
  return ctx;
};
