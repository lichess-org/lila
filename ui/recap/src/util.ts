export function showDuration(seconds: number): string {
  const d = Math.floor(seconds / (24 * 3600));
  const h = Math.floor((seconds % (24 * 3600)) / 3600);
  const m = Math.floor((seconds % 3600) / 60);

  let result: string[] = [];
  if (d > 0) {
    result.push(`${d} day${d === 1 ? '' : 's'}`);
  }
  if (h > 0) {
    result.push(`${h} hour${h === 1 ? '' : 's'}`);
  }
  if (m > 0 || (d === 0 && h === 0)) {
    result.push(`${m} minute${m === 1 ? '' : 's'}`);
  }

  return result.slice(0, 2).join(' and ');
}
