import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { describe, test } from 'node:test';
import { fileURLToPath } from 'node:url';

import { pieceRoles } from '../src/crazy/crazyCtrl';

// public/piece/cburnett-cursor/ holds copies of the cburnett piece SVGs used
// for the crazyhouse drop cursor (see ui/round/css/_zh.scss), with explicit
// width/height added - Firefox otherwise fails to size an SVG cursor image
// that only has a viewBox. They're kept separate from public/piece/cburnett/
// so this doesn't affect board rendering or other piece-set consumers.
//
// This test makes sure the two stay in sync: if cburnett's artwork is ever
// updated, the cursor copies should be updated identically, not left stale.
const roleLetters: Record<string, string> = { pawn: 'P', knight: 'N', bishop: 'B', rook: 'R', queen: 'Q' };
const openTag = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 45 45">';
const cursorOpenTag = '<svg xmlns="http://www.w3.org/2000/svg" width="45" height="45" viewBox="0 0 45 45">';

const publicPath = (path: string) => fileURLToPath(new URL(`../../../public/${path}`, import.meta.url));

describe('cburnett-cursor SVGs stay in sync with cburnett', () => {
  for (const colorLetter of ['w', 'b']) {
    for (const role of pieceRoles) {
      const roleLetter = roleLetters[role];

      test(`${colorLetter}${roleLetter}`, () => {
        const original = readFileSync(publicPath(`piece/cburnett/${colorLetter}${roleLetter}.svg`), 'utf8');
        const cursor = readFileSync(
          publicPath(`piece/cburnett-cursor/${colorLetter}${roleLetter}.svg`),
          'utf8',
        );

        assert.ok(original.startsWith(openTag), 'unexpected cburnett SVG opening tag - update this test too');
        assert.equal(cursor, cursorOpenTag + original.slice(openTag.length));
      });
    }
  }
});
