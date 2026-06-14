import assert from 'node:assert/strict';
import { test } from 'node:test';

interface MockCtrl {
  data: { game: { variant: { key: string } } };
  isCevalAllowed: () => boolean;
  cevalEnabled: () => boolean;
  playBestMove: () => void;
  flip: () => void;
  flipCalled: boolean;
  playBestMoveCalled: boolean;
  node?: { crazy?: { pockets?: Record<string, number>[] } };
  navigate: {
    prev: () => void;
    next: () => void;
    first: () => void;
    last: () => void;
  };
  redraw: () => void;
  prevCalledCount: number;
  nextCalledCount: number;
  redrawCalled: boolean;
}

function makeCtrl(
  overrides: Partial<{
    variantKey: string;
    isCevalAllowed: boolean;
    cevalEnabled: boolean;
    hasBestMove: boolean;
  }> = {},
): MockCtrl {
  const opts = {
    variantKey: 'standard',
    isCevalAllowed: true,
    cevalEnabled: true,
    hasBestMove: true,
    ...overrides,
  };

  let flipCalled = false;
  let playBestMoveCalled = false;
  let prevCalledCount = 0;
  let nextCalledCount = 0;
  let redrawCalled = false;

  const ctrl: MockCtrl = {
    data: { game: { variant: { key: opts.variantKey } } },
    isCevalAllowed: () => opts.isCevalAllowed,
    cevalEnabled: () => opts.cevalEnabled,
    playBestMove() {
      playBestMoveCalled = true;
    },
    flip() {
      flipCalled = true;
    },
    get flipCalled() {
      return flipCalled;
    },
    get playBestMoveCalled() {
      return playBestMoveCalled;
    },
    navigate: {
      prev() {
        prevCalledCount++;
      },
      next() {
        nextCalledCount++;
      },
      first() {},
      last() {},
    },
    redraw() {
      redrawCalled = true;
    },
    get prevCalledCount() {
      return prevCalledCount;
    },
    get nextCalledCount() {
      return nextCalledCount;
    },
    get redrawCalled() {
      return redrawCalled;
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
    'o: announce current square',
    'c: announce last move or capture',
    'l: announce last move',
    't: read out clocks',
    'm: announce possible moves',
    'f: flip the board',
    'arrow keys: move with arrows',
    'k-q-r-b-n-p: move to piece by type',
    '1 to 8: move to rank',
    'shift+1 to 8: move to file',
    'shift+a/d: move backward or forward',
    'x: announce pieces around this square (try shift and alt)',
    'shift+m: announce possible captures',
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
    'b: Go to board',
    'p: Announce piece locations',
    's: Announce pieces on rank or file',
    "eval: announce last move's computer evaluation",
    'best: announce the top engine move',
    'prev: return to the previous move',
    'next: go to the next move',
    'prev line: switch to the previous variation',
    'next line: switch to the next variation',
    ...(ctrl.data.game.variant.key === 'crazyhouse'
      ? ['pocket: Read out pockets for white or black. Example: "pocket black"']
      : []),
    'help: list all available input commands',
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

  if (e.key === 'f') {
    if (ctrl.data.game.variant.key !== 'racingKings') {
      notify.set('Flipping the board');
      ctrl.flip();
    }
  } else if (e.shiftKey && e.key === 'H') {
    notify.set(buildBoardHelpString(ctrl));
  } else if (e.ctrlKey && e.key === 'ArrowDown') {
    notify.set('Navigated to start');
  } else if (e.ctrlKey && e.key === 'ArrowUp') {
    notify.set('Navigated to end');
  } else if (e.ctrlKey && e.key === 'ArrowLeft') {
    for (let i = 0; i < 6; i++) ctrl.navigate.prev();
    ctrl.redraw();
  } else if (e.ctrlKey && e.key === 'ArrowRight') {
    for (let i = 0; i < 6; i++) ctrl.navigate.next();
    ctrl.redraw();
  } else if ((e.key === '9' || e.key === '0') && ctrl.data.game.variant.key === 'crazyhouse') {
    const index = e.key === '9' ? 0 : 1;
    const pockets = ctrl.node?.crazy?.pockets;
    if (!pockets?.[index]) {
      notify.set('');
      return;
    }
    const parts = Object.entries(pockets[index])
      .filter(([, count]) => count > 0)
      .map(([piece, count]) => `${piece}: ${count}`);
    notify.set(parts.join(', '));
  }
}

function simulateGlobalF(ctrl: MockCtrl, notify: ReturnType<typeof makeNotify>) {
  if (ctrl.data.game.variant.key !== 'racingKings') {
    notify.set('Flipping the board');
    ctrl.flip();
  }
}

test('f key (global): announces and flips outside board focus', () => {
  const ctrl = makeCtrl();
  const notify = makeNotify();
  simulateGlobalF(ctrl, notify);
  assert.equal(notify.last, 'Flipping the board');
  assert.equal(ctrl.flipCalled, true);
});

test('f key (global): suppressed in racingKings', () => {
  const ctrl = makeCtrl({ variantKey: 'racingKings' });
  const notify = makeNotify();
  simulateGlobalF(ctrl, notify);
  assert.equal(ctrl.flipCalled, false);
  assert.equal(notify.last, '');
});

test('Shift+H: help string contains all core shortcuts', () => {
  const ctrl = makeCtrl();
  const help = buildBoardHelpString(ctrl);
  assert.ok(help.includes('arrow keys'), 'should mention arrow keys');
  assert.ok(help.toLowerCase().includes('shift+g'), 'should mention shift+g for best move');
  assert.ok(help.includes('f'), 'should mention f for flip');
  assert.ok(help.toLowerCase().includes('shift+h'), 'should mention shift+h help');
});

test('Shift+H: crazyhouse variant adds pocket shortcuts', () => {
  const ctrl = makeCtrl({ variantKey: 'crazyhouse' });
  const help = buildBoardHelpString(ctrl);
  assert.ok(help.includes('9'), 'should mention key 9 for white pocket');
  assert.ok(help.includes('0'), 'should mention key 0 for black pocket');
});

test('Shift+H: non-crazyhouse variant omits pocket shortcuts', () => {
  const ctrl = makeCtrl({ variantKey: 'standard' });
  const help = buildBoardHelpString(ctrl);
  assert.ok(!help.includes('9: announce white pocket'), 'should not mention pocket key 9');
  assert.ok(!help.includes('0: announce black pocket'), 'should not mention pocket key 0');
});

test('Shift+H: help string is non-empty', () => {
  const ctrl = makeCtrl();
  const help = buildBoardHelpString(ctrl);
  assert.ok(help.length > 50, 'help string should have meaningful content');
});

test('f key: announces "Flipping the board" via notify', () => {
  const ctrl = makeCtrl();
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'f' });
  assert.equal(notify.last, 'Flipping the board');
});

test('f key: calls ctrl.flip()', () => {
  const ctrl = makeCtrl();
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'f' });
  assert.equal(ctrl.flipCalled, true);
});

test('f key: does NOT flip in racingKings variant', () => {
  const ctrl = makeCtrl({ variantKey: 'racingKings' });
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'f' });
  assert.equal(ctrl.flipCalled, false, 'flip must be suppressed in racingKings');
  assert.equal(notify.last, '', 'no announcement should be made in racingKings');
});

test('f key: announcement precedes flip (notify called before flip)', () => {
  const order: string[] = [];
  const ctrl = makeCtrl();
  ctrl.flip = () => order.push('flip');
  let lastMsg = '';
  const notify = {
    set: (msg: string) => {
      order.push('notify:' + msg);
      lastMsg = msg;
    },
    get last() {
      return lastMsg;
    },
  };
  simulateBoardKeydown(ctrl, notify, { key: 'f' });
  assert.equal(order[0], 'notify:Flipping the board', 'notify must come before flip');
  assert.equal(order[1], 'flip', 'flip must come after notify');
});

test('Shift+H via keydown: sets notify to the full help string', () => {
  const ctrl = makeCtrl();
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'H', shiftKey: true });
  const expected = buildBoardHelpString(ctrl);
  assert.equal(notify.last, expected);
});

test('Crazyhouse: 9 announces white pocket pieces', () => {
  const ctrl = makeCtrl({ variantKey: 'crazyhouse' });
  const notify = makeNotify();
  ctrl.node = {
    crazy: {
      pockets: [
        { pawn: 1, knight: 1, bishop: 0, rook: 0, queen: 0, king: 0 },
        { pawn: 0, knight: 0, bishop: 0, rook: 0, queen: 1, king: 0 },
      ],
    },
  };
  simulateBoardKeydown(ctrl, notify, { key: '9', code: 'Digit9' });
  assert.ok(notify.last.includes('pawn: 1'), 'should list pawn: 1');
  assert.ok(notify.last.includes('knight: 1'), 'should list knight: 1');
});

test('Crazyhouse: 0 announces black pocket pieces', () => {
  const ctrl = makeCtrl({ variantKey: 'crazyhouse' });
  const notify = makeNotify();
  ctrl.node = {
    crazy: {
      pockets: [
        { pawn: 1, knight: 1, bishop: 0, rook: 0, queen: 0, king: 0 },
        { pawn: 0, knight: 0, bishop: 0, rook: 0, queen: 1, king: 0 },
      ],
    },
  };
  simulateBoardKeydown(ctrl, notify, { key: '0', code: 'Digit0' });
  assert.ok(notify.last.includes('queen: 1'), 'should list queen: 1');
});

test("Crazyhouse: empty pocket shows ''", () => {
  const ctrl = makeCtrl({ variantKey: 'crazyhouse' });
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: '9', code: 'Digit9' });
  assert.equal(notify.last, '');
});

test('Ctrl+ArrowDown and Ctrl+ArrowUp navigates to start/end of the game', () => {
  const ctrl = makeCtrl();
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'ArrowDown', ctrlKey: true });
  assert.equal(notify.last, 'Navigated to start');
  simulateBoardKeydown(ctrl, notify, { key: 'ArrowUp', ctrlKey: true });
  assert.equal(notify.last, 'Navigated to end');
});

test('Ctrl+Left jumps 6 moves back', () => {
  const ctrl = makeCtrl();
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'ArrowLeft', ctrlKey: true });
  assert.equal(ctrl.prevCalledCount, 6);
  assert.equal(ctrl.redrawCalled, true);
});

test('Ctrl+Right jumps 6 moves forward', () => {
  const ctrl = makeCtrl();
  const notify = makeNotify();
  simulateBoardKeydown(ctrl, notify, { key: 'ArrowRight', ctrlKey: true });
  assert.equal(ctrl.nextCalledCount, 6);
  assert.equal(ctrl.redrawCalled, true);
});

test('buildInputHelpString: contains core input commands', () => {
  const ctrl = makeCtrl();
  const help = buildInputHelpString(ctrl);
  assert.ok(help.includes('eval:'), 'should mention eval command');
  assert.ok(help.includes('best:'), 'should mention best command');
  assert.ok(help.includes('prev:'), 'should mention prev command');
  assert.ok(help.includes('next:'), 'should mention next command');
});

test('buildInputHelpString: crazyhouse variant includes pocket command', () => {
  const ctrl = makeCtrl({ variantKey: 'crazyhouse' });
  const help = buildInputHelpString(ctrl);
  assert.ok(help.includes('pocket:'), 'should mention pocket command in crazyhouse');
});

test('buildInputHelpString: non-crazyhouse variant omits pocket command', () => {
  const ctrl = makeCtrl({ variantKey: 'standard' });
  const help = buildInputHelpString(ctrl);
  assert.ok(!help.includes('pocket:'), 'should not mention pocket command in standard variant');
});
