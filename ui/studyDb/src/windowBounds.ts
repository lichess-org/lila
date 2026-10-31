import { clamp } from 'lib/algo';

export interface Rect {
  left: number;
  top: number;
  width: number;
  height: number;
}

const minWindowWidth = 280;
export const resizeEdges = ['e', 'se', 's', 'sw', 'w'] as const;
export type ResizeEdge = (typeof resizeEdges)[number];

export function windowViewport(): Rect {
  return {
    width: document.documentElement.clientWidth || window.innerWidth,
    height: window.innerHeight,
    left: 0,
    top: parseFloat(window.getComputedStyle(document.body).getPropertyValue('---site-header-height')) || 0,
  };
}

export function fitWindow(rect: Rect, viewport: Rect): Rect {
  const inset = Math.min(8, viewport.width / 4, viewport.height / 4);
  const minTop = Math.max(inset, viewport.top);
  const availableHeight = Math.max(0, viewport.height - inset - minTop);
  const width = clamp(rect.width, {
    min: minWindowWidth,
    max: Math.max(minWindowWidth, viewport.width - 2 * inset),
  });
  const height = clamp(rect.height, {
    min: Math.min(160, availableHeight),
    max: availableHeight,
  });
  return {
    left: clamp(rect.left, { min: inset, max: viewport.width - inset - width }),
    top: clamp(rect.top, { min: minTop, max: viewport.height - inset - height }),
    width,
    height,
  };
}

export function moveWindow(
  rect: Rect,
  dx: number,
  dy: number,
  edge: ResizeEdge | undefined,
  viewport: Rect,
): Rect {
  if (!edge) return fitWindow({ ...rect, left: rect.left + dx, top: rect.top + dy }, viewport);
  const inset = Math.min(8, viewport.width / 4, viewport.height / 4);
  const minWidth = minWindowWidth;
  const minTop = Math.max(inset, viewport.top);
  const minHeight = Math.min(160, Math.max(0, viewport.height - inset - minTop));
  let { left } = rect;
  let right = left + rect.width;
  let bottom = rect.top + rect.height;
  if (edge.includes('w')) left = clamp(left + dx, { min: inset, max: right - minWidth });
  if (edge.includes('e')) right = clamp(right + dx, { min: left + minWidth, max: viewport.width - inset });
  if (edge.includes('s'))
    bottom = clamp(bottom + dy, { min: rect.top + minHeight, max: viewport.height - inset });
  return { left, top: rect.top, width: right - left, height: bottom - rect.top };
}
