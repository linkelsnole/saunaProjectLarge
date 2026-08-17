import { fetchWithAuth } from './authApi';
import type {
  UpdateRecuperatorSettingsRequest,
  UpgradeStatusData,
  ServerStateData,
  UserActionEntry,
  LogSettingsData,
} from './typesApi';

interface UpdateManualSettingsRequest {
  supplySpeed?: number;
  boostPercent?: number;
}

interface UpdateTempSettingsRequest {
  targetZone1?: number;
  targetZone2?: number;
  waterWarnThreshold?: number;
  waterFailThreshold?: number;
  inflowWarnThreshold?: number;
  inflowFailThreshold?: number;

  neutralZoneT?: number;
  hotValveMinProc?: number;
  waterMinTNoWork?: number;
  waterMinTWork?: number;

  valveStartupPosition?: number;
  controlAlgorithm?: number;
  tempTrackingInterval?: number;
}


export async function updateManualSettings(data: UpdateManualSettingsRequest): Promise<void> {
  const response = await fetchWithAuth('/api/v1/devices/manual-settings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  
  if (!response.ok) {
    throw new Error('Failed to update manual settings');
  }
}

export async function updateTempSettings(data: UpdateTempSettingsRequest): Promise<void> {
  const response = await fetchWithAuth('/api/v1/devices/temp-settings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  
  if (!response.ok) {
    throw new Error('Failed to update temperature settings');
  }
}

export async function updateRecuperatorSettings(data: UpdateRecuperatorSettingsRequest): Promise<void> {
  const response = await fetchWithAuth('/api/v1/devices/recuperator-settings', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error('Failed to update recuperator settings');
  }
}

export async function toggleRecuperator(enable: boolean): Promise<void> {
  const endpoint = enable ? '/api/v1/devices/recuperator/enable' : '/api/v1/devices/recuperator/disable';
  const response = await fetchWithAuth(endpoint, { method: 'POST' });

  if (!response.ok) {
    throw new Error('Failed to toggle recuperator');
  }
}

const normalizeUpgradeStatus = (data?: Partial<UpgradeStatusData> | null): UpgradeStatusData => ({
  errors: Array.isArray(data?.errors) ? data.errors : [],
  info: Array.isArray(data?.info) ? data.info : [],
});

export async function getServerState(): Promise<ServerStateData> {
  const response = await fetchWithAuth('/api/v1/devices/server-state');
  if (!response.ok) {
    throw new Error('Failed to get server state');
  }
  return response.json();
}

export async function getUpgradeStoredUrl(): Promise<{ url: string }> {
  const response = await fetchWithAuth('/api/v1/devices/upgrade/url');
  if (!response.ok) {
    throw new Error('Failed to get upgrade url');
  }
  return response.json();
}

export async function setUpgradeStoredUrl(url: string): Promise<{ url: string }> {
  const response = await fetchWithAuth(`/api/v1/devices/upgrade/url?url=${encodeURIComponent(url)}`, {
    method: 'POST',
  });
  if (!response.ok) {
    throw new Error('Failed to set upgrade url');
  }
  return response.json();
}

async function postUpgrade(path: string, params: Record<string, string>): Promise<UpgradeStatusData> {
  const query = new URLSearchParams(params).toString();
  const response = await fetchWithAuth(`${path}?${query}`, { method: 'POST' });
  if (!response.ok) {
    throw new Error('Upgrade request failed');
  }
  return normalizeUpgradeStatus(await response.json());
}

export async function getUpgradeLoadStatus(url?: string): Promise<UpgradeStatusData> {
  const params: Record<string, string> = { force: 'false' };
  if (url?.trim()) params.url = url.trim();
  return postUpgrade('/api/v1/devices/upgrade/load', params);
}

export async function startUpgradeDownload(url?: string): Promise<UpgradeStatusData> {
  const params: Record<string, string> = { force: 'true' };
  if (url?.trim()) params.url = url.trim();
  return postUpgrade('/api/v1/devices/upgrade/load', params);
}

export async function getUpgradeInstallStatus(): Promise<UpgradeStatusData> {
  return postUpgrade('/api/v1/devices/upgrade/execute', { force: 'false' });
}

export async function startUpgradeInstall(): Promise<UpgradeStatusData> {
  return postUpgrade('/api/v1/devices/upgrade/execute', { force: 'true' });
}

export async function getLogSettings(): Promise<LogSettingsData> {
  const response = await fetchWithAuth('/api/v1/logs/settings');
  if (!response.ok) {
    throw new Error('Failed to get log settings');
  }
  return response.json();
}

export async function updateLogSettings(intervalMinutes: number): Promise<LogSettingsData> {
  const response = await fetchWithAuth('/api/v1/logs/settings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ intervalMinutes }),
  });
  if (!response.ok) {
    throw new Error('Failed to update log settings');
  }
  return response.json();
}

export async function getUserActionLogs(minutes: number, type: string): Promise<UserActionEntry[]> {
  const response = await fetchWithAuth(`/api/v1/logs/actions?minutes=${minutes}&type=${encodeURIComponent(type)}`);
  if (!response.ok) {
    throw new Error('Failed to get user action logs');
  }
  const data = await response.json();
  return Array.isArray(data) ? data : [];
}

const formatFileStamp = (ms: number): string => {
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}_${pad(d.getHours())}-${pad(d.getMinutes())}`;
};

export async function downloadReadingsFile(from: number, to: number): Promise<void> {
  const response = await fetchWithAuth(`/api/v1/logs/readings/export?from=${from}&to=${to}`);
  if (!response.ok) {
    throw new Error('Failed to download readings file');
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `readings_${formatFileStamp(from)}_${formatFileStamp(to)}.xlsx`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
