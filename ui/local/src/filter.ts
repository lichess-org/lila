import type { Point as CjsPoint } from 'chart.js';
import { clamp } from 'common/algo';

export type Point = [number, number];

export interface Filter {
  readonly range: { min: number; max: number };
  by: 'move' | 'score' | 'time';
  data: Point[];
}

export type Filters = { [key: string]: Filter };

const DOMAIN_CAP_DELTA = 10;

export function addPoint(m: Filter, add: Point): void {
  normalize(m);
  const qX = quantizeX(add[0], m);
  const data = m.data;
  const i = data.findIndex(p => p[0] >= qX);
  if (i >= 0) {
    if (data[i][0] === qX) data[i] = [qX, add[1]];
    else data.splice(i, 0, [qX, add[1]]);
  } else data.push([qX, add[1]]);
}

export function asData(m: Filter): CjsPoint[] {
  const pts = m.data.slice();
  const xs = domain(m);
  const defaultVal = (m.range.max - m.range.min) / 2;
  if (pts.length === 0)
    return [
      { x: xs.min - DOMAIN_CAP_DELTA, y: defaultVal },
      { x: xs.max + DOMAIN_CAP_DELTA, y: defaultVal },
    ];
  pts.unshift([xs.min - DOMAIN_CAP_DELTA, pts[0][1]]);
  pts.push([xs.max + DOMAIN_CAP_DELTA, pts[pts.length - 1][1]]);
  return pts.map(p => ({ x: p[0], y: p[1] }));
}

export function quantizeX(x: number, m: Filter): number {
  const qu = m.by === 'score' ? 0.01 : 1;
  return Math.round(x / qu) * qu;
}

export function normalize(m: Filter): void {
  const newData = m.data.reduce((acc: Point[], p) => {
    const x = quantizeX(p[0], m);
    const i = acc.findIndex(q => q[0] === x);
    if (i >= 0) acc[i] = [x, p[1]];
    else acc.push([x, p[1]]);
    return acc;
  }, []);
  newData.sort((a, b) => a[0] - b[0]);
  m.data = newData;
}

export function interpolate(m: Filter, x: number): number {
  const constrain = (x: number) => clamp(x, m.range);
  const to = m.data;

  if (to.length === 0) return (m.range.max + m.range.min) / 2;
  if (to.length === 1 || x <= to[0][0]) return constrain(to[0][1]);
  for (let i = 0; i < to.length - 1; i++) {
    const p1 = to[i];
    const p2 = to[i + 1];
    if (p1[0] <= x && x <= p2[0]) {
      const m = (p2[1] - p1[1]) / (p2[0] - p1[0]);
      return constrain(p1[1] + m * (x - p1[0]));
    }
  }
  return to[to.length - 1][1];
}

export function domain(m: Filter): { min: number; max: number } {
  switch (m.by) {
    case 'move':
      return { min: 1, max: 60 };
    case 'score':
      return { min: 0, max: 1 };
    case 'time': // this is log2 of the thinktime seconds
      return { min: -2, max: 8 };
    default:
      return { min: NaN, max: NaN };
  }
}
