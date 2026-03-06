/** Parse time string (H:MM:SS, MM:SS, M:SS, SS, optionally with tenths/hundredths) to centis. Returns undefined if invalid. */
export function parseTimeToCentis(str: string): number | undefined {
  const s = str.trim();
  if (s === '' || s === '--:--') return undefined;
  const parts = s.split(':').map(p => p.trim());
  if (parts.some(p => p === '')) return undefined;
  const lastPart = parts[parts.length - 1];
  if (lastPart.endsWith('.')) return undefined;
  const lastNum = parseFloat(lastPart);
  if (isNaN(lastNum) || lastNum < 0) return undefined;
  const wholeParts = parts.slice(0, -1).map(p => parseInt(p, 10));
  if (wholeParts.some(n => isNaN(n) || n < 0)) return undefined;
  // Clock semantics: minutes and seconds segments must be 0–59 (reject e.g. 1:60 or 0:90)
  const secSegment = Math.floor(lastNum);
  if (secSegment >= 60) return undefined;
  if (parts.length === 2 && parseInt(parts[0], 10) >= 60) return undefined;
  if (parts.length === 3 && parseInt(parts[1], 10) >= 60) return undefined;
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

/** Format centis exactly for the edit input so round-trip matches (tenths or hundredths as stored). */
export function formatClockFromCentis(centis: number): string {
  if (centis <= 0) return '0:00:00';
  const date = new Date(centis * 10),
    hours = Math.floor(centis / 360000),
    mins = date.getUTCMinutes(),
    secs = date.getUTCSeconds(),
    remainder = centis % 100;
  const secStr =
    remainder === 0
      ? pad2(secs)
      : remainder % 10 === 0
        ? `${pad2(secs)}.${remainder / 10}`
        : `${pad2(secs)}.${remainder < 10 ? '0' + remainder : remainder}`;
  if (hours > 0) {
    return `${hours}:${pad2(mins)}:${secStr}`;
  }
  if (mins > 0) {
    return `${mins}:${secStr}`;
  }
  return remainder > 0
    ? remainder % 10 === 0
      ? `${secs}.${remainder / 10}`
      : `${secs}.${remainder < 10 ? '0' + remainder : remainder}`
    : `${secs}`;
}
