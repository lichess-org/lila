import { RecapPerf } from './interfaces';

export function formatDuration(seconds: number, glue = '<br>'): string {
  const d = Math.floor(seconds / (24 * 3600));
  const h = Math.floor((seconds % (24 * 3600)) / 3600);
  const m = Math.floor((seconds % 3600) / 60);

  let result: string[] = [];
  if (d > 0) {
    result.push(simplePlural(d, 'day'));
  }
  result.push(simplePlural(h, 'hour'));
  result.push(simplePlural(m, 'minute'));

  return result.slice(0, 2).join(glue);
}

function simplePlural(n: number, unit: string): string {
  return `${n} ${unit}${n === 1 ? '' : 's'}`;
}

export const perfNames: Record<Exclude<Perf, 'fromPosition'>, string> = {
  ultraBullet: 'UltraBullet',
  bullet: 'Bullet',
  blitz: 'Blitz',
  rapid: 'Rapid',
  classical: 'Classical',
  correspondence: 'Correspondence',
  racingKings: 'Racing Kings',
  threeCheck: 'Three-check',
  antichess: 'Antichess',
  horde: 'Horde',
  atomic: 'Atomic',
  crazyhouse: 'Crazyhouse',
  chess960: 'Chess960',
  kingOfTheHill: 'King of the Hill',
};

export const perfLabel = (p: RecapPerf): string => {
  const tcs: Perf[] = ['ultraBullet', 'bullet', 'blitz', 'rapid', 'classical', 'correspondence'];
  return tcs.includes(p.key) ? 'favorite time control' : 'favorite variant';
};
