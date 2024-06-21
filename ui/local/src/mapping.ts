import { Mapping, Point } from './types';

const EXTERNAL_DOMAIN_DELTA = 10;

export function addPoint(m: Mapping, add: Point) {
  normalize(m);
  const qX = quantize(add.x, m);
  if (!Array.isArray(m.data)) m.data = [];
  const i = m.data.findIndex(p => p.x >= qX);
  if (i >= 0) {
    if (m.data[i].x === qX) m.data[i] = { x: qX, y: add.y };
    else m.data.splice(i, 0, { x: qX, y: add.y });
  } else m.data.push({ x: qX, y: add.y });
}

export function asData(m: Mapping) {
  const pts = Array.isArray(m.data) ? m.data.slice() : [];
  const xs = domain(m);
  const defaultVal = typeof m.data === 'number' ? m.data : (m.range.max - m.range.min) / 2;
  if (pts.length === 0)
    return [
      { x: xs.min - EXTERNAL_DOMAIN_DELTA, y: defaultVal },
      { x: xs.max + EXTERNAL_DOMAIN_DELTA, y: defaultVal },
    ];
  pts.unshift({ x: xs.min - EXTERNAL_DOMAIN_DELTA, y: pts[0].y });
  pts.push({ x: xs.max + EXTERNAL_DOMAIN_DELTA, y: pts[pts.length - 1].y });
  return pts;
}

export function quantize(x: number, m: Mapping) {
  const qu = m.from === 'moves' ? 1 : 0.01;
  return Math.round(x / qu) * qu;
}

export function normalize(m: Mapping) {
  if (!Array.isArray(m.data)) return;
  const newData = m.data.reduce((acc: Point[], p) => {
    const x = quantize(p.x, m);
    const i = acc.findIndex(q => q.x === x);
    if (i >= 0) acc[i] = { x, y: p.y };
    else acc.push({ x, y: p.y });
    return acc;
  }, []);
  newData.sort((a, b) => a.x - b.x);
  m.data = newData;
}

export function interpolate(m: Mapping, x: number) {
  if (typeof m.data === 'number') return m.data;
  if (m.data.length === 0) return undefined;
  if (m.data.length === 1) return m.data[0].y;
  for (let i = 1; i < m.data.length - 1; i++) {
    const p1 = m.data[i];
    const p2 = m.data[i + 1];
    if (p1.x <= x && x <= p2.x) {
      const m = (p2.y - p1.y) / (p2.x - p1.x);
      return p1.y + m * (x - p1.x);
    }
  }
  return m.data[m.data.length - 1].y;
}

export function domain(m: Mapping) {
  if (m.from === 'moves') return { min: 1, max: 60 };
  else return { min: -1, max: 1 };
}
