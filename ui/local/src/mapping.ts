import { Mapping, Point } from './types';

export function updateMapping(m: Mapping, op: { add: Point } | { remove: number } | 'clear' | 'normalize') {
  if (op === 'normalize') normalize(m);
  else if (op === 'clear') m.data = [];
  else if ('add' in op) {
    normalize(m);
    const addX = quantize(op.add.x, m);
    const i = m.data.findIndex(p => p.x >= addX);
    if (i >= 0) {
      if (m.data[i].x === addX) m.data[i] = { x: addX, y: op.add.y };
      else m.data.splice(i, 0, { x: addX, y: op.add.y });
    } else m.data.push({ x: addX, y: op.add.y });
  } else {
    const removeX = quantize(m.data[op.remove].x, m);
    const i = m.data.findIndex(p => p.x === removeX);
    if (i >= 0) m.data.splice(i, 1);
  }
}

export function asChartJsData(m: Mapping) {
  const pts = m.data.slice() as any[];
  const xs = domain(m);
  const yMid = (m.scale.maxY - m.scale.minY) / 2;
  if (pts.length === 0)
    return [
      { x: xs.min, y: yMid, radius: 0 },
      { x: xs.max, y: yMid, radius: 0 },
    ];
  if (pts[0].x > xs.min) pts.unshift({ x: xs.min, y: pts[0].y, radius: 0 });
  if (pts[pts.length - 1].x < xs.max) pts.push({ x: xs.max, y: pts[pts.length - 1].y, radius: 0 });
  return pts;
}

export function quantize(x: number, m: Mapping) {
  const qu = m.by === 'moves' ? 1 : 0.01;
  return Math.round(x / qu) * qu;
}

export function normalize(m: Mapping) {
  const newData = m.data.reduce((acc: Point[], p) => {
    const x = quantize(p.x, m);
    const i = acc.findIndex(q => q.x === x);
    if (i >= 0) acc[i] = { x, y: p.y };
    else acc.push({ x, y: p.y });
    return acc;
  }, []);
  newData.sort((a, b) => a.x - b.x);
  m.data = newData;
  return m.data.slice();
}

export function interpolate(curve: Mapping, x: number) {
  if (curve.data.length === 0) return undefined;
  if (curve.data.length === 1) return curve.data[0].y;
  for (let i = 1; i < curve.data.length; i++) {
    const p1 = curve.data[i];
    const p2 = curve.data[i + 1];
    if (p1.x <= x && x <= p2.x) {
      const m = (p2.y - p1.y) / (p2.x - p1.x);
      return p1.y + m * (x - p1.x);
    }
  }
  return curve.data[curve.data.length - 1].y;
}

export function domain(m: Mapping) {
  if (m.by === 'moves') return { min: 1, max: 60 };
  else return { min: -1, max: 1 };
}
