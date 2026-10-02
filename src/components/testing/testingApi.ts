import { fetchWithAuth } from '@/api/authApi';

export interface TestResult { command: number; value?: number }
export interface TestStatus { enabled: boolean }
export interface TestRequest { command: number; index?: number; value?: number }

async function request<T>(path: string, payload?: object): Promise<T> {
  const response = await fetchWithAuth(`/api/v1/testing/${path}`, payload ? {
    method: 'POST', body: JSON.stringify(payload),
  } : {});
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.error || `Ошибка связи (${response.status})`);
  }
  return response.json();
}
export const testingApi = {
  status: () => request<TestStatus>('status'),
  mode: (enabled: boolean) => request<TestStatus>('mode', { enabled }),
  execute: (payload: TestRequest) => request<TestResult>('commands', payload),
};

export function percentParameter(command: number, percent: string): number {
  if (!percent.trim()) throw new Error('invalidPercent');
  const parsed = Number(percent.replace(',', '.'));
  if (!Number.isFinite(parsed) || parsed < 0 || parsed > 100) throw new Error('invalidPercent');
  if (command !== 5 && !Number.isInteger(parsed)) throw new Error('integerPercent');
  return command === 5 ? Math.round(parsed * 100) : parsed;
}
