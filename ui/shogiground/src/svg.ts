import { State } from './state'
import { key2pos, createEl } from './util'
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

type CustomBrushes = Map<string, DrawBrush>; // by hash

type ArrowDests = Map<cg.Key, number>; // how many arrows land on a square

type Hash = string;

export function renderSvg(state: State, root: SVGElement): void {

  const d = state.drawable,
    curD = d.current,
    cur = curD && curD.mouseSq ? curD as DrawShape : undefined,
    arrowDests: ArrowDests = new Map(),
    bounds = state.dom.bounds();

  for (const s of d.shapes.concat(d.autoShapes).concat(cur ? [cur] : [])) {
    if (s.dest) arrowDests.set(s.dest, (arrowDests.get(s.dest) || 0) + 1);
  }

  const shapes: Shape[] = d.shapes.concat(d.autoShapes).map((s: DrawShape) => {
    return {
      shape: s,
      current: false,
      hash: shapeHash(s, arrowDests, false, bounds)
    };
  });
  if (cur) shapes.push({
    shape: cur,
    current: true,
    hash: shapeHash(cur, arrowDests, true, bounds)
  });

  const fullHash = shapes.map(sc => sc.hash).join(';');
  if (fullHash === state.drawable.prevSvgHash) return;
  state.drawable.prevSvgHash = fullHash;

  const defsEl = root.firstChild as SVGElement;

  syncDefs(d, shapes, defsEl);
  syncShapes(state, shapes, d.brushes, arrowDests, root, defsEl);
}

// append only. Don't try to update/remove.
function syncDefs(d: Drawable, shapes: Shape[], defsEl: SVGElement) {
  const brushes: CustomBrushes = new Map();
  let brush: DrawBrush;
  for (const s of shapes) {
    if (s.shape.dest) {
      brush = d.brushes[s.shape.brush];
      if (s.shape.modifiers) brush = makeCustomBrush(brush, s.shape.modifiers);
      brushes.set(brush.key, brush);
    }
  }
  const keysInDom = new Set();
  let el: SVGElement | undefined = defsEl.firstChild as SVGElement;
  while (el) {
    keysInDom.add(el.getAttribute('cgKey'));
    el = el.nextSibling as SVGElement | undefined;
  }
  for (const [key, brush] of brushes.entries()) {
    if (!keysInDom.has(key)) defsEl.appendChild(renderMarker(brush));
  }
}

// append and remove only. No updates.
function syncShapes(state: State, shapes: Shape[], brushes: DrawBrushes, arrowDests: ArrowDests, root: SVGElement, defsEl: SVGElement): void {
  const bounds = state.dom.bounds(),
    hashesInDom = new Map(), // by hash
    toRemove: SVGElement[] = [];
  for (const sc of shapes) hashesInDom.set(sc.hash, false);
  let el: SVGElement | undefined = defsEl.nextSibling as SVGElement, elHash: Hash;
  while (el) {
    elHash = el.getAttribute('cgHash') as Hash;
    // found a shape element that's here to stay
    if (hashesInDom.has(elHash)) hashesInDom.set(elHash, true);
    // or remove it
    else toRemove.push(el);
    el = el.nextSibling as SVGElement | undefined;
  }
  // remove old shapes
  for (const el of toRemove) root.removeChild(el);
  // insert shapes that are not yet in dom
  for (const sc of shapes) {
    if (!hashesInDom.get(sc.hash)) root.appendChild(renderShape(state, sc, brushes, arrowDests, bounds));
  }
}

function shapeHash({ orig, dest, brush, piece, modifiers }: DrawShape, arrowDests: ArrowDests, current: boolean, bounds: ClientRect): Hash {
  return [bounds.width, bounds.height, current, orig, dest, brush, dest && (arrowDests.get(dest) || 0) > 1,
  piece && pieceHash(piece),
  modifiers && modifiersHash(modifiers)
  ].filter(x => x).join(',');
}

function pieceHash(piece: DrawShapePiece): Hash {
  return [piece.color, piece.role, piece.scale].filter(x => x).join(',');
}

function modifiersHash(m: DrawModifiers): Hash {
  return '' + (m.lineWidth || '');
}

function renderShape(state: State, { shape, current, hash }: Shape, brushes: DrawBrushes, arrowDests: ArrowDests, bounds: ClientRect): SVGElement {
  let el: SVGElement;
  if (shape.piece) el = renderPiece(
    orient(key2pos(shape.orig), state.orientation),
    shape.piece,
    bounds);
  else {
    const orig = orient(key2pos(shape.orig), state.orientation);
    if (shape.dest) {
      let brush: DrawBrush = brushes[shape.brush];
      if (shape.modifiers) brush = makeCustomBrush(brush, shape.modifiers);
      el = renderArrow(
        brush,
        orig,
        orient(key2pos(shape.dest), state.orientation),
        current,
        (arrowDests.get(shape.dest) || 0) > 1,
        bounds);
    }
    else el = renderCircle(brushes[shape.brush], orig, current, bounds);
  }
  el.setAttribute('cgHash', hash);
  return el;
}

function renderCircle(brush: DrawBrush, pos: cg.Pos, current: boolean, bounds: ClientRect): SVGElement {
  const o = pos2px(pos, bounds),
    widths = circleWidth(bounds),
    radius = (bounds.width + bounds.height) / 36;
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

function renderArrow(brush: DrawBrush, orig: cg.Pos, dest: cg.Pos, current: boolean, shorten: boolean, bounds: ClientRect): SVGElement {
  const m = arrowMargin(bounds, shorten && !current),
    a = pos2px(orig, bounds),
    b = pos2px(dest, bounds),
    dx = b[0] - a[0],
    dy = b[1] - a[1],
    angle = Math.atan2(dy, dx),
    xo = Math.cos(angle) * m,
    yo = Math.sin(angle) * m;
  return setAttributes(createElement('line'), {
    stroke: brush.color,
    'stroke-width': lineWidth(brush, current, bounds),
    'stroke-linecap': 'round',
    'marker-end': 'url(#arrowhead-' + brush.key + ')',
    opacity: opacity(brush, current),
    x1: a[0],
    y1: a[1],
    x2: b[0] - xo,
    y2: b[1] - yo
  });
}

function renderPiece(pos: cg.Pos, piece: DrawShapePiece, bounds: ClientRect): SVGElement {
  const o = pos2px(pos, bounds),
    size = bounds.width / 9 * (piece.scale || 1);
  let el = setAttributes(createElement('foreignObject'), {
    className: `${piece.role} ${piece.color}`,
    x: o[0] - size / 2,
    y: o[1] - size / 2,
    width: size,
    height: size
  });
  let pieceEl = createEl('piece', `${piece.color} ${piece.role}`);
  pieceEl.style.width = "100%";
  pieceEl.style.height = "100%";
  el.append(pieceEl);
  return el;
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
  for (const key in attrs) el.setAttribute(key, attrs[key]);
  return el;
}

function orient(pos: cg.Pos, color: cg.Color): cg.Pos {
  return color === 'white' ? pos : [8 - pos[0], 8 - pos[1]];
}

function makeCustomBrush(base: DrawBrush, modifiers: DrawModifiers): DrawBrush {
  return {
    color: base.color,
    opacity: Math.round(base.opacity * 10) / 10,
    lineWidth: Math.round(modifiers.lineWidth || base.lineWidth),
    key: [base.key, modifiers.lineWidth].filter(x => x).join(''),
  };
}

function circleWidth(bounds: ClientRect): [number, number] {
  const base = bounds.width / 512;
  return [3 * base, 4 * base];
}

function lineWidth(brush: DrawBrush, current: boolean, bounds: ClientRect): number {
  return (brush.lineWidth || 10) * (current ? 0.85 : 1) / 512 * bounds.width;
}

function opacity(brush: DrawBrush, current: boolean): number {
  return (brush.opacity || 1) * (current ? 0.9 : 1);
}

function arrowMargin(bounds: ClientRect, shorten: boolean): number {
  return (shorten ? 20 : 10) / 512 * bounds.width;
}

function pos2px(pos: cg.Pos, bounds: ClientRect): cg.NumberPair {
  return [(pos[0] + 0.5) * bounds.width / 9, (8.5 - pos[1]) * bounds.height / 9];
}
