import { stringToRole, makeSquare, makeUsi, Move, parseSquare } from 'shogiops';

export function defined<A>(v: A | undefined): v is A {
  return typeof v !== 'undefined';
}

export function empty(a: any): boolean {
  return !a || a.length === 0;
}

export interface Prop<T> {
  (): T;
  (v: T): T;
}

// like mithril prop but with type safety
export function prop<A>(initialValue: A): Prop<A> {
  let value = initialValue;
  const fun = function (v: A | undefined) {
    if (defined(v)) value = v;
    return value;
  };
  return fun as Prop<A>;
}

function parseChessSquare(str: string): number | undefined {
  if (str.length !== 2) return;
  const file = Math.abs(str.charCodeAt(0) - 'a'.charCodeAt(0));
  const rank = Math.abs(str.charCodeAt(1) - '1'.charCodeAt(0));
  if (file < 0 || file >= 9 || rank < 0 || rank >= 9) return;
  return file + 9 * rank;
}

function parseLishogiUci(str: string): Move | undefined {
  if (str[1] === '*' && str.length === 4) {
    const role = stringToRole(str[0]);
    const to = parseChessSquare(str.slice(2));
    if (defined(role) && defined(to)) return { role, to };
  } else if (str.length === 4 || str.length === 5) {
    const from = parseChessSquare(str.slice(0, 2));
    const to = parseChessSquare(str.slice(2, 4));
    const promotion = str[4] === '+' ? true : false;
    if (defined(from) && defined(to)) return { from, to, promotion };
  }
  return;
}

function assureUsi(str: string): string | undefined {
  if (str.match(/^([1-9][a-i]|([RBGSNLP]\*))[1-9][a-i](\+|\=)?$/)) return str;
  if (str.match(/^([a-i][1-9]|([RBGSNLP]\*))[a-i][1-9](\+|\=)?$/)) return makeUsi(parseLishogiUci(str)!);
  return;
}

export function pretendItsSquare(cssq: string): Key {
  const sq = parseSquare(cssq);
  if (sq) return makeSquare(sq);
  else {
    console.log('NOT SHOGI SQUARE:', cssq);
    console.trace();
    return makeSquare(parseChessSquare(cssq)!);
  }
}

export function pretendItsUsi(uciOrUsi: string): Usi {
  const usi = assureUsi(uciOrUsi);
  if (!usi || usi !== uciOrUsi) {
    console.log('NOT USI:', uciOrUsi);
    console.trace();
  }
  return usi!;
}
