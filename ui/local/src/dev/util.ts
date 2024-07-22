import * as co from 'chessops';
import * as licon from 'common/licon';
import type { NumberInfo, RangeInfo } from './types';
import type { Outcome, Result } from '../types';
import type { Script } from './devCtrl';

type ObjectPath = { obj: any; path: { keys: string[] } | { id: string } };

function pathToKeys({ path, obj }: ObjectPath): string[] {
  if ('keys' in path) return path.keys;
  const keys = path.id.split('_');
  return keys[0] in obj ? keys : keys.slice(1);
}

export function resolveObjectProperty(op: ObjectPath): string[] {
  return pathToKeys(op).reduce((o, key) => o?.[key], op.obj);
}

export function removeObjectProperty({ obj, path }: ObjectPath, stripEmptyObjects = false): void {
  const keys = pathToKeys({ obj, path });
  if (!(obj && keys[0] && obj[keys[0]])) return;
  if (keys.length > 1)
    removeObjectProperty({ obj: obj[keys[0]], path: { keys: keys.slice(1) } }, stripEmptyObjects);
  if (keys.length === 1 || (stripEmptyObjects && Object.keys(obj[keys[0]]).length === 0)) {
    delete obj[keys[0]];
  }
}

export function setObjectProperty({ obj, path, value }: ObjectPath & { value: any }): void {
  const keys = pathToKeys({ obj, path });
  if (keys.length === 0) return;
  if (keys.length === 1) obj[keys[0]] = value;
  else if (!(keys[0] in obj)) obj[keys[0]] = {};
  setObjectProperty({ obj: obj[keys[0]], path: { keys: keys.slice(1) }, value });
}

export function maxChars(info: NumberInfo | RangeInfo): number {
  const len = Math.max(info.max.toString().length, info.min.toString().length);
  if (!('step' in info)) return len;
  const fractionLen = info.step < 1 ? String(info.step).length - String(info.step).indexOf('.') - 1 : 0;
  return len + fractionLen + 1;
}

export function score(outcome: Outcome, color: Color = 'white'): number {
  return outcome === color ? 1 : outcome === 'draw' ? 0.5 : 0;
}

export function botScore(r: Result, uid: string): number {
  return r.outcome === 'draw' ? 0.5 : r[r.outcome] === uid ? 1 : 0;
}

export function outcomesFor(results: Result[], uid: string | undefined): { w: number; d: number; l: number } {
  return results.reduce(
    (a, r) => ({
      w: a.w + (r.outcome !== 'draw' && r[r.outcome] === uid ? 1 : 0),
      d: a.d + (r.outcome === 'draw' && (r.white === uid || r.black === uid) ? 1 : 0),
      l: a.l + (r.outcome !== 'draw' && r[co.opposite(r.outcome)] === uid ? 1 : 0),
    }),
    { w: 0, d: 0, l: 0 },
  );
}

export function playerResults(results: Result[], uid?: string): string {
  const { w, d, l } = outcomesFor(results, uid);
  return `${w}/${d}/${l}`;
}

export function playersWithResults(script: Script): string[] {
  return [...new Set(script.players.filter(p => script.results.some(r => r.white === p || r.black === p)))];
}

export function removeButton(): Node {
  return $as<Node>(
    `<button class="button button-empty button-red icon-btn" tabindex="0" data-icon="${licon.Cancel}" data-click="remove">`,
  );
}
