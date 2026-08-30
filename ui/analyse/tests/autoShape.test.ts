import assert from 'node:assert/strict';
import { test } from 'node:test';

import type { TreeNode } from 'lib/tree/types';

import { compute } from '../src/autoShape';
import type AnalyseCtrl from '../src/ctrl';

const fen = '8/8/8/8/8/8/8/8 w - - 0 1';

test('renders a Crazyhouse drop variation on its destination square', () => {
  const children = [
    { uci: 'P@f5', fen, ply: 1, children: [] },
    { uci: 'e2e4', fen, ply: 1, children: [] },
  ] as unknown as TreeNode[];
  const ctrl = {
    node: { fen, children } as TreeNode,
    practice: undefined,
    explorer: { hovering: () => null },
    ceval: { hovering: () => null },
    fork: { hover() {}, selectedIndex: 0 },
    chessground: { state: { drawable: { brushes: {} } } },
    visibleChildren: () => children,
    isCevalAllowed: () => false,
    showVariationArrows: () => true,
    showMoveAnnotations: () => false,
    motifEnabled: () => false,
  } as unknown as AnalyseCtrl;

  assert.deepEqual(
    compute(ctrl).map(({ orig, dest }) => [orig, dest]),
    [
      ['f5', undefined],
      ['e2', 'e4'],
    ],
  );
});
