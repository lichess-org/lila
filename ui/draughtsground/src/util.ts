import * as cg from './types';

export const colors: cg.Color[] = ['white', 'black'];

export const allKeys: cg.Key[] = ["01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50"];

export const pos2key = (pos: cg.Pos) => allKeys[pos[0] + 5 * pos[1] - 6];

export const key2pos = (k: cg.Key) => key2posn(parseInt(k));
const key2posn = (k: number) => [(k - 1) % 5 + 1, ((k - 1) + (5 - (k - 1) % 5)) / 5] as cg.Pos;

export function memo<A>(f: () => A): cg.Memo<A> {
  let v: A | undefined;
  const ret: any = () => {
    if (v === undefined) v = f();
    return v;
  };
  ret.clear = () => { v = undefined; };
  return ret;
}

export const timer: () => cg.Timer = () => {
  let startAt: number | undefined;
  return {
    start() { startAt = Date.now(); },
    cancel() { startAt = undefined; },
    stop() {
      if (!startAt) return 0;
      const time = Date.now() - startAt;
      startAt = undefined;
      return time;
    }
  };
}

export const opposite = (c: cg.Color) => c === 'white' ? 'black' : 'white';

export function containsX<X>(xs: X[] | undefined, x: X): boolean {
  return xs !== undefined && xs.indexOf(x) !== -1;
}

export const distanceSq: (pos1: cg.Pos, pos2: cg.Pos) => number = (pos1, pos2) => {
  return Math.pow(pos1[0] - pos2[0], 2) + Math.pow(pos1[1] - pos2[1], 2);
}

export const samePiece: (p1: cg.Piece, p2: cg.Piece) => boolean = (p1, p2) =>
  p1.role === p2.role && p1.color === p2.color;

export const computeIsTrident = () => window.navigator.userAgent.indexOf('Trident/') > -1;

const posToTranslateBase: (pos: cg.Pos, asWhite: boolean, xFactor: number, yFactor: number, shift: number) => cg.NumberPair =
  (pos, asWhite, xFactor, yFactor, shift: number) => {
    if (shift !== 0) {
      return [
        (!asWhite ? 4.5 - ((shift - 0.5) + pos[0]) : (shift - 0.5) + pos[0]) * xFactor,
        (!asWhite ? 10 - pos[1] : pos[1] - 1) * yFactor
      ];
    } else {
      return [
        (!asWhite ? 4.5 - ((pos[1] % 2 !== 0 ? -0.5 : -1) + pos[0]) : (pos[1] % 2 !== 0 ? -0.5 : -1) + pos[0]) * xFactor,
        (!asWhite ? 10 - pos[1] : pos[1] - 1) * yFactor
      ];
    }
  }

export const posToTranslateAbs = (bounds: ClientRect) => {
  const xFactor = bounds.width / 5, yFactor = bounds.height / 10;
  return (pos: cg.Pos, asWhite: boolean, shift: number) => posToTranslateBase(pos, asWhite, xFactor, yFactor, shift);
};

export const posToTranslateRel: (pos: cg.Pos, asWhite: boolean, shift: number) => cg.NumberPair =
    (pos, asWhite, shift: number) => posToTranslateBase(pos, asWhite, 20.0, 10.0, shift);

/**
 * Modifies dom element style with asolute value (translate attribute, amount of pixels)
 */
export const translateAbs = (el: HTMLElement, pos: cg.Pos) => {
  el.style.transform = `translate(${pos[0]}px,${pos[1]}px)`;
}

/**
 * Modifies dom element style with relative value (percentage)
 */
export const translateRel = (el: HTMLElement, percents: cg.NumberPair) => {
  el.style.left = percents[0] + '%';
  el.style.top = percents[1] + '%';
}

export const translateAway = (el: HTMLElement) => translateAbs(el, [-99999, -99999]);

// touchend has no position!
export const eventPosition: (e: cg.MouchEvent) => cg.NumberPair | undefined = e => {
  if (e.clientX || e.clientX === 0) return [e.clientX, e.clientY];
  if (e.touches && e.targetTouches[0]) return [e.targetTouches[0].clientX, e.targetTouches[0].clientY];
  return undefined;
}

export const isRightButton = (e: MouseEvent) => e.buttons === 2 || e.button === 2;

export const createEl = (tagName: string, className?: string) => {
  const el = document.createElement(tagName);
  if (className) el.className = className;
  return el;
}

export const raf = (window.requestAnimationFrame || window.setTimeout).bind(window);
