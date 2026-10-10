import type { DrawShape } from '@lichess-org/chessground/draw';
import type * as cg from '@lichess-org/chessground/types';
import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import { PromotionCtrl } from '../src/game/promotion';

function makeGround(pieces: Array<[cg.Key, cg.Piece]> = [['e7', { color: 'white', role: 'pawn' }]]) {
  const state = {
    pieces: new Map<cg.Key, cg.Piece>(pieces),
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

  test('dismiss ignores stale promotion choices fired during hook cleanup', () => {
    const { ground, autoShapes } = makeGround();
    let submits = 0;
    const ctrl = new PromotionCtrl(
      f => f(ground),
      () => {},
      () => {},
    );

    assert.equal(
      ctrl.start('e7', 'e8', {
        submit: () => {
          submits++;
        },
        show: (activeCtrl, roles) => {
          if (roles === false) activeCtrl.finish('queen');
        },
      }),
      true,
    );
    assert.ok(ctrl.view());

    assert.equal(ctrl.dismiss(), true);
    assert.equal(submits, 0);
    assert.equal(autoShapes().length, 0);
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

  test('atomic: a pawn capturing on the last rank promotes to queen without asking', () => {
    const { ground } = makeGround([['e8', { color: 'white', role: 'pawn' }]]);
    const submitted: Role[] = [];
    const ctrl = new PromotionCtrl(
      f => f(ground),
      () => {},
      () => {},
      () => 'atomic',
    );

    assert.equal(
      ctrl.start(
        'e7',
        'e8',
        { submit: (_orig, _dest, role) => submitted.push(role) },
        { premove: false, captured: { color: 'black', role: 'rook' } },
      ),
      true,
    );
    assert.deepEqual(submitted, ['queen']);
    assert.equal(ctrl.view(), undefined);
  });

  test('atomic: a pawn push to the last rank still promotes', () => {
    const { ground } = makeGround([['e8', { color: 'white', role: 'pawn' }]]);
    const ctrl = new PromotionCtrl(
      f => f(ground),
      () => {},
      () => {},
      () => 'atomic',
    );

    assert.equal(ctrl.start('e7', 'e8', { submit: () => {} }, { premove: false }), true);
    assert.ok(ctrl.view());
  });

  test('atomic: a premoved capture on the last rank pre-promotes to queen', () => {
    const { ground, autoShapes } = makeGround([
      ['e7', { color: 'white', role: 'pawn' }],
      ['d8', { color: 'black', role: 'rook' }],
    ]);
    const ctrl = new PromotionCtrl(
      f => f(ground),
      () => {},
      () => {},
      () => 'atomic',
    );

    assert.equal(
      ctrl.start('e7', 'd8', { submit: () => assert.fail('a premove should not submit yet') }),
      true,
    );
    assert.equal(ctrl.view(), undefined);
    assert.deepEqual(
      autoShapes().map(s => s.piece?.role),
      ['queen'],
    );
  });

  test('the variant is read when the move is played, not when the control is built', () => {
    const { ground } = makeGround([['e8', { color: 'white', role: 'pawn' }]]);
    let variant: VariantKey = 'standard';
    const submitted: Role[] = [];
    const ctrl = new PromotionCtrl(
      f => f(ground),
      () => {},
      () => {},
      () => variant,
    );
    const capture = { premove: false, captured: { color: 'black', role: 'rook' } } as const;

    assert.equal(ctrl.start('e7', 'e8', { submit: () => {} }, capture), true);
    assert.ok(ctrl.view(), 'standard offers a choice');
    ctrl.dismiss();

    variant = 'atomic';
    assert.equal(
      ctrl.start('e7', 'e8', { submit: (_orig, _dest, role) => submitted.push(role) }, capture),
      true,
    );
    assert.deepEqual(submitted, ['queen']);
    assert.equal(ctrl.view(), undefined, 'atomic does not');
  });

  test('a capture on the last rank still promotes outside atomic', () => {
    const { ground } = makeGround([['e8', { color: 'white', role: 'pawn' }]]);
    const ctrl = new PromotionCtrl(
      f => f(ground),
      () => {},
      () => {},
    );

    assert.equal(
      ctrl.start(
        'e7',
        'e8',
        { submit: () => {} },
        { premove: false, captured: { color: 'black', role: 'rook' } },
      ),
      true,
    );
    assert.ok(ctrl.view());
  });
});
