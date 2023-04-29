import * as cs from 'chess';

// findTransforms & validOps also found in @build/src/makeGrammar.ts

const mode = { del: true, sub: 2 };

export function findTransforms(
  h: string,
  x: string,
  pos = 0, // for recursion
  line: Transform[] = [], // for recursion
  lines: Transform[][] = [], // for recursion
  crumbs = new Map<string, number>() // for (finite) recursion
): Transform[][] {
  if (h === x) return [line];
  if (pos >= x.length && !mode.del) return [];
  if (crumbs.has(h + pos) && crumbs.get(h + pos)! <= line.length) return [];
  crumbs.set(h + pos, line.length);

  return validOps(h, x, pos).flatMap(({ hnext, op }) =>
    findTransforms(
      hnext,
      x,
      pos + (op === 'skip' ? 1 : op.to.length),
      op === 'skip' ? line : [...line, op],
      lines,
      crumbs
    )
  );
}

function validOps(h: string, x: string, pos: number) {
  const validOps: { hnext: string; op: Transform | 'skip' }[] = [];
  if (h[pos] === x[pos]) validOps.push({ hnext: h, op: 'skip' });
  const minSlice = mode.del !== true || validOps.length > 0 ? 1 : 0;
  let slen = Math.min(mode.sub ?? 0, x.length - pos);
  while (slen >= minSlice) {
    const slice = x.slice(pos, pos + slen);
    if (pos < h.length && !(slen > 0 && h.startsWith(slice, pos)))
      validOps.push({
        hnext: h.slice(0, pos) + slice + h.slice(pos + 1),
        op: { from: h[pos], at: pos, to: slice },
      });
    slen--;
  }
  return validOps;
}

// optimizations for when a set has only one element, which is most of the time

export function spread<T>(v: undefined | T | Set<T>): T[] {
  return v === undefined ? [] : v instanceof Set ? [...v] : [v];
}

export function spreadMap<T>(m: Map<string, T | Set<T>>): [string, T[]][] {
  return [...m].map(([k, v]) => [k, spread(v)]);
}

export function getSpread<T>(m: Map<string, T | Set<T>>, key: string): T[] {
  return spread(m.get(key));
}

export function remove<T>(m: Map<string, T | Set<T>>, key: string, val: T) {
  const v = m.get(key);
  if (v === val) m.delete(key);
  else if (v instanceof Set) v.delete(val);
}

export function pushMap<T>(m: Map<string, T | Set<T>>, key: string, val: T) {
  const v = m.get(key);
  if (!v) m.set(key, val);
  else {
    if (v instanceof Set) v.add(val);
    else if (v !== val) m.set(key, new Set([v as T, val]));
  }
}

export function movesTo(s: number, role: string, board: cs.Board): number[] {
  const deltas = (d: number[], s = 0) => d.flatMap(x => [s - x, s + x]);

  if (role === 'K') return deltas([1, 7, 8, 9], s).filter(o => o >= 0 && o < 64 && cs.squareDist(s, o) === 1);
  else if (role === 'N') return deltas([6, 10, 15, 17], s).filter(o => o >= 0 && o < 64 && cs.squareDist(s, o) <= 2);
  const dests: number[] = [];
  for (const delta of deltas(role === 'Q' ? [1, 7, 8, 9] : role === 'R' ? [1, 8] : role === 'B' ? [7, 9] : [])) {
    for (
      let square = s + delta;
      square >= 0 && square < 64 && cs.squareDist(square, square - delta) === 1;
      square += delta
    ) {
      dests.push(square);
      if (board.pieces[square]) break;
    }
  }
  return dests;
}

export type Transform = {
  from: string; // single token
  to: string; // zero or more tokens, (empty string for erasure)
  at: number; // index (unused now, previously for breadcrumbs)
};

export const src = (uci: Uci) => uci.slice(0, 2) as Key;
export const dest = (uci: Uci) => uci.slice(2, 4) as Key;

export const promo = (uci: Uci) =>
  ({
    P: 'pawn',
    N: 'knight',
    B: 'bishop',
    R: 'rook',
    Q: 'queen',
    K: 'king',
  }[uci.slice(4, 5).toUpperCase()]);
