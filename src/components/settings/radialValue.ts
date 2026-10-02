/** Map the 270-degree dial to a stepped value; the bottom gap clamps to an endpoint. */
export function radialValue(x: number, y: number, max: number, step: number) {
  let degrees = (Math.atan2(y, x) * 180 / Math.PI - 135 + 360) % 360;
  if (degrees > 270) degrees = degrees < 315 ? 270 : 0;
  return Math.max(0, Math.min(max, Math.round(degrees / 270 * max / step) * step));
}
