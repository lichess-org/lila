import { NumberInfo, RangeInfo } from './types';
import { Outcome, Result } from '../types';
import { Script } from './devCtrl';
import * as co from 'chessops';

type ObjectPath = { obj: any; path: { keys: string[] } | { id: string } };

function pathToKeys({ path, obj }: ObjectPath): string[] {
  if ('keys' in path) return path.keys;
  const keys = path.id.split('_');
  return keys[0] in obj ? keys : keys.slice(1);
}

export function resolveObjectProperty(op: ObjectPath) {
  return pathToKeys(op).reduce((o, key) => o?.[key], op.obj);
}

export function removeObjectProperty({ obj, path }: ObjectPath, stripEmptyObjects = false) {
  const keys = pathToKeys({ obj, path });
  if (!(obj && keys[0] && obj[keys[0]])) return;
  if (keys.length > 1)
    removeObjectProperty({ obj: obj[keys[0]], path: { keys: keys.slice(1) } }, stripEmptyObjects);
  if (keys.length === 1 || (stripEmptyObjects && Object.keys(obj[keys[0]]).length === 0)) {
    delete obj[keys[0]];
  }
}

export function setObjectProperty({ obj, path, value }: ObjectPath & { value: any }) {
  const keys = pathToKeys({ obj, path });
  if (keys.length === 0) return;
  if (keys.length === 1) obj[keys[0]] = value;
  else if (!(keys[0] in obj)) obj[keys[0]] = {};
  setObjectProperty({ obj: obj[keys[0]], path: { keys: keys.slice(1) }, value });
}

export function maxChars(info: NumberInfo | RangeInfo) {
  const len = Math.max(info.max.toString().length, info.min.toString().length);
  if (!('step' in info)) return len;
  const fractionLen = info.step < 1 ? String(info.step).length - String(info.step).indexOf('.') - 1 : 0;
  return len + fractionLen;
}

export function score(outcome: Outcome, color: Color = 'white') {
  return outcome === color ? 1 : outcome === 'draw' ? 0.5 : 0;
}

export function botScore(r: Result, uid: string) {
  return r.outcome === 'draw' ? 0.5 : r[r.outcome] === uid ? 1 : 0;
}

export function outcomesFor(results: Result[], uid: string | undefined) {
  const { w, d, l } = results.reduce(
    (a, r) => ({
      w: a.w + (r.outcome !== 'draw' && r[r.outcome] === uid ? 1 : 0),
      d: a.d + (r.outcome === 'draw' && (r.white === uid || r.black === uid) ? 1 : 0),
      l: a.l + (r.outcome !== 'draw' && r[co.opposite(r.outcome)] === uid ? 1 : 0),
    }),
    { w: 0, d: 0, l: 0 },
  );
  return { wins: w, draws: d, losses: l };
}

export function playerResults(results: Result[], uid?: string) {
  const { wins, losses, draws } = outcomesFor(results, uid);
  return `${wins}/${draws}/${losses}`;
}

export function playersWithResults(script: Script) {
  return [...new Set(script.players.filter(p => script.results.some(r => r.white === p || r.black === p)))];
}
