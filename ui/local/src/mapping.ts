import { Mapping, Point } from './types';

const DOMAIN_CAP_DELTA = 10;

export function addPoint(m: Mapping, add: Point) {
  normalize(m);
  const qX = quantize(add.x, m);
  if (!Array.isArray(m.data.to)) m.data.to = [];
  const data = m.data.to;
  const i = data.findIndex(p => p.x >= qX);
  if (i >= 0) {
    if (data[i].x === qX) data[i] = { x: qX, y: add.y };
    else data.splice(i, 0, { x: qX, y: add.y });
  } else data.push({ x: qX, y: add.y });
}

export function asData(m: Mapping) {
  const to = m.data.to;
  const pts = Array.isArray(to) ? to.slice() : [];
  const xs = domain(m);
  const defaultVal = typeof to === 'number' ? to : (m.range.max - m.range.min) / 2;
  if (pts.length === 0)
    return [
      { x: xs.min - DOMAIN_CAP_DELTA, y: defaultVal },
      { x: xs.max + DOMAIN_CAP_DELTA, y: defaultVal },
    ];
  pts.unshift({ x: xs.min - DOMAIN_CAP_DELTA, y: pts[0].y });
  pts.push({ x: xs.max + DOMAIN_CAP_DELTA, y: pts[pts.length - 1].y });
  return pts;
}

export function quantize(x: number, m: Mapping) {
  const qu = m.data.from === 'move' ? 1 : 0.01;
  return Math.round(x / qu) * qu;
}

export function normalize(m: Mapping) {
  if (m.data.from === 'const') return;
  const newData = m.data.to.reduce((acc: Point[], p) => {
    const x = quantize(p.x, m);
    const i = acc.findIndex(q => q.x === x);
    if (i >= 0) acc[i] = { x, y: p.y };
    else acc.push({ x, y: p.y });
    return acc;
  }, []);
  newData.sort((a, b) => a.x - b.x);
  m.data.to = newData;
}

export function interpolate(m: Mapping, x: number) {
  if (m.data.from === 'const') return m.data.to;
  const to = m.data.to;
  if (to.length === 0) return undefined;
  if (to.length === 1) return to[0].y;
  for (let i = 1; i < to.length - 1; i++) {
    const p1 = to[i];
    const p2 = to[i + 1];
    if (p1.x <= x && x <= p2.x) {
      const m = (p2.y - p1.y) / (p2.x - p1.x);
      return p1.y + m * (x - p1.x);
    }
  }
  return to[to.length - 1].y;
}

export function domain(m: Mapping) {
  if (m.data.from === 'move') return { min: 1, max: 60 };
  else if (m.data.from === 'score') return { min: -1, max: 1 };
  else return { min: NaN, max: NaN };
}
