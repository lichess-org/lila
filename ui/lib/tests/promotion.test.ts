import type { DrawShape } from '@lichess-org/chessground/draw';
import type * as cg from '@lichess-org/chessground/types';
import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import { PromotionCtrl } from '../src/game/promotion';

function makeGround() {
  const state = {
    pieces: new Map<cg.Key, cg.Piece>([['e7', { color: 'white', role: 'pawn' }]]),
    turnColor: 'black' as cg.Color,
    orientation: 'white' as cg.Color,
  };
  let autoShapes: DrawShape[] = [];
  const ground = {
    state,
    setAutoShapes(shapes: DrawShape[]) {
      autoShapes = shapes;
    },
    setPieces(pieces: Map<cg.Key, cg.Piece | undefined>) {
      for (const [key, piece] of pieces) {
        if (piece) state.pieces.set(key, piece);
        else state.pieces.delete(key);
      }
    },
  } as unknown as CgApi;

  return {
    ground,
    autoShapes: () => autoShapes,
  };
}

describe('promotion control', () => {
  test('dismiss clears a visible promotion choice without running the cancel hook', () => {
    const { ground } = makeGround();
    let redraws = 0;
    let cancels = 0;
    const shownRoles: Array<cg.Role[] | false> = [];
    const ctrl = new PromotionCtrl(
      f => f(ground),
      () => {
        cancels++;
      },
      () => {
        redraws++;
      },
    );

    assert.equal(
      ctrl.start('e7', 'e8', {
        submit: () => assert.fail('promotion should not submit while waiting for a role'),
        show: (_ctrl, roles) => shownRoles.push(roles),
      }),
      true,
    );
    assert.ok(ctrl.view());

    assert.equal(ctrl.dismiss(), true);
    assert.equal(cancels, 0);
    assert.equal(redraws, 2);
    assert.deepEqual(shownRoles.at(-1), false);
    assert.equal(ctrl.view(), undefined);
  });

  test('cancel keeps the existing cancel hook behavior', () => {
    const { ground } = makeGround();
    let cancels = 0;
    const ctrl = new PromotionCtrl(
      f => f(ground),
      () => {
        cancels++;
      },
      () => {},
    );

    assert.equal(ctrl.start('e7', 'e8', { submit: () => assert.fail('promotion should not submit') }), true);

    ctrl.cancel();

    assert.equal(cancels, 1);
    assert.equal(ctrl.view(), undefined);
  });

  test('dismiss clears a stored premove promotion role', () => {
    const { ground, autoShapes } = makeGround();
    let cancels = 0;
    const ctrl = new PromotionCtrl(
      f => f(ground),
      () => {
        cancels++;
      },
      () => {},
    );

    assert.equal(ctrl.start('e7', 'e8', { submit: () => assert.fail('promotion should not submit') }), true);
    ctrl.finish('queen');
    assert.equal(autoShapes().length, 1);

    assert.equal(ctrl.dismiss(), false);
    assert.equal(cancels, 0);
    assert.equal(autoShapes().length, 0);
  });
});
