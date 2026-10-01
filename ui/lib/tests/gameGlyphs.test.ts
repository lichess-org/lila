import type { DrawShape } from '@lichess-org/chessground/draw';
import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import { annotationShapes, endgameGlyphs, glyphToSvg } from '../src/game/glyphs';
import type { TreeNode } from '../src/tree/types';

const winningAnnotation = {
  uci: 'e1e2',
  san: 'Ke2',
  glyphs: [{ id: 16, name: 'White is winning', symbol: '+-' }],
} as TreeNode;

describe('annotationShapes', () => {
  test('stacks a winning annotation after an endgame win glyph on the same square', () => {
    const existingShapes: DrawShape[] = [
      { orig: 'e2', customSvg: { html: endgameGlyphs.win(0), center: 'orig' } },
    ];

    const [annotation] = annotationShapes(winningAnnotation, existingShapes);

    assert.equal(annotation.orig, 'e2');
    assert.equal(annotation.customSvg?.html, glyphToSvg['+-'](1));
  });

  test('keeps the first annotation at the default stack position', () => {
    const [annotation] = annotationShapes(winningAnnotation);

    assert.equal(annotation.customSvg?.html, glyphToSvg['+-'](0));
  });
});
