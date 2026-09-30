import { fetchWithAuth } from './authApi';

export interface PanelUpdateStatus {
  enabled: boolean;
  status?: 'waiting' | 'checking' | 'available' | 'queued' | 'installing' | 'success' | 'failed';
  version?: string;
  commit?: string;
  ready?: boolean;
  installedVersion?: string;
}

export async function panelUpdateStatus(): Promise<PanelUpdateStatus> {
  const response = await fetchWithAuth('/api/v1/panel-updates', { cache: 'no-store', signal: AbortSignal.timeout(8000) });
  if (!response.ok) throw new Error(`Update status unavailable (${response.status})`);
  return response.json();
}

export async function requestPanelUpdate(version: string): Promise<PanelUpdateStatus> {
  const response = await fetchWithAuth('/api/v1/panel-updates', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ version }), signal: AbortSignal.timeout(8000),
  });
  if (!response.ok) throw new Error(`Update request rejected (${response.status})`);
  return response.json();
}

export function updateOutcome(state: PanelUpdateStatus, requested: string): 'waiting' | 'reload' | 'failed' {
  if (!requested || state.version !== requested) return 'waiting';
  if (state.status === 'success') return 'reload';
  if (state.status === 'failed') return 'failed';
  return 'waiting';
}

export interface PanelVersion {
  version: string; commit?: string; description: string; installedAt: number;
  databaseSavedAt?: number; current: boolean; restorable: boolean;
}
export async function panelVersionHistory(): Promise<{enabled:boolean;versions:PanelVersion[]}> {
  const response=await fetchWithAuth('/api/v1/panel-updates/history',{cache:'no-store'});
  if(!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}
export async function restorePanelVersion(version:string): Promise<PanelUpdateStatus> {
  const response=await fetchWithAuth('/api/v1/panel-updates/restore',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({version})});
  if(!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}
