import { State } from './state';
import * as drag from './drag';
import * as draw from './draw';
import { cancelDropMode, drop } from './drop';
import { eventPosition, isRightButton } from './util';
import * as cg from './types';
import { getKeyAtDomPos, sentePov } from './board';

type MouchBind = (e: cg.MouchEvent) => void;
type StateMouchBind = (d: State, e: cg.MouchEvent) => void;

export function bindBoard(s: State, boundsUpdated: () => void): void {
  const boardEl = s.dom.elements.board;

  if (!s.dom.relative && s.resizable && 'ResizeObserver' in window) {
    const observer = new (window as any)['ResizeObserver'](boundsUpdated);
    observer.observe(boardEl);
  }

  if (s.viewOnly) return;

  // Cannot be passive, because we prevent touch scrolling and dragging of
  // selected elements.
  const onStart = startDragOrDraw(s);
  boardEl.addEventListener('touchstart', onStart as EventListener, {
    passive: false,
  });
  boardEl.addEventListener('mousedown', onStart as EventListener, {
    passive: false,
  });

  if (s.disableContextMenu || s.drawable.enabled) {
    boardEl.addEventListener('contextmenu', e => e.preventDefault());
  }
}

// returns the unbind function
export function bindDocument(s: State, boundsUpdated: () => void): cg.Unbind {
  const unbinds: cg.Unbind[] = [];

  // Old versions of Edge and Safari do not support ResizeObserver. Send
  // shogiground.resize if a user action has changed the bounds of the board.
  if (!s.dom.relative && s.resizable && !('ResizeObserver' in window)) {
    unbinds.push(unbindable(document.body, 'shogiground.resize', boundsUpdated));
  }

  if (!s.viewOnly) {
    const onmove: MouchBind = dragOrDraw(s, drag.move, draw.move);
    const onend: MouchBind = dragOrDraw(s, drag.end, draw.end);

    for (const ev of ['touchmove', 'mousemove']) unbinds.push(unbindable(document, ev, onmove));
    for (const ev of ['touchend', 'mouseup']) unbinds.push(unbindable(document, ev, onend));

    const onScroll = () => s.dom.bounds.clear();
    unbinds.push(unbindable(document, 'scroll', onScroll, { capture: true, passive: true }));
    unbinds.push(unbindable(window, 'resize', onScroll, { passive: true }));
  }

  return () => unbinds.forEach(f => f());
}

function unbindable(el: EventTarget, eventName: string, callback: MouchBind, options?: any): cg.Unbind {
  el.addEventListener(eventName, callback as EventListener, options);
  return () => el.removeEventListener(eventName, callback as EventListener, options);
}

function startDragOrDraw(s: State): MouchBind {
  return e => {
    if (s.draggable.current) drag.cancel(s);
    else if (s.drawable.current) draw.cancel(s);
    else if (e.shiftKey || isRightButton(e)) {
      if (s.drawable.enabled) draw.start(s, e);
    } else if (!s.viewOnly) {
      if (s.dropmode.active && !squareOccupied(s, e)) drop(s, e);
      else {
        cancelDropMode(s);
        drag.start(s, e);
      }
    }
  };
}

function dragOrDraw(s: State, withDrag: StateMouchBind, withDraw: StateMouchBind): MouchBind {
  return e => {
    if (s.drawable.current) {
      if (s.drawable.enabled) withDraw(s, e);
    } else if (!s.viewOnly) withDrag(s, e);
  };
}

function squareOccupied(s: State, e: cg.MouchEvent): boolean {
  const position = eventPosition(e);
  const dest = position && getKeyAtDomPos(position, sentePov(s), s.dom.bounds());
  if (dest && s.pieces.has(dest)) return true;
  return false;
}
