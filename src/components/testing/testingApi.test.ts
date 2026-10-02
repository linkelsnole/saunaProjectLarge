import { describe, expect, it, vi } from 'vitest';
import { percentParameter, testingApi } from './testingApi';
import { fetchWithAuth } from '@/api/authApi';
vi.mock('@/api/authApi', () => ({ fetchWithAuth: vi.fn() }));
describe('test command parameters', () => {
  it('scales only recuperator speed', () => {
    expect(percentParameter(5, '50,25')).toBe(5025);
    expect(percentParameter(7, '50')).toBe(50);
    expect(percentParameter(14, '100')).toBe(100);
  });
  it.each(['', ' ', '-1', '101', 'NaN', 'Infinity'])('rejects invalid percentage %s', value => {
    expect(() => percentParameter(5, value)).toThrow();
  });
  it('rejects fractional integer parameters', () => {
    expect(() => percentParameter(7, '50.5')).toThrow();
  });
  it('sends one complete operation and surfaces API failure without retry', async () => {
    vi.mocked(fetchWithAuth).mockResolvedValueOnce(new Response(JSON.stringify({ error: 'offline' }), { status: 502 }));
    await expect(testingApi.execute({ command: 12, index: 3, value: 1 })).rejects.toThrow('offline');
    expect(fetchWithAuth).toHaveBeenCalledTimes(1);
    expect(fetchWithAuth).toHaveBeenCalledWith('/api/v1/testing/commands', { method: 'POST', body: JSON.stringify({ command: 12, index: 3, value: 1 }) });
  });
});
