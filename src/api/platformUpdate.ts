export interface PlatformState { buildId: string; updating: boolean }
const buildId = import.meta.env.MODE === 'development' ? '' : (typeof __BUILD_ID__ === 'string' ? __BUILD_ID__ : '');
let blocked = false;
let pending: Promise<PlatformState> | null = null;
export const platformBuildId = buildId;
export const platformIsBlocked = () => blocked;

export function blockForPlatformUpdate() {
  blocked = true;
  window.dispatchEvent(new Event('platform-updating'));
}

export function checkPlatform(): Promise<PlatformState> {
  if (!pending) {
    pending = fetch('/api/v1/platform-state', { cache: 'no-store', signal: AbortSignal.timeout(8000) })
      .then(async response => {
        if (!response.ok) throw new Error('Platform state unavailable');
        const state: PlatformState = await response.json();
        if (typeof state.buildId !== 'string' || typeof state.updating !== 'boolean') {
          throw new Error('Invalid platform state');
        }
        if (state.updating || (buildId && state.buildId && state.buildId !== buildId)) blockForPlatformUpdate();
        return state;
      }).finally(() => { pending = null; });
  }
  return pending;
}

export async function platformFetch(url: string, options: RequestInit): Promise<Response> {
  const mutation = !['GET', 'HEAD', 'OPTIONS'].includes((options.method || 'GET').toUpperCase());
  if (mutation && blocked) throw new Error('Платформа обновляется. Команда не отправлена.');
  const headers = new Headers(options.headers);
  if (buildId) headers.set('X-Panel-Build', buildId);
  const response = await fetch(url, { ...options, headers });
  if (response.headers.get('X-Panel-Update') === 'required') {
    blockForPlatformUpdate();
    throw new Error('Платформа обновляется. Команда не выполнена.');
  }
  return response;
}
