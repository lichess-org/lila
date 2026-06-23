import type { DrawShape } from '@lichess-org/chessground/draw';
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { compute } from '../src/autoShape';
import type AnalyseCtrl from '../src/ctrl';

function makeCtrl(retro?: { isSolving(): boolean; showBadNode(): { uci: Uci } | undefined }): AnalyseCtrl {
  let explorerHover: { fen: FEN; uci: Uci } | null = null;

  return {
    node: {
      fen: '4k3/4n3/8/8/8/4R3/8/K7 w - - 0 1',
      uci: 'e2e4',
      san: 'e4',
      glyphs: [{ symbol: '?' }],
    },
    explorer: {
      hovering(value?: { fen: FEN; uci: Uci } | null) {
        if (arguments.length > 0) explorerHover = value ?? null;
        return explorerHover;
      },
    },
    ceval: {
      hovering: () => null,
      search: { multiPv: 0 },
    },
    fork: {
      hover: () => {},
    },
    settings: {
      showManeuverMoveArrows: false,
    },
    practice: undefined,
    retro,
    showBestMoveArrows: () => false,
    showEvaluation: () => false,
    isCevalAllowed: () => true,
    threatMode: () => false,
    showMoveAnnotations: () => true,
    showVariationArrows: () => false,
    visibleChildren: () => [],
    motifEnabled: () => true,
    motif: {
      detectPins: () => [{ pinned: 52 }],
      detectUndefended: () => [{ square: 43 }],
      detectCheckable: () => [{ king: 60 }],
    },
  } as unknown as AnalyseCtrl;
}

const annotationShape = (shape: DrawShape): boolean => Boolean(shape.customSvg || shape.label);

test('hides move annotations and motifs while retrospect is solving', () => {
  const shapes = compute(
    makeCtrl({
      isSolving: () => true,
      showBadNode: () => undefined,
    }),
  );

  assert.equal(shapes.some(annotationShape), false);
});

test('still shows the bad move arrow while retrospect is solving', () => {
  const shapes = compute(
    makeCtrl({
      isSolving: () => true,
      showBadNode: () => ({ uci: 'e2e4' }),
    }),
  );

  assert.deepEqual(shapes, [{ orig: 'e2', dest: 'e4', brush: 'paleRed', modifiers: { lineWidth: 8 } }]);
});

test('keeps move annotations and motifs outside retrospect solving', () => {
  const shapes = compute(
    makeCtrl({
      isSolving: () => false,
      showBadNode: () => undefined,
    }),
  );

  assert.equal(shapes.some(annotationShape), true);
});
