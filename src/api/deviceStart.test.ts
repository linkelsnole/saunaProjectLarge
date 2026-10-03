import { afterEach, expect, it, vi } from 'vitest';
import { fetchWithAuth } from './authApi';
import { toggleRecuperator } from './deviceApi';
vi.mock('./authApi', () => ({ fetchWithAuth: vi.fn() }));
afterEach(() => vi.resetAllMocks());
it('shows the server interlock message without retrying start', async () => {
    vi.mocked(fetchWithAuth).mockResolvedValue(new Response(JSON.stringify({error: 'Сначала выключите режим тестирования'}), {status:409}));
    await expect(toggleRecuperator(true)).rejects.toThrow('Сначала выключите режим тестирования');
    expect(fetchWithAuth).toHaveBeenCalledTimes(1);
});
it('does not mask ordinary start errors with a testing message', async () => {
    vi.mocked(fetchWithAuth).mockResolvedValue(new Response('', {status:500}));
    await expect(toggleRecuperator(true)).rejects.toThrow('Failed to toggle recuperator');
});
