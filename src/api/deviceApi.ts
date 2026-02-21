import { fetchWithAuth } from './authApi';
import type { UpdateRecuperatorSettingsRequest } from './typesApi';

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
