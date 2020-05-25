import { State } from './state'
import { key2pos } from './util'
import { Drawable, DrawShape, DrawShapePiece, DrawBrush, DrawBrushes, DrawModifiers } from './draw'
import * as cg from './types'

export function createElement(tagName: string): SVGElement {
  return document.createElementNS('http://www.w3.org/2000/svg', tagName);
}

interface Shape {
  shape: DrawShape;
  current: boolean;
  hash: Hash;
}

interface CustomBrushes {
  [hash: string]: DrawBrush
}

interface ArrowDests {
  [key: string]: number; // how many arrows land on a square
}

type Hash = string;

export function renderSvg(state: State, root: SVGElement): void {

  const d = state.drawable, curD = d.current, arrowDests: ArrowDests = {};

  const cur = curD && (curD.mouseSq || curD.prev) ? curD as DrawShape : undefined;


  d.shapes.concat(d.autoShapes).concat(cur ? [cur] : []).forEach(s => {
    if (s.dest) arrowDests[s.dest] = (arrowDests[s.dest] || 0) + 1;
  });

  const shapes: Shape[] = d.shapes.concat(d.autoShapes).map((s: DrawShape) => {
    return {
      shape: s,
      current: false,
      hash: shapeHash(s, arrowDests, false)
    };
  });
  if (cur) {
    shapes.push({
      shape: cur,
      current: true,
      hash: shapeHash(cur, arrowDests, true)
    });
  }

  const fullHash = shapes.map(sc => sc.hash).join('');
  if (fullHash === state.drawable.prevSvgHash) return;
  state.drawable.prevSvgHash = fullHash;

  const defsEl = root.firstChild as SVGElement;

  syncDefs(d, shapes, defsEl);
  syncShapes(state, shapes, d.brushes, arrowDests, root, defsEl);

}

// append only. Don't try to update/remove.
function syncDefs(d: Drawable, shapes: Shape[], defsEl: SVGElement) {
  const brushes: CustomBrushes = {};
  let brush: DrawBrush;
  shapes.forEach(s => {
    if (s.shape.dest) {
      brush = d.brushes[s.shape.brush];
      if (s.shape.modifiers) brush = makeCustomBrush(brush, s.shape.modifiers);
      brushes[brush.key] = brush;
    }
  });
  const keysInDom: {[key: string]: boolean} = {};
  let el: SVGElement = defsEl.firstChild as SVGElement;
  while(el) {
    keysInDom[el.getAttribute('cgKey') as string] = true;
    el = el.nextSibling as SVGElement;
  }
  for (let key in brushes) {
    if (!keysInDom[key]) defsEl.appendChild(renderMarker(brushes[key]));
  }
}

// append and remove only. No updates.
function syncShapes(state: State, shapes: Shape[], brushes: DrawBrushes, arrowDests: ArrowDests, root: SVGElement, defsEl: SVGElement): void {
  const bounds = state.dom.bounds(),
  hashesInDom: {[hash: string]: boolean} = {},
  toRemove: SVGElement[] = [];
  shapes.forEach(sc => { hashesInDom[sc.hash] = false; });
  let el: SVGElement = defsEl.nextSibling as SVGElement, elHash: Hash;
  while(el) {
    elHash = el.getAttribute('cgHash') as Hash;
    // found a shape element that's here to stay
    if (hashesInDom.hasOwnProperty(elHash)) hashesInDom[elHash] = true;
    // or remove it
    else toRemove.push(el);
    el = el.nextSibling as SVGElement;
  }
  // remove old shapes
  toRemove.forEach(el => root.removeChild(el));
  // insert shapes that are not yet in dom
  shapes.forEach(sc => {
    if (!hashesInDom[sc.hash]) root.appendChild(renderShape(state, sc, brushes, arrowDests, bounds, state.boardSize));
  });
}

function shapeHash({ orig, dest, brush, piece, modifiers }: DrawShape, arrowDests: ArrowDests, current: boolean): Hash {
  return [current, orig, dest, brush, dest && arrowDests[dest] > 1,
    piece && pieceHash(piece),
    modifiers && modifiersHash(modifiers)
  ].filter(x => x).join('');
}

function pieceHash(piece: DrawShapePiece): Hash {
  return [piece.color, piece.role, piece.scale].filter(x => x).join('');
}

function modifiersHash(m: DrawModifiers): Hash {
  return '' + (m.lineWidth || '');
}

function renderShape(state: State, { shape, current, hash }: Shape, brushes: DrawBrushes, arrowDests: ArrowDests, bounds: ClientRect, boardSize: cg.BoardSize): SVGElement {
  let el: SVGElement;
  if (shape.piece) el = renderPiece(
    state.drawable.pieces.baseUrl,
    orient(key2pos(shape.orig, state.boardSize), state.orientation, boardSize),
    shape.piece,
    bounds,
    boardSize);
  else {
    const orig = orient(key2pos(shape.orig, state.boardSize), state.orientation, boardSize);
    if (shape.orig && shape.dest) {
      let brush: DrawBrush = brushes[shape.brush];
      if (shape.modifiers) brush = makeCustomBrush(brush, shape.modifiers);
      el = renderArrow(
        brush,
        orig,
        orient(key2pos(shape.dest, state.boardSize), state.orientation, boardSize),
        current,
        arrowDests[shape.dest] > 1,
        bounds,
        boardSize);
    }
    else el = renderCircle(brushes[shape.brush], orig, current, bounds, boardSize);
  }
  el.setAttribute('cgHash', hash);
  return el;
}

function renderCircle(brush: DrawBrush, pos: cg.Pos, current: boolean, bounds: ClientRect, boardSize: cg.BoardSize): SVGElement {
  const o = pos2px(pos, bounds, boardSize),
  widths = circleWidth(bounds, boardSize),
  radius = (bounds.width + bounds.height) / (2 * (boardSize[0] + boardSize[1]));
  return setAttributes(createElement('circle'), {
    stroke: brush.color,
    'stroke-width': widths[current ? 0 : 1],
    fill: 'none',
    opacity: opacity(brush, current),
    cx: o[0],
    cy: o[1],
    r: radius - widths[1] / 2
  });
}

function renderArrow(brush: DrawBrush, orig: cg.Pos, dest: cg.Pos, current: boolean, shorten: boolean, bounds: ClientRect, boardSize: cg.BoardSize): SVGElement {
  const m = arrowMargin(bounds, shorten && !current, boardSize),
  a = pos2px(orig, bounds, boardSize),
  b = pos2px(dest, bounds, boardSize),
  dx = b[0] - a[0],
  dy = b[1] - a[1],
  angle = Math.atan2(dy, dx),
  xo = Math.cos(angle) * m,
  yo = Math.sin(angle) * m;
  return setAttributes(createElement('line'), {
    stroke: brush.color,
    'stroke-width': lineWidth(brush, current, bounds, boardSize),
    'stroke-linecap': 'round',
    'marker-end': 'url(#arrowhead-' + brush.key + ')',
    opacity: opacity(brush, current),
    x1: a[0],
    y1: a[1],
    x2: b[0] - xo,
    y2: b[1] - yo
  });
}

function renderPiece(baseUrl: string, pos: cg.Pos, piece: DrawShapePiece, bounds: ClientRect, boardSize: cg.BoardSize): SVGElement {
  const o = pos2px(pos, bounds, boardSize),
  size = bounds.width / boardSize[0] * (piece.scale || 1),
  name = piece.color[0] + piece.role[0].toUpperCase();
  return setAttributes(createElement('image'), {
    className: `${piece.role} ${piece.color}`,
    x: o[0] - size / 2,
    y: o[1] - size / 2,
    width: size,
    height: size,
    href: baseUrl + name + '.svg'
  });
}

function renderMarker(brush: DrawBrush): SVGElement {
  const marker = setAttributes(createElement('marker'), {
    id: 'arrowhead-' + brush.key,
    orient: 'auto',
    markerWidth: 4,
    markerHeight: 8,
    refX: 2.05,
    refY: 2.01
  });
  marker.appendChild(setAttributes(createElement('path'), {
    d: 'M0,0 V4 L3,2 Z',
    fill: brush.color
  }));
  marker.setAttribute('cgKey', brush.key);
  return marker;
}

function setAttributes(el: SVGElement, attrs: { [key: string]: any }): SVGElement {
  for (let key in attrs) el.setAttribute(key, attrs[key]);
  return el;
}

function orient(pos: cg.Pos, color: cg.Color, boardSize: cg.BoardSize): cg.Pos {
  return color === 'white' ? pos : [(boardSize[0] / 2 + 1) - pos[0], (boardSize[1] + 1) - pos[1]];
}

function makeCustomBrush(base: DrawBrush, modifiers: DrawModifiers): DrawBrush {
  const brush: Partial<DrawBrush> = {
    color: base.color,
    opacity: Math.round(base.opacity * 10) / 10,
    lineWidth: Math.round(modifiers.lineWidth || base.lineWidth)
  };
  brush.key = [base.key, modifiers.lineWidth].filter(x => x).join('');
  return brush as DrawBrush;
}

function circleWidth(bounds: ClientRect, boardSize: cg.BoardSize): [number, number] {
  const factor = 512 * boardSize[0] / 10, base = bounds.width / factor;
  return [3 * base, 4 * base];
}

function lineWidth(brush: DrawBrush, current: boolean, bounds: ClientRect, boardSize: cg.BoardSize): number {
  const factor = 512 * boardSize[0] / 10;
  return (brush.lineWidth || 10) * (current ? 0.85 : 1) / factor * bounds.width;
}

function opacity(brush: DrawBrush, current: boolean): number {
  return (brush.opacity || 1) * (current ? 0.9 : 1);
}

function arrowMargin(bounds: ClientRect, shorten: boolean, boardSize: cg.BoardSize): number {
  return boardSize[0] * (shorten ? 2 : 1) / 512 * bounds.width;
}

function pos2px(pos: cg.Pos, bounds: ClientRect, boardSize: cg.BoardSize): cg.NumberPair {
  return [(2 * pos[0] - (pos[1] % 2 !== 0 ? 0.5 : 1.5)) * bounds.width / boardSize[0], (pos[1] - 0.5) * bounds.height / boardSize[1]];
}
