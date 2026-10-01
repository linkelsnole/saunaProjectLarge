import type { DashboardData } from "../api/typesApi";

type ZoneData = Pick<DashboardData, "climateSensors" | "temperatureControlModeZone1Code" | "temperatureControlModeZone2Code">;

/** OV uses zone 1's target for both zones, including scheduled operation. */
export function zoneTarget(data: ZoneData | undefined, zone: string): number | null {
  const shared = data?.temperatureControlModeZone1Code === 2 && data?.temperatureControlModeZone2Code === 2;
  const id = shared ? "T_ZONE1" : zone;
  return data?.climateSensors?.find(sensor => sensor.id === id)?.temperature ?? null;
}
