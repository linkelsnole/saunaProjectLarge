import { describe, expect, it } from "vitest";
import { zoneTarget } from "./zoneTarget";

const data = {
  temperatureControlModeZone1Code: 2,
  temperatureControlModeZone2Code: 2,
  climateSensors: [
    { id: "T_ZONE1", temperature: 20, humidity: null, label: "1" },
    { id: "T_ZONE2", temperature: 18, humidity: null, label: "2" },
  ],
};
describe("diagram zone targets", () => {
  it.each([true, false])("shares OV target regardless of screen mode %s", screenMode => {
    const snapshot = { ...data, screenMode };
    expect(zoneTarget(snapshot, "T_ZONE1")).toBe(20);
    expect(zoneTarget(snapshot, "T_ZONE2")).toBe(20);
  });
  it.each([[1, 1], [3, 3], [2, 1], [1, 2]])("keeps independent targets for modes %s/%s", (one, two) => {
    const snapshot = { ...data, temperatureControlModeZone1Code: one, temperatureControlModeZone2Code: two };
    expect(zoneTarget(snapshot, "T_ZONE1")).toBe(20);
    expect(zoneTarget(snapshot, "T_ZONE2")).toBe(18);
  });
  it("returns to independent targets when actual mode changes", () => {
    expect(zoneTarget(data, "T_ZONE2")).toBe(20);
    expect(zoneTarget({ ...data, temperatureControlModeZone2Code: 1 }, "T_ZONE2")).toBe(18);
  });
  it("does not substitute zone 2 when shared target is missing", () => {
    expect(zoneTarget({ ...data, climateSensors: data.climateSensors.slice(1) }, "T_ZONE2")).toBeNull();
    expect(zoneTarget(undefined, "T_ZONE1")).toBeNull();
  });
  it("preserves a zero target", () => {
    expect(zoneTarget({ ...data, climateSensors: [{ ...data.climateSensors[0], temperature: 0 }] }, "T_ZONE2")).toBe(0);
  });
});
