import type { DrawShape } from '@lichess-org/chessground/draw';
import type { Chess } from 'chessops/chess';
import { makeSquare, squareFile, squareRank } from 'chessops/util';

interface GooglyEyeLayout {
  eyeX: number;
  eyeY: number;
  eyeSpacing: number;
}

const DEFAULT_LAYOUT: GooglyEyeLayout = { eyeX: -10, eyeY: -15, eyeSpacing: 15 };

const GOOGLY_EYE_LAYOUTS: Record<string, GooglyEyeLayout> = {
  default: DEFAULT_LAYOUT,
  cburnett: DEFAULT_LAYOUT,
  merida: { eyeX: -14, eyeY: -15, eyeSpacing: 15 },
  alpha: { eyeX: -4, eyeY: -18, eyeSpacing: 15 },
  pirouetti: { eyeX: -4, eyeY: -18, eyeSpacing: 15 },
  chessnut: { eyeX: -10, eyeY: -8, eyeSpacing: 15 },
  chess7: { eyeX: -10, eyeY: -15, eyeSpacing: 20 },
  reillycraig: { eyeX: -2, eyeY: -30, eyeSpacing: 15 },
  companion: { eyeX: -7, eyeY: -10, eyeSpacing: 15 },
  riohacha: { eyeX: -2, eyeY: -25, eyeSpacing: 15 },
  kosal: DEFAULT_LAYOUT,
  leipzig: DEFAULT_LAYOUT,
  fantasy: DEFAULT_LAYOUT,
  spatial: DEFAULT_LAYOUT,
  celtic: DEFAULT_LAYOUT,
  california: { eyeX: -2, eyeY: -15, eyeSpacing: 15 },
  caliente: { eyeX: -2, eyeY: -15, eyeSpacing: 15 },
  pixel: DEFAULT_LAYOUT,
  firi: { eyeX: -10, eyeY: -8, eyeSpacing: 15 },
  rhosgfx: DEFAULT_LAYOUT,
  maestro: DEFAULT_LAYOUT,
  fresca: DEFAULT_LAYOUT,
  cardinal: DEFAULT_LAYOUT,
  gioco: { eyeX: -3, eyeY: -15, eyeSpacing: 15 },
  tatiana: { eyeX: -3, eyeY: -15, eyeSpacing: 15 },
  staunty: { eyeX: -3, eyeY: -15, eyeSpacing: 15 },
  cooke: { eyeX: -2, eyeY: -25, eyeSpacing: 15 },
  monarchy: { eyeX: -4, eyeY: -18, eyeSpacing: 15 },
  governor: { eyeX: -5, eyeY: -18, eyeSpacing: 15 },
  dubrovny: { eyeX: -10, eyeY: -15, eyeSpacing: 18 },
  'shahi-ivory-brown': { eyeX: -6, eyeY: -18, eyeSpacing: 15 },
  icpieces: { eyeX: -10, eyeY: -8, eyeSpacing: 15 },
  mpchess: { eyeX: -10, eyeY: -20, eyeSpacing: 15 },
  'kiwen-suwi': { eyeX: 3, eyeY: -15, eyeSpacing: 16 },
  horsey: { eyeX: 4, eyeY: -15, eyeSpacing: 15 },
  anarcandy: { eyeX: -22, eyeY: -11, eyeSpacing: 18 },
  xkcd: DEFAULT_LAYOUT,
  shapes: { eyeX: -10, eyeY: -15, eyeSpacing: 20 },
  letter: { eyeX: 0, eyeY: -8, eyeSpacing: 15 },
  disguised: { eyeX: 0, eyeY: -8, eyeSpacing: 15 },
};

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

export function enableGooglyEyesTracking(wrap: HTMLElement, redraw: () => void): void {
  disableGooglyEyesTracking();
  boardRectSource = () => wrap.getBoundingClientRect();
  requestRedraw = redraw;
  document.addEventListener('mousemove', onMouseMove);
}

export function disableGooglyEyesTracking(): void {
  if (rafId !== undefined) cancelAnimationFrame(rafId);
  rafId = undefined;
  boardRectSource = undefined;
  requestRedraw = undefined;
  document.removeEventListener('mousemove', onMouseMove);
}

export function makeGooglyShapes(pos: Chess, bottomColor: Color, pieceSet: string): DrawShape[] {
  const layout = GOOGLY_EYE_LAYOUTS[pieceSet] ?? DEFAULT_LAYOUT;
  return [...pos.board.knight].map(sq => ({
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
