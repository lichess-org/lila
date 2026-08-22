import assert from 'node:assert/strict';
import { test } from 'node:test';

interface MockCtrl {
  data: {
    game: { variant: { key: string } };
    steps: Array<{ crazy?: { pockets?: Record<string, number>[] } }>;
  };
  ply: number;
  flip: boolean;
  redrawCalled: boolean;
  redraw: () => void;
}

function makeCtrl(
  overrides: Partial<{
    variantKey: string;
    steps: Array<{ crazy?: { pockets?: Record<string, number>[] } }>;
    ply: number;
  }> = {},
): MockCtrl {
  const opts = {
    variantKey: 'standard',
    steps: [{}, {}, {}, {}], // Default 4 steps mock
    ply: 2,
    ...overrides,
  };

  let redrawCalled = false;

  const ctrl: MockCtrl = {
    data: {
      game: { variant: { key: opts.variantKey } },
      steps: opts.steps,
    },
    ply: opts.ply,
    flip: false,
    get redrawCalled() {
      return redrawCalled;
    },
    redraw() {
      redrawCalled = true;
    },
  };

  return ctrl;
}

function makeNotify() {
  let last = '';
  return {
    set(msg: string) {
      last = msg;
    },
    get last() {
      return last;
    },
  };
}

function buildBoardHelpString(ctrl: MockCtrl): string {
  const parts = [
    'Board command list',
    'i: go to input form',
    'f: flip the board',
    'shift+a/d: move backward or forward',
    'v: announce computer evaluation',
    'ctrl+arrow down/up: go to first or last move of variation',
    'g: announce computer best move',
    'shift+g: play computer best move',
    'alt+shift+a/d: cycle previous or next variation',
    ...(ctrl.data.game.variant.key === 'crazyhouse'
      ? ['9: announce white pocket pieces', '0: announce black pocket pieces']
      : []),
    'shift+h: board command list',
  ];
  return parts.join('. ');
}

function buildInputHelpString(ctrl: MockCtrl): string {
  const parts = [
    'Input command list',
    'help: list all available input commands',
    ...(ctrl.data.game.variant.key === 'crazyhouse' ? ['pocket: format: pocket [white|black]'] : []),
  ];
  return parts.join('. ');
}

function simulateBoardKeydown(
  ctrl: MockCtrl,
  notify: ReturnType<typeof makeNotify>,
  event: { key: string; shiftKey?: boolean; ctrlKey?: boolean; altKey?: boolean; code?: string },
) {
  const e = {
    key: event.key,
    shiftKey: !!event.shiftKey,
    ctrlKey: !!event.ctrlKey,
    altKey: !!event.altKey,
    code: event.code ?? '',
    preventDefault: () => {},
  };

  if (e.shiftKey && e.key === 'H') {
    notify.set(buildBoardHelpString(ctrl));
  } else if ((e.key === '9' || e.key === '0') && ctrl.data.game.variant.key === 'crazyhouse') {
    const step = ctrl.data.steps[ctrl.ply];
    const pockets = step?.crazy?.pockets;
    if (pockets) {
      const idx = e.key === '9' ? 0 : 1;
      const pocket = pockets[idx];
      const parts = Object.entries(pocket)
        .filter(([, count]) => count > 0)
        .map(([piece, count]) => `${piece}: ${count}`);
      notify.set(parts.join(', ') || 'none');
    }
  } else if (e.ctrlKey && e.key === 'ArrowDown') {
    ctrl.ply = 0;
    ctrl.redraw();
  } else if (e.ctrlKey && e.key === 'ArrowUp') {
    ctrl.ply = ctrl.data.steps.length - 1;
    ctrl.redraw();
  } else if (e.ctrlKey && e.key === 'ArrowLeft') {
    ctrl.ply = Math.max(0, ctrl.ply - 6);
    ctrl.redraw();
  } else if (e.ctrlKey && e.key === 'ArrowRight') {
    ctrl.ply = Math.min(ctrl.data.steps.length - 1, ctrl.ply + 6);
    ctrl.redraw();
  } else if (e.key.toLowerCase() === 'f') {
    if (ctrl.data.game.variant.key !== 'racingKings') {
      notify.set('Flipping the board');
      ctrl.flip = !ctrl.flip;
      ctrl.redraw();
    }
  }
}

test('Shift+H via keydown: sets notify to the full help string', () => {
  const ctrl = makeCtrl();
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'H', shiftKey: true });
  const expected = buildBoardHelpString(ctrl);
  assert.equal(notify.last, expected);
});

test('Shift+H: crazyhouse variant adds pocket shortcuts to help string', () => {
  const ctrl = makeCtrl({ variantKey: 'crazyhouse' });
  const help = buildBoardHelpString(ctrl);
  assert.ok(help.includes('9'), 'should mention key 9 for white pocket');
  assert.ok(help.includes('0'), 'should mention key 0 for black pocket');
});

test('Crazyhouse: 9 announces white pocket pieces', () => {
  const steps = [
    {},
    {
      crazy: {
        pockets: [
          { pawn: 2, knight: 1, bishop: 0, rook: 0, queen: 0, king: 0 },
          { pawn: 0, knight: 0, bishop: 0, rook: 0, queen: 1, king: 0 },
        ],
      },
    },
  ];
  const ctrl = makeCtrl({ variantKey: 'crazyhouse', steps, ply: 1 });
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: '9' });
  assert.ok(notify.last.includes('pawn: 2'), 'should list pawn: 2');
  assert.ok(notify.last.includes('knight: 1'), 'should list knight: 1');
});

test('Crazyhouse: 0 announces black pocket pieces', () => {
  const steps = [
    {},
    {
      crazy: {
        pockets: [
          { pawn: 1, knight: 1, bishop: 0, rook: 0, queen: 0, king: 0 },
          { pawn: 0, knight: 0, bishop: 0, rook: 0, queen: 1, king: 0 },
        ],
      },
    },
  ];
  const ctrl = makeCtrl({ variantKey: 'crazyhouse', steps, ply: 1 });
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: '0' });
  assert.ok(notify.last.includes('queen: 1'), 'should list queen: 1');
});

test('Crazyhouse: empty pocket shows "none"', () => {
  const steps = [
    {},
    {
      crazy: {
        pockets: [
          { pawn: 0, knight: 0, bishop: 0, rook: 0, queen: 0, king: 0 },
          { pawn: 0, knight: 0, bishop: 0, rook: 0, queen: 0, king: 0 },
        ],
      },
    },
  ];
  const ctrl = makeCtrl({ variantKey: 'crazyhouse', steps, ply: 1 });
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: '9' });
  assert.equal(notify.last, 'none');
});

test('Non-crazyhouse: 9 or 0 key does not trigger pocket announcements', () => {
  const ctrl = makeCtrl({ variantKey: 'standard', ply: 1 });
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: '9' });
  assert.equal(notify.last, '');
});

test('Ctrl+ArrowLeft jumps 6 plys back', () => {
  const steps = [{}, {}, {}, {}, {}, {}, {}, {}, {}]; // 9 moves
  const ctrl = makeCtrl({ steps, ply: 8 });
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'ArrowLeft', ctrlKey: true });
  assert.equal(ctrl.ply, 2);
  assert.equal(ctrl.redrawCalled, true);
});

test('Ctrl+ArrowDown navigates to the start of the game', () => {
  const ctrl = makeCtrl({ ply: 2 });
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'ArrowDown', ctrlKey: true });
  assert.equal(ctrl.ply, 0);
  assert.equal(ctrl.redrawCalled, true);
});

test('Ctrl+ArrowRight jumps 6 plys forward', () => {
  const steps = [{}, {}, {}, {}, {}, {}, {}, {}, {}]; // 9 moves
  const ctrl = makeCtrl({ steps, ply: 1 });
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'ArrowRight', ctrlKey: true });
  assert.equal(ctrl.ply, 7);
  assert.equal(ctrl.redrawCalled, true);
});

test('Ctrl+ArrowUp navigates to the end of the game', () => {
  const ctrl = makeCtrl({ ply: 1 });
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'ArrowUp', ctrlKey: true });
  assert.equal(ctrl.ply, ctrl.data.steps.length - 1);
  assert.equal(ctrl.redrawCalled, true);
});

test('f key: announces and triggers board flip', () => {
  const ctrl = makeCtrl();
  const notify = makeNotify();
  const initialFlip = ctrl.flip;
  simulateBoardKeydown(ctrl, notify, { key: 'f' });
  assert.equal(notify.last, 'Flipping the board');
  assert.equal(ctrl.flip, !initialFlip);
  assert.equal(ctrl.redrawCalled, true);
});

test('f key: flip is suppressed in racingKings variant', () => {
  const ctrl = makeCtrl({ variantKey: 'racingKings' });
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'f' });
  assert.equal(ctrl.flip, false);
  assert.equal(ctrl.redrawCalled, false);
  assert.equal(notify.last, '');
});

test('buildInputHelpString returns correct help details', () => {
  const ctrl = makeCtrl();
  const help = buildInputHelpString(ctrl);
  assert.ok(help.includes('Input command list'));
  assert.ok(help.includes('help: list all available input commands'));
});

test('buildInputHelpString includes pocket layout for crazyhouse variant', () => {
  const ctrl = makeCtrl({ variantKey: 'crazyhouse' });
  const help = buildInputHelpString(ctrl);
  assert.ok(help.includes('pocket: format: pocket [white|black]'));
});
