import { drop as cgDrop } from '@lichess-org/chessground/drop';
import { defaults, type State } from '@lichess-org/chessground/state';
import type { MouchEvent } from '@lichess-org/chessground/types';
import assert from 'node:assert/strict';
import { beforeEach, describe, test } from 'node:test';

import type { CrazyPocket, NodeCrazy } from 'lib/tree/types';

import { click, clickedPiece, crazyKeys, drag, isClicked, onDrop, refreshDrop } from '../src/crazy/crazyCtrl';
import type RoundController from '../src/ctrl';

const emptyPocket = (): CrazyPocket => ({ pawn: 0, knight: 0, bishop: 0, rook: 0, queen: 0 });

function makeCtrl(pockets: [CrazyPocket, CrazyPocket], playing = true): RoundController {
  const state = defaults() as State;
  state.dom = {
    bounds: () => ({ left: 0, top: 0, width: 512, height: 512, right: 512, bottom: 512 }) as DOMRectReadOnly,
    redraw: () => {},
    elements: { board: {} as HTMLElement },
  } as unknown as State['dom'];
  const crazyhouse: NodeCrazy = { pockets };
  const ctrl = {
    replaying: () => false,
    isPlaying: () => playing,
    redraw: () => {},
    blindfold: () => false,
    chessground: { state, redrawAll: () => {} },
    data: {
      player: { color: 'white' },
      game: { status: { id: 20, name: 'started' }, player: 'white', turns: 0 },
      crazyhouse,
      pref: { destination: true },
    },
  };
  return ctrl as unknown as RoundController;
}

function makeEvent(attrs: Record<string, string>): MouchEvent {
  return {
    button: 0,
    target: { getAttribute: (name: string) => attrs[name] ?? null },
    stopPropagation: () => {},
    preventDefault: () => {},
  } as unknown as MouchEvent;
}

// Mirrors ui/round/src/crazy/crazyView.ts's real DOM wiring in MoveEvent.ClickOrDrag
// mode (the default "Either" pref): mousedown fires drag() *and* the subsequent
// native click event fires click() - both listeners are bound to the same pocket
// element. Exercising just click() in isolation misses bugs from that interaction
// (see the "toggle off" test below).
function pocketClick(ctrl: RoundController, attrs: Record<string, string>): void {
  drag(ctrl, makeEvent(attrs));
  click(ctrl, makeEvent(attrs));
}

function boardClickAt(state: State, key: string): void {
  // Place the board bounds so that key a1 (bottom-left, white POV) is at (0, 448)-(64,512),
  // matching chessground's own square-size math for an 8x8 board with our 512px bounds.
  const file = key.charCodeAt(0) - 'a'.charCodeAt(0);
  const rank = key.charCodeAt(1) - '1'.charCodeAt(0);
  const x = file * 64 + 32;
  const y = (7 - rank) * 64 + 32;
  cgDrop(state, { clientX: x, clientY: y } as unknown as MouchEvent);
}

describe('crazyhouse click-click drops', () => {
  // crazyCtrl.ts's `clickedPiece`/`crazyKeys` are module-level singletons (mirroring
  // production, where there's exactly one board per page); reset them between tests
  // so a previous test's selection can't leak into the next one.
  beforeEach(() => {
    crazyKeys.length = 0;
    onDrop(makeCtrl([emptyPocket(), emptyPocket()]));
  });

  test('clicking a pocket piece arms chessground dropmode', () => {
    const ctrl = makeCtrl([{ ...emptyPocket(), knight: 1 }, emptyPocket()]);
    click(ctrl, makeEvent({ 'data-role': 'knight', 'data-color': 'white', 'data-nb': '1' }));

    assert.equal(isClicked('white', 'knight'), true);
    assert.equal(ctrl.chessground.state.dropmode.active, true);
    assert.deepEqual(ctrl.chessground.state.dropmode.piece, { color: 'white', role: 'knight' });
  });

  test('arming a piece highlights every empty square as a move-dest', () => {
    const ctrl = makeCtrl([{ ...emptyPocket(), knight: 1 }, emptyPocket()]);
    click(ctrl, makeEvent({ 'data-role': 'knight', 'data-color': 'white', 'data-nb': '1' }));

    // Starting position: ranks 3-6 are empty, ranks 1/2/7/8 are occupied.
    const custom = ctrl.chessground.state.highlight.custom;
    assert.equal(custom?.size, 32);
    assert.equal(custom?.get('e4'), 'move-dest');
    assert.equal(custom?.get('e2'), undefined);
  });

  test('a pawn is not highlighted as droppable on the back ranks', () => {
    const ctrl = makeCtrl([{ ...emptyPocket(), pawn: 1 }, emptyPocket()]);
    click(ctrl, makeEvent({ 'data-role': 'pawn', 'data-color': 'white', 'data-nb': '1' }));

    // Ranks 1/8 are occupied anyway in the starting position, but a pawn
    // could never be highlighted there even on an otherwise-empty rank.
    const custom = ctrl.chessground.state.highlight.custom;
    assert.equal(custom?.get('e4'), 'move-dest');
    assert.equal(custom?.get('e1'), undefined);
  });

  test('the destination pref off leaves no highlight', () => {
    const ctrl = makeCtrl([{ ...emptyPocket(), knight: 1 }, emptyPocket()]);
    ctrl.data.pref.destination = false;
    click(ctrl, makeEvent({ 'data-role': 'knight', 'data-color': 'white', 'data-nb': '1' }));

    assert.equal(ctrl.chessground.state.highlight.custom, undefined);
  });

  test('deselecting the armed piece clears the highlight', () => {
    const ctrl = makeCtrl([{ ...emptyPocket(), knight: 1 }, emptyPocket()]);
    const attrs = { 'data-role': 'knight', 'data-color': 'white', 'data-nb': '1' };
    click(ctrl, makeEvent(attrs));
    assert.ok(ctrl.chessground.state.highlight.custom?.size);

    click(ctrl, makeEvent(attrs)); // toggle off
    assert.equal(ctrl.chessground.state.highlight.custom, undefined);
  });

  test('clicking a board square completes the drop and disarms dropmode', () => {
    const ctrl = makeCtrl([{ ...emptyPocket(), knight: 1 }, emptyPocket()]);
    click(ctrl, makeEvent({ 'data-role': 'knight', 'data-color': 'white', 'data-nb': '1' }));

    boardClickAt(ctrl.chessground.state, 'e4');
    assert.equal(ctrl.chessground.state.pieces.get('e4')?.role, 'knight');

    // Round's ctrl.ts calls this from the movable.events.afterNewPiece hook (onUserNewPiece).
    onDrop(ctrl);

    assert.equal(clickedPiece, undefined);
    assert.equal(ctrl.chessground.state.dropmode.active, false);
  });

  test('without the fix, dropmode would stay armed and hijack the next board click', () => {
    const ctrl = makeCtrl([{ ...emptyPocket(), knight: 2 }, emptyPocket()]);
    click(ctrl, makeEvent({ 'data-role': 'knight', 'data-color': 'white', 'data-nb': '2' }));
    boardClickAt(ctrl.chessground.state, 'e4');
    // Intentionally NOT calling onDrop() here, to reproduce PR #21247's bug.
    assert.equal(ctrl.chessground.state.dropmode.active, true, 'dropmode stays armed after a drop');

    // A later, unrelated board click would silently place another piece
    // instead of doing a normal move/select - this is the bug onDrop() fixes.
    boardClickAt(ctrl.chessground.state, 'd5');
    assert.equal(ctrl.chessground.state.pieces.get('d5')?.role, 'knight');
  });

  test('clicking the same pocket piece again cancels the selection', () => {
    const ctrl = makeCtrl([{ ...emptyPocket(), knight: 1 }, emptyPocket()]);
    const attrs = { 'data-role': 'knight', 'data-color': 'white', 'data-nb': '1' };
    pocketClick(ctrl, attrs);
    assert.equal(isClicked('white', 'knight'), true);

    // Real production sequence: mousedown (drag()) fires before the click event
    // (click()) for the very same pocket piece. drag() must not clear the
    // selection here, or click()'s toggle-off would just re-arm it.
    pocketClick(ctrl, attrs);
    assert.equal(isClicked('white', 'knight'), false);
    assert.equal(ctrl.chessground.state.dropmode.active, false);
  });

  test('clicking a different pocket piece switches the armed role', () => {
    const ctrl = makeCtrl([{ ...emptyPocket(), knight: 1, bishop: 1 }, emptyPocket()]);
    click(ctrl, makeEvent({ 'data-role': 'knight', 'data-color': 'white', 'data-nb': '1' }));
    click(ctrl, makeEvent({ 'data-role': 'bishop', 'data-color': 'white', 'data-nb': '1' }));

    assert.equal(isClicked('white', 'knight'), false);
    assert.equal(isClicked('white', 'bishop'), true);
    assert.deepEqual(ctrl.chessground.state.dropmode.piece, { color: 'white', role: 'bishop' });
  });

  test('an empty pocket slot (nb=0) does not arm dropmode', () => {
    const ctrl = makeCtrl([emptyPocket(), emptyPocket()]);
    click(ctrl, makeEvent({ 'data-role': 'knight', 'data-color': 'white', 'data-nb': '0' }));

    assert.equal(clickedPiece, undefined);
    assert.equal(ctrl.chessground.state.dropmode.active, false);
  });

  test('starting a real drag clears a pending click-selection', () => {
    const ctrl = makeCtrl([{ ...emptyPocket(), knight: 1, bishop: 1 }, emptyPocket()]);
    click(ctrl, makeEvent({ 'data-role': 'knight', 'data-color': 'white', 'data-nb': '1' }));
    assert.equal(isClicked('white', 'knight'), true);

    drag(ctrl, makeEvent({ 'data-role': 'bishop', 'data-color': 'white', 'data-nb': '1' }));
    assert.equal(clickedPiece, undefined);
    assert.equal(ctrl.chessground.state.dropmode.active, false);
  });

  test('refreshDrop cancels dropmode once the pocket runs out of the selected role', () => {
    const pockets: [CrazyPocket, CrazyPocket] = [{ ...emptyPocket(), knight: 1 }, emptyPocket()];
    const ctrl = makeCtrl(pockets);
    click(ctrl, makeEvent({ 'data-role': 'knight', 'data-color': 'white', 'data-nb': '1' }));
    assert.equal(ctrl.chessground.state.dropmode.active, true);

    pockets[0].knight = 0;
    refreshDrop(ctrl);
    assert.equal(ctrl.chessground.state.dropmode.piece, undefined);
  });
});
