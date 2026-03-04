/** Parse time string (H:MM:SS, MM:SS, M:SS, SS, or SS.t for tenths) to centis. Returns undefined if invalid. */
export function parseTimeToCentis(str: string): number | undefined {
  const s = str.trim();
  if (s === '' || s === '--:--') return undefined;
  const parts = s.split(':').map(p => p.trim());
  if (parts.some(p => p === '')) return undefined;
  const lastPart = parts[parts.length - 1];
  const lastNum = parseFloat(lastPart);
  if (isNaN(lastNum) || lastNum < 0) return undefined;
  const wholeParts = parts.slice(0, -1).map(p => parseInt(p, 10));
  if (wholeParts.some(n => isNaN(n) || n < 0)) return undefined;
  let seconds = 0;
  if (parts.length === 1) {
    seconds = lastNum;
  } else if (parts.length === 2) {
    seconds = parseInt(parts[0], 10) * 60 + lastNum;
  } else if (parts.length === 3) {
    seconds = parseInt(parts[0], 10) * 3600 + parseInt(parts[1], 10) * 60 + lastNum;
  } else {
    return undefined;
  }
  return Math.round(seconds * 100);
}

const pad2 = (num: number): string => (num < 10 ? '0' : '') + num;

/** Format centis as H:MM:SS, MM:SS, or SS.t for display in input (tenths when non-integer). */
export function formatClockFromCentis(centis: number): string {
  if (centis <= 0) return '0:00:00';
  const date = new Date(centis * 10),
    hours = Math.floor(centis / 360000),
    mins = date.getUTCMinutes(),
    secs = date.getUTCSeconds(),
    tenths = Math.round((centis % 100) / 10);
  const secStr = tenths > 0 ? `${pad2(secs)}.${tenths}` : pad2(secs);
  if (hours > 0) {
    return `${hours}:${pad2(mins)}:${secStr}`;
  }
  if (mins > 0) {
    return `${mins}:${secStr}`;
  }
  return tenths > 0 ? `${secs}.${tenths}` : `${secs}`;
}
