import * as co from 'chessops';
import * as licon from 'lib/licon';
import type { BotInfo } from 'lib/bot/types';
import { frag } from 'lib';
import type { NumberInfo, RangeInfo } from './devTypes';
import type { Result } from './devCtrl';

type ObjectPath = { obj: any; path: { keys: string[] } | { id: string } };

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

export function deadStrip(info: BotInfo & { disabled: Set<string> }): BotInfo {
  if (!('disabled' in info)) return info;

  const temp = structuredClone(info);

  for (const id of info.disabled) {
    removeObjectProperty({ obj: temp, path: { id } }, true);
  }
  return temp;
}

export function maxChars(info: NumberInfo | RangeInfo): number {
  const len = Math.max(info.max.toString().length, info.min.toString().length);
  if (!('step' in info)) return len;
  const fractionLen = info.step < 1 ? String(info.step).length - String(info.step).indexOf('.') - 1 : 0;
  return len + fractionLen + 1;
}

export function score(outcome: Color | undefined, color: Color = 'white'): number {
  return outcome === color ? 1 : outcome === undefined ? 0.5 : 0;
}

export function botScore(r: Result, uid: string): number {
  return r.winner === undefined ? 0.5 : r[r.winner] === uid ? 1 : 0;
}

export function resultsObject(
  results: Result[],
  uid: string | undefined,
): { w: number; d: number; l: number } {
  return results.reduce(
    (a, r) => ({
      w: a.w + (r.winner !== undefined && r[r.winner] === uid ? 1 : 0),
      d: a.d + (r.winner === undefined && (r.white === uid || r.black === uid) ? 1 : 0),
      l: a.l + (r.winner !== undefined && r[co.opposite(r.winner)] === uid ? 1 : 0),
    }),
    { w: 0, d: 0, l: 0 },
  );
}

export function resultsString(results: Result[], uid?: string): string {
  const { w, d, l } = resultsObject(results, uid);
  return `${w}/${d}/${l}`;
}

export function playersWithResults(results: Result[]): string[] {
  return [...new Set(results.flatMap(r => [r.white ?? '', r.black ?? ''].filter(x => x)))];
}

export function renderRemoveButton(cls: string = ''): Node {
  return frag(
    `<button class="button button-empty button-red icon-btn ${cls}" tabindex="0" data-icon="${licon.Cancel}" data-action="remove">`,
  );
}

function pathToKeys({ path, obj }: ObjectPath): string[] {
  if ('keys' in path) return path.keys;
  const keys = path.id.split('_');
  return keys[0] in obj ? keys : keys.slice(1);
}

export const rangeTicks: Record<string, [number, string][]> = {
  initial: [
    [15, '15 seconds'],
    [30, '30 seconds'],
    [45, '45 seconds'],
    [60, '1 minute'],
    [120, '2 minutes'],
    [180, '3 minutes'],
    [300, '5 minutes'],
    [600, '10 minutes'],
    [1800, '30 minutes'],
    [3600, '60 minutes'],
    [5400, '90 minutes'],
    [Infinity, 'unlimited'],
  ],
  increment: [
    [0, 'none'],
    [1, '1 second'],
    [2, '2 seconds'],
    [3, '3 seconds'],
    [5, '5 seconds'],
    [10, '10 seconds'],
    [30, '30 seconds'],
  ],
};
