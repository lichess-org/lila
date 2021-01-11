import { State } from './state'
import { unselect, cancelMove, getKeyAtDomPos, whitePov } from './board'
import { eventPosition, isRightButton } from './util'
import * as cg from './types'

export interface DrawShape {
  orig: cg.Key;
  dest?: cg.Key;
  brush: string;
  modifiers?: DrawModifiers;
  piece?: DrawShapePiece;
}

export interface DrawShapePiece {
  role: cg.Role;
  color: cg.Color;
  scale?: number;
}

export interface DrawBrush {
  key: string;
  color: string;
  opacity: number;
  lineWidth: number;
}

export interface DrawBrushes {
  [name: string]: DrawBrush;
}

export interface DrawModifiers {
  lineWidth?: number;
}

export interface Drawable {
  enabled: boolean; // can draw
  visible: boolean; // can view
  eraseOnClick: boolean;
  onChange?: (shapes: DrawShape[]) => void;
  shapes: DrawShape[]; // user shapes
  autoShapes: DrawShape[]; // computer shapes
  current?: DrawCurrent;
  brushes: DrawBrushes;
  // drawable SVG pieces; used for crazyhouse drop
  pieces: {
    baseUrl: string;
  };
  prevSvgHash: string;
  piece?: cg.Piece;
}

export interface DrawCurrent {
  orig: cg.Key; // orig key of drawing
  dest?: cg.Key; // shape dest, or undefined for circle
  piece?: cg.Piece;
  mouseSq?: cg.Key; // square being moused over
  pos: cg.NumberPair; // relative current position
  brush: string; // brush name for shape
}

const brushes = ['green', 'red', 'blue', 'yellow'];

export function start(state: State, e: cg.MouchEvent): void {
  // support one finger touch only
  if (e.touches && e.touches.length > 1) return; /* eslint-disable-line */
  e.stopPropagation();
  e.preventDefault();
  e.ctrlKey ? unselect(state) : cancelMove(state);
  const pos = eventPosition(e)!,
    orig = getKeyAtDomPos(pos, whitePov(state), state.dom.bounds()),
    piece = state.drawable.piece;
  if (!orig) return;
  state.drawable.current = {
    orig,
    pos,
    piece,
    brush: eventBrush(e)
  };
  processDraw(state);
}

export function processDraw(state: State): void {
  requestAnimationFrame(() => {
    const cur = state.drawable.current;
    if (cur) {
      const mouseSq = getKeyAtDomPos(cur.pos, whitePov(state), state.dom.bounds());
      if (mouseSq !== cur.mouseSq) {
        cur.mouseSq = mouseSq;
        cur.dest = mouseSq !== cur.orig ? mouseSq : undefined;
        state.dom.redrawNow();
      }
      processDraw(state);
    }
  });
}

export function move(state: State, e: cg.MouchEvent): void {
  if (state.drawable.current) state.drawable.current.pos = eventPosition(e)!;
}

export function end(state: State): void {
  const cur = state.drawable.current;
  if (cur) {
    if (cur.mouseSq) addShape(state.drawable, cur);
    cancel(state);
  }
}

export function cancel(state: State): void {
  if (state.drawable.current) {
    state.drawable.current = undefined;
    state.dom.redraw();
  }
}

export function clear(state: State): void {
  if (state.drawable.shapes.length) {
    state.drawable.shapes = [];
    state.dom.redraw();
    onChange(state.drawable);
  }
}

function eventBrush(e: cg.MouchEvent): string {
  const modA = (e.shiftKey || e.ctrlKey) && isRightButton(e);
  const modB = e.altKey || e.metaKey || e.getModifierState('AltGraph');
  return brushes[(modA ? 1 : 0) + (modB ? 2 : 0)];
}

function addShape(drawable: Drawable, cur: DrawCurrent): void {
  const sameShape = (s: DrawShape) => s.orig === cur.orig && s.dest === cur.dest;
  const similar = drawable.shapes.find(sameShape);
  if (similar) drawable.shapes = drawable.shapes.filter(s => !sameShape(s));
  if (!similar || similar.brush !== cur.brush) drawable.shapes.push(cur);
  onChange(drawable);
}

function onChange(drawable: Drawable): void {
  if (drawable.onChange) drawable.onChange(drawable.shapes);
}
