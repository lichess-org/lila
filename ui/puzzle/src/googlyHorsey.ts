import type { DrawShape } from '@lichess-org/chessground/draw';
import type { Chess } from 'chessops/chess';
import { makeSquare, squareFile, squareRank } from 'chessops/util';

export interface GooglyEyeLayout {
  eyeX: number;
  eyeY: number;
  eyeSpacing: number;
}

const DEFAULT_LAYOUT: GooglyEyeLayout = { eyeX: -10, eyeY: -15, eyeSpacing: 20 };

/** Hardcode different eye positions per piece set. Add entries as needed. */
const GOOGLY_EYE_LAYOUTS: Record<string, GooglyEyeLayout> = {
  default: DEFAULT_LAYOUT,
  cburnett: DEFAULT_LAYOUT,
  merida: DEFAULT_LAYOUT,
  alpha: { eyeX: -4, eyeY: -18, eyeSpacing: 15 },
  pirouetti: { eyeX: -4, eyeY: -18, eyeSpacing: 15 },
  chessnut: DEFAULT_LAYOUT,
  chess7: DEFAULT_LAYOUT,
  reillycraig: DEFAULT_LAYOUT,
  companion: { eyeX: -7, eyeY: -10, eyeSpacing: 15 },
  riohacha: DEFAULT_LAYOUT,
  kosal: DEFAULT_LAYOUT,
  leipzig: DEFAULT_LAYOUT,
  fantasy: DEFAULT_LAYOUT,
  spatial: DEFAULT_LAYOUT,
  celtic: DEFAULT_LAYOUT,
  california: DEFAULT_LAYOUT,
  caliente: { eyeX: -2, eyeY: -15, eyeSpacing: 15 },
  pixel: DEFAULT_LAYOUT,
  firi: DEFAULT_LAYOUT,
  rhosgfx: DEFAULT_LAYOUT,
  maestro: DEFAULT_LAYOUT,
  fresca: DEFAULT_LAYOUT,
  cardinal: DEFAULT_LAYOUT,
  gioco: DEFAULT_LAYOUT,
  tatiana: DEFAULT_LAYOUT,
  staunty: DEFAULT_LAYOUT,
  cooke: DEFAULT_LAYOUT,
  monarchy: DEFAULT_LAYOUT,
  governor: DEFAULT_LAYOUT,
  dubrovny: DEFAULT_LAYOUT,
  shahi: DEFAULT_LAYOUT,
  icpieces: DEFAULT_LAYOUT,
  mpchess: DEFAULT_LAYOUT,
  kiwen: DEFAULT_LAYOUT,
  horsey: DEFAULT_LAYOUT,
  anarcandy: DEFAULT_LAYOUT,
  xkcd: DEFAULT_LAYOUT,
  shapes: DEFAULT_LAYOUT,
  letter: DEFAULT_LAYOUT,
  disguised: DEFAULT_LAYOUT,
};

function getGooglyEyeLayout(pieceSet: string): GooglyEyeLayout {
  return GOOGLY_EYE_LAYOUTS[pieceSet] ?? DEFAULT_LAYOUT;
}

let mousePos = { x: 0.5, y: 0.5 };
let rafId: number | undefined;
let boardRectSource: (() => DOMRect | undefined) | undefined;
let requestRedraw: (() => void) | undefined;

function onMouseMove(e: MouseEvent): void {
  if (!boardRectSource || !requestRedraw) return;
  const rect = boardRectSource();
  if (!rect) return;
  mousePos = {
    x: (e.clientX - rect.left) / rect.width,
    y: (e.clientY - rect.top) / rect.height,
  };
  if (rafId === undefined) {
    rafId = requestAnimationFrame(() => {
      rafId = undefined;
      requestRedraw?.();
    });
  }
}

// Maybe revisit - this keeps eyes from disappearing when clicked, but there's a flicker
function onMouseDown(_e: MouseEvent): void {
  if (!boardRectSource || !requestRedraw) return;
  const rect = boardRectSource();
  if (!rect) return;
  if (rafId === undefined) {
    rafId = requestAnimationFrame(() => {
      rafId = undefined;
      requestRedraw?.();
    });
  }
}

export function enableGooglyEyesTracking(wrap: HTMLElement, redraw: () => void): void {
  disableGooglyEyesTracking();
  boardRectSource = () => wrap.getBoundingClientRect();
  requestRedraw = redraw;
  document.addEventListener('mousemove', onMouseMove);
  document.addEventListener('mousedown', onMouseDown);
}

export function disableGooglyEyesTracking(): void {
  if (rafId !== undefined) cancelAnimationFrame(rafId);
  rafId = undefined;
  boardRectSource = undefined;
  requestRedraw = undefined;
  document.removeEventListener('mousemove', onMouseMove);
  document.removeEventListener('mousedown', onMouseDown);
}

export function makeGooglyShapes(pos: Chess, bottomColor: Color, pieceSet: string): DrawShape[] {
  const layout = getGooglyEyeLayout(pieceSet);
  const knightSquares = [...pos.board.knight];
  return knightSquares.map(sq => ({
    orig: makeSquare(sq) as Key,
    customSvg: { html: renderGooglySvg(sq, bottomColor, layout) },
  }));
}

function renderGooglySvg(square: number, bottomColor: Color, layout: GooglyEyeLayout): string {
  const { eyeX, eyeY, eyeSpacing } = layout;

  const { x: mx, y: my } = mousePos;
  const file = squareFile(square);
  const rank = squareRank(square);
  const isBlackAtBottom = bottomColor === 'black';
  const eyeCenter = isBlackAtBottom
    ? { x: (7.5 - file) / 8, y: (rank + 0.5) / 8 }
    : { x: (file + 0.5) / 8, y: (7.5 - rank) / 8 };
  let dx = mx - eyeCenter.x;
  let dy = my - eyeCenter.y;
  const len = Math.hypot(dx, dy) || 1;
  const maxOffset = 4;
  dx = (dx / len) * maxOffset;
  dy = (dy / len) * maxOffset;
  const leftEyeX = eyeX - eyeSpacing / 2;
  const rightEyeX = eyeX + eyeSpacing / 2;
  const lx = leftEyeX + dx;
  const ly = eyeY + dy;
  const rx = rightEyeX + dx;
  const ry = eyeY + dy;
  return $html`
  <g transform="translate(50, 50)">
    <circle cx="${leftEyeX}" cy="${eyeY}" r="8" fill="white" stroke="#333" stroke-width="1.5"/>
    <circle cx="${lx}" cy="${ly}" r="5" fill="#222"/>
    <circle cx="${lx - 2}" cy="${ly - 2.5}" r="1.5" fill="white" opacity="0.9"/>
    <circle cx="${rightEyeX}" cy="${eyeY}" r="8" fill="white" stroke="#333" stroke-width="1.5"/>
    <circle cx="${rx}" cy="${ry}" r="5" fill="#222"/>
    <circle cx="${rx - 2}" cy="${ry - 2.5}" r="1.5" fill="white" opacity="0.9"/>
  </g>`;
}
