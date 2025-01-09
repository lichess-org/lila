// Everything in seconds

export function clockShow(
  lim: number,
  byo: number | undefined,
  inc: number | undefined,
  per: number | undefined,
): string {
  byo = byo || 0;
  inc = inc || 0;
  per = per || 1;

  const base: string = inc ? `${limitString(lim)}+${inc}` : limitString(lim);
  if (byo) {
    const perStr = per > 1 ? `(${per}x)` : '';
    return `${base}|${byo}${perStr}`;
  } else return inc ? base : `${base}|0`;
}

export function clockToPerf(
  lim: number,
  byo: number | undefined,
  inc: number | undefined,
  per: number | undefined,
): Perf {
  const timeSum = clockEstimateSeconds(lim, byo, inc, per);
  if (timeSum < 60) return 'ultraBullet';
  else if (timeSum < 300) return 'bullet';
  else if (timeSum < 599) return 'blitz';
  else if (timeSum < 1500) return 'rapid';
  else return 'classical';
}

export function clockEstimateSeconds(
  lim: number,
  byo: number | undefined,
  inc: number | undefined,
  per: number | undefined,
): number {
  byo = byo || 0;
  inc = inc || 0;
  per = per || 1;

  return lim + 60 * inc + 25 * per * byo;
}

function limitString(lim: number): string {
  if (lim % 60 === 0) return (lim / 60).toString();
  else if (lim === 15) return '¼';
  else if (lim === 30) return '½';
  else if (lim === 45) return '¾';
  else if (lim === 90) return '1.5';
  else return (lim / 60).toFixed(2);
}
