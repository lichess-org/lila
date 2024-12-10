export function showDuration(seconds: number): string {
  const d = Math.floor(seconds / (24 * 3600));
  const h = Math.floor((seconds % (24 * 3600)) / 3600);
  const m = Math.floor((seconds % 3600) / 60);

  let result: string[] = [];
  if (d > 0) {
    result.push(simplePlural(d, 'day'));
  }
  if (h > 0) {
    result.push(simplePlural(h, 'hour'));
  }
  if (m > 0 || seconds < 60) {
    result.push(simplePlural(m, 'minute'));
  }

  return result.slice(0, 2).join(' and ');
}

function simplePlural(n: number, unit: string): string {
  return `${n} ${unit}${n === 1 ? '' : 's'}`;
}
