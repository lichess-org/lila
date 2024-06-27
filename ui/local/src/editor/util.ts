import { NumberInfo, RangeInfo } from './types';

export function idToPath({ id, obj }: { id: string; obj: any }): string[] {
  const path = id.split('_');
  return path[0] in obj ? path : path.slice(1);
}

export function resolvePath({ obj, path }: { obj: any; path: string[] }) {
  return path.reduce((o, key) => o?.[key], obj);
}

export function removePath({ obj, path }: { obj: any; path: string[] }, stripEmptyObjects = false) {
  if (!(obj && path[0] && obj[path[0]])) return;
  if (path.length > 1) removePath({ obj: obj[path[0]], path: path.slice(1) }, stripEmptyObjects);
  if (path.length === 1 || (stripEmptyObjects && Object.keys(obj[path[0]]).length === 0)) {
    delete obj[path[0]];
  }
}

export function setPath({ obj, path, value }: { obj: any; path: string[]; value: any }) {
  if (path.length === 0) return;
  if (path.length === 1) obj[path[0]] = value;
  else if (!(path[0] in obj)) obj[path[0]] = {};
  setPath({ obj: obj[path[0]], path: path.slice(1), value });
}

export function maxChars(info: NumberInfo | RangeInfo) {
  const len = Math.max(info.max.toString().length, info.min.toString().length);
  if (!('step' in info)) return len;
  const fractionLen = info.step < 1 ? String(info.step).length - String(info.step).indexOf('.') - 1 : 0;
  return len + fractionLen;
}
