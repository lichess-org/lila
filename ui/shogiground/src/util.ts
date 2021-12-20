import * as cg from './types';

export const invFiles: readonly cg.File[] = [...cg.files].reverse();

const promotions: { [role: string]: cg.Role } = {
  rook: 'dragon',
  bishop: 'horse',
  silver: 'promotedsilver',
  knight: 'promotedknight',
  lance: 'promotedlance',
  pawn: 'tokin',
};

// 1a, 1b, 1c ...
export const allKeys: readonly cg.Key[] = Array.prototype.concat(...cg.files.map(c => cg.ranks.map(r => c + r)));

export const pos2key = (pos: cg.Pos): cg.Key => allKeys[9 * pos[0] + pos[1]];

//export const pos2key = (pos: cg.Pos): cg.Key => allKeys[9 * (8 - pos[0]) + (8 - pos[1])];
export const key2pos = (k: cg.Key): cg.Pos => [k.charCodeAt(0) - 49, k.charCodeAt(1) - 97];
//export const key2pos = (k: cg.Key): cg.Pos => {
//  if (Number.isInteger(k[0])) return [k.charCodeAt(0) - 49, k.charCodeAt(1) - 97];
//  return [8 - (k.charCodeAt(0) - 97), 8 - (k.charCodeAt(1) - 49)];
//};

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

export const opposite = (c: cg.Color): cg.Color => (c === 'sente' ? 'gote' : 'sente');

export const distanceSq = (pos1: cg.Pos, pos2: cg.Pos): number => {
  const dx = pos1[0] - pos2[0],
    dy = pos1[1] - pos2[1];
  return dx * dx + dy * dy;
};

export const samePiece = (p1: cg.Piece, p2: cg.Piece): boolean => p1.role === p2.role && p1.color === p2.color;

export const validProm = (p1: cg.Piece, p2: cg.Piece): boolean => {
  let r = p1.color === p2.color && (promotions[p1.role] == p2.role || promotions[p2.role] == p1.role);
  return r;
};

const posToTranslateBase = (
  pos: cg.Pos,
  dims: cg.Dimensions,
  asSente: boolean,
  xFactor: number,
  yFactor: number
): cg.NumberPair => [
  (asSente ? dims.files - 1 - pos[0] : pos[0]) * xFactor,
  (asSente ? pos[1] : dims.ranks - 1 - pos[1]) * yFactor,
];

export const posToTranslateAbs = (
  dims: cg.Dimensions,
  bounds: ClientRect
): ((pos: cg.Pos, asSente: boolean) => cg.NumberPair) => {
  const xFactor = bounds.width / dims.files,
    yFactor = bounds.height / dims.ranks;
  return (pos, asSente) => posToTranslateBase(pos, dims, asSente, xFactor, yFactor);
};

export const posToTranslateRel =
  (dims: cg.Dimensions): ((pos: cg.Pos, asSente: boolean) => cg.NumberPair) =>
  (pos, asSente) =>
    posToTranslateBase(pos, dims, asSente, 50, 50);

// we don't scale squares
export const translateAbs = (el: HTMLElement, pos: cg.NumberPair, scale: boolean = true): void => {
  el.style.transform = `translate(${pos[0]}px,${pos[1]}px) ${scale ? 'scale(0.5)' : ''}`;
};

export const translateRel = (el: HTMLElement, percents: cg.NumberPair, scale: boolean = true): void => {
  const scaleRatio = scale ? 1 : 2;
  el.style.transform = `translate(${scaleRatio * percents[0]}%,${scaleRatio * percents[1]}%) ${
    scale ? 'scale(0.5)' : ''
  }`;
};

export const setVisible = (el: HTMLElement, v: boolean): void => {
  el.style.visibility = v ? 'visible' : 'hidden';
};

export const eventPosition = (e: cg.MouchEvent): cg.NumberPair | undefined => {
  if (e.clientX || e.clientX === 0) return [e.clientX, e.clientY!];
  if (e.targetTouches?.[0]) return [e.targetTouches[0].clientX, e.targetTouches[0].clientY];
  return; // touchend has no position!
};

export const isRightButton = (e: cg.MouchEvent): boolean => e.buttons === 2 || e.button === 2;

export const createEl = (tagName: string, className?: string): HTMLElement => {
  const el = document.createElement(tagName);
  if (className) el.className = className;
  return el;
};

export const isMiniBoard = (el: HTMLElement): boolean => {
  return Array.from(el.classList).includes('mini-board');
};

export function computeSquareCenter(
  key: cg.Key,
  asSente: boolean,
  dims: cg.Dimensions,
  bounds: ClientRect
): cg.NumberPair {
  const pos = key2pos(key);
  if (asSente) {
    pos[0] = dims.files - 1 - pos[0];
    pos[1] = dims.ranks - 1 - pos[1];
  }
  return [
    bounds.left + (bounds.width * pos[0]) / dims.files + bounds.width / (dims.files * 2),
    bounds.top + (bounds.height * (dims.ranks - 1 - pos[1])) / dims.ranks + bounds.height / (dims.ranks * 2),
  ];
}

// todo - pass valid hand roles in config
export const droppableRoles: readonly cg.Role[] = ['pawn', 'lance', 'knight', 'silver', 'gold', 'bishop', 'rook'];
export const miniDroppableRoles: readonly cg.Role[] = ['pawn', 'silver', 'gold', 'bishop', 'rook'];
