import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

const STORAGE_KEY = 'recup_type';

export type RecuperatorTypeValue = 1 | 2;

type RecuperatorTypeContextValue = {
  type: RecuperatorTypeValue;
  setType: (next: RecuperatorTypeValue) => void;
};

const RecuperatorTypeContext = createContext<RecuperatorTypeContextValue | null>(null);

export function RecuperatorTypeProvider({ children }: { children: ReactNode }) {
  const [type, setTypeState] = useState<RecuperatorTypeValue>(() => {
    const raw = Number(localStorage.getItem(STORAGE_KEY));
    return raw === 2 ? 2 : 1;
  });

  const setType = useCallback((next: RecuperatorTypeValue) => {
    setTypeState(next);
    localStorage.setItem(STORAGE_KEY, String(next));
  }, []);

  const value = useMemo(() => ({ type, setType }), [type, setType]);

  return (
    <RecuperatorTypeContext.Provider value={value}>
      {children}
    </RecuperatorTypeContext.Provider>
  );
}

export function useRecuperatorType() {
  const ctx = useContext(RecuperatorTypeContext);
  if (!ctx) {
    throw new Error('useRecuperatorType must be used within RecuperatorTypeProvider');
  }
  return { type: ctx.type, changeType: ctx.setType };
}
