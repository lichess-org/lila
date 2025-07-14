import { clamp, quantize } from '../algo';

export type Point = [number, number];
export type FilterFacet = keyof typeof filterData;
export type FilterBy = 'max' | 'min' | 'avg';
export type FilterValue = { [key in FilterFacet]?: number };
export type Filters = { [key: string]: Filter };
export interface Filter {
  readonly range: { min: number; max: number };
  by: FilterBy;
  move?: Point[];
  score?: Point[];
  time?: Point[];
}
export const filterData = {
  move: { domain: { min: 1, max: 60 }, quantum: 1 },
  score: { domain: { min: 0, max: 1 }, quantum: 0.01 },
  time: { domain: { min: -2, max: 8 }, quantum: 1 },
} as const;
export const filterFacets = Object.keys(filterData) as FilterFacet[];
export const filterBys: FilterBy[] = ['max', 'min', 'avg'];

export function addPoint(f: Filter, facet: FilterFacet, add: Point): void {
  // TODO functional
  quantizeFilter(f);
  const qX = quantize(add[0], filterData[facet].quantum);
  const data = (f[facet] ??= []);
  const i = data.findIndex(p => p[0] >= qX);
  if (i >= 0) {
    if (data[i][0] === qX) data[i] = [qX, add[1]];
    else data.splice(i, 0, [qX, add[1]]);
  } else data.push([qX, add[1]]);
}

export function asData(f: Filter, facet: FilterFacet): { x: number; y: number }[] {
  const pts = f[facet]?.slice() ?? [];
  const xs = filterData[facet].domain;
  const defaultVal = (f.range.max - f.range.min) / 2;
  if (pts.length === 0)
    return [
      { x: xs.min - 1, y: defaultVal },
      { x: xs.max + 1, y: defaultVal },
    ];
  pts.unshift([xs.min - 1, pts[0][1]]);
  pts.push([xs.max + 1, pts[pts.length - 1][1]]);
  return pts.map(p => ({ x: p[0], y: p[1] }));
}

export function quantizeFilter(f: Filter): void {
  // TODO functional
  for (const facet of filterFacets) {
    if (!f[facet]) continue;
    const newData = f[facet].reduce((acc: Point[], p) => {
      const x = quantize(p[0], filterData[facet].quantum);
      const i = acc.findIndex(q => q[0] === x);
      if (i >= 0) acc[i] = [x, p[1]];
      else acc.push([x, p[1]]);
      return acc;
    }, []);
    newData.sort((a, b) => a[0] - b[0]);
    f[facet] = newData;
  }
}

export function evaluateFilter(f: Filter, x: FilterValue): FilterValue {
  const value: FilterValue = {};
  facetIteration: for (const facet of filterFacets) {
    if (!f[facet] || !x[facet]) continue;
    const to = (f[facet] ??= []);

    if (to.length === 0) value[facet] = (f.range.max + f.range.min) / 2;
    else if (to.length === 1 || x[facet] <= to[0][0]) value[facet] = clamp(to[0][1], f.range);
    else {
      for (let i = 0; i < to.length - 1; i++) {
        const p1 = to[i];
        const p2 = to[i + 1];
        if (p1[0] <= x[facet] && x[facet] <= p2[0]) {
          const m = (p2[1] - p1[1]) / (p2[0] - p1[0]);
          value[facet] = clamp(p1[1] + m * (x[facet] - p1[0]), f.range);
          continue facetIteration;
        }
      }
      value[facet] = to[to.length - 1][1];
    }
  }
  return value;
}

export function combine(v: FilterValue, by: FilterBy): number {
  switch (by) {
    case 'max':
      return Math.max(...Object.values(v));
    case 'min':
      return Math.min(...Object.values(v));
    case 'avg':
      return Object.values(v).reduce((sum, w) => sum + w, 0) / Object.keys(v).length;
  }
}
