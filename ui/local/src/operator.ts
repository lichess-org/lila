import type { Operator, Point } from './types';
import type { Point as CjsPoint } from 'chart.js';

const DOMAIN_CAP_DELTA = 10;

export function addPoint(m: Operator, add: Point): void {
  normalize(m);
  const qX = quantizeX(add[0], m);
  const data = m.data;
  const i = data.findIndex(p => p[0] >= qX);
  if (i >= 0) {
    if (data[i][0] === qX) data[i] = [qX, add[1]];
    else data.splice(i, 0, [qX, add[1]]);
  } else data.push([qX, add[1]]);
}

export function asData(m: Operator): CjsPoint[] {
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

export function quantizeX(x: number, m: Operator): number {
  const qu = m.from === 'move' ? 1 : 0.01;
  return Math.round(x / qu) * qu;
}

export function normalize(m: Operator) {
  // TODO - not in place
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

export function interpolate(m: Operator, x: number) {
  const to = m.data;
  // if (to.length === 0) return undefined; // TODO explicit default?
  if (to.length === 0) return (m.range.max + m.range.min) / 2;
  if (to.length === 1) return to[0][1];
  for (let i = 1; i < to.length - 1; i++) {
    const p1 = to[i];
    const p2 = to[i + 1];
    if (p1[0] <= x && x <= p2[0]) {
      const m = (p2[1] - p1[1]) / (p2[0] - p1[0]);
      return p1[1] + m * (x - p1[0]);
    }
  }
  return to[to.length - 1][1];
}

export function domain(m: Operator) {
  if (m.from === 'move') return { min: 1, max: 60 };
  else if (m.from === 'score') return { min: -1, max: 1 };
  else return { min: NaN, max: NaN };
}
