import type { DrawShape } from '@lichess-org/chessground/draw';
import type { Chess } from 'chessops/chess';
import { makeSquare, squareFile, squareRank } from 'chessops/util';

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

export function makeGooglyShapes(pos: Chess, bottomColor: Color): DrawShape[] {
  const knightSquares = [...pos.board.knight];
  return knightSquares.map(sq => ({
    orig: makeSquare(sq) as Key,
    customSvg: { html: renderGooglySvg(sq, bottomColor) },
  }));
}

function renderGooglySvg(square: number, bottomColor: Color): string {
  const eyeX = -10;
  const eyeY = -15;
  const eyeSpacing = 20;

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
