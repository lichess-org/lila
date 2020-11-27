import * as cg from "./types";

export const invRanks: readonly cg.Rank[] = [
  "9",
  "8",
  "7",
  "6",
  "5",
  "4",
  "3",
  "2",
  "1",
];

const promotions: { [role: string]: cg.Role } = {
  rook: "dragon",
  bishop: "horse",
  silver: "promotedSilver",
  knight: "promotedKnight",
  lance: "promotedLance",
  pawn: "tokin",
};

export const allKeys: readonly cg.Key[] = Array.prototype.concat(
  ...cg.files.map((c) => cg.ranks.map((r) => c + r))
);

export const pos2key = (pos: cg.Pos): cg.Key => allKeys[9 * pos[0] + pos[1]];

export const key2pos = (k: cg.Key): cg.Pos => [
  k.charCodeAt(0) - 97,
  k.charCodeAt(1) - 49,
];

export function memo<A>(f: () => A): cg.Memo<A> {
  let v: A | undefined;
  const ret = (): A => {
    if (v === undefined) v = f();
    return v;
  };
  ret.clear = () => {
    v = undefined;
  };
  return ret;
}

export const timer = (): cg.Timer => {
  let startAt: number | undefined;
  return {
    start() {
      startAt = performance.now();
    },
    cancel() {
      startAt = undefined;
    },
    stop() {
      if (!startAt) return 0;
      const time = performance.now() - startAt;
      startAt = undefined;
      return time;
    },
  };
};

export const opposite = (c: cg.Color): cg.Color =>
  c === "white" ? "black" : "white";

export const distanceSq = (pos1: cg.Pos, pos2: cg.Pos): number => {
  const dx = pos1[0] - pos2[0],
    dy = pos1[1] - pos2[1];
  return dx * dx + dy * dy;
};

export const samePiece = (p1: cg.Piece, p2: cg.Piece): boolean =>
  p1.role === p2.role && p1.color === p2.color;

export const validProm = (p1: cg.Piece, p2: cg.Piece): boolean => {
  let r =
    p1.color === p2.color &&
    (promotions[p1.role] == p2.role || promotions[p2.role] == p1.role);
  return r;
};

const posToTranslateBase = (
  pos: cg.Pos,
  asWhite: boolean,
  xFactor: number,
  yFactor: number
): cg.NumberPair => [
  (asWhite ? pos[0] : 8 - pos[0]) * xFactor,
  (asWhite ? 8 - pos[1] : pos[1]) * yFactor,
];

export const posToTranslateAbs = (
  bounds: ClientRect
): ((pos: cg.Pos, asWhite: boolean) => cg.NumberPair) => {
  const xFactor = bounds.width / 9,
    yFactor = bounds.height / 9;
  return (pos, asWhite) => posToTranslateBase(pos, asWhite, xFactor, yFactor);
};

export const posToTranslateRel = (
  pos: cg.Pos,
  asWhite: boolean
): cg.NumberPair => posToTranslateBase(pos, asWhite, 100, 100);

export const translateAbs = (el: HTMLElement, pos: cg.NumberPair): void => {
  el.style.transform = `translate(${pos[0]}px,${pos[1]}px)`;
};

export const translateRel = (
  el: HTMLElement,
  percents: cg.NumberPair
): void => {
  el.style.transform = `translate(${percents[0]}%,${percents[1]}%)`;
};

export const setVisible = (el: HTMLElement, v: boolean): void => {
  el.style.visibility = v ? "visible" : "hidden";
};

export const eventPosition = (e: cg.MouchEvent): cg.NumberPair | undefined => {
  if (e.clientX || e.clientX === 0) return [e.clientX, e.clientY];
  if (e.touches && e.targetTouches[0])
    return [
      e.targetTouches[0].clientX,
      e.targetTouches[0].clientY,
    ]; /* eslint-disable-line */
  return; // touchend has no position!
};

export const isRightButton = (e: MouseEvent): boolean =>
  e.buttons === 2 || e.button === 2;

export const createEl = (tagName: string, className?: string): HTMLElement => {
  const el = document.createElement(tagName);
  if (className) el.className = className;
  return el;
};
