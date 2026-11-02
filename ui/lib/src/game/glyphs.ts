import type { DrawShape } from '@lichess-org/chessground/draw';
import * as cgGlyphs from '@lichess-org/chessground/glyph';
import { parseUci, makeSquare, squareRank } from 'chessops/util';

import type { Glyph, TreeNode } from '@/tree/types';

// maximum number of glyphs to show for a given move
const maxGlyphs = 4;

export function annotationShapes(node: TreeNode, existingShapes: DrawShape[] = []): DrawShape[] {
  const { uci, glyphs, san } = node;
  if (uci && san && glyphs) {
    return (
      glyphs
        .slice(0, maxGlyphs)
        .map((glyph: Glyph, idx: number) => {
          const move = parseUci(uci)!;
          const destSquare = san.startsWith('O-O') // castle, short or long
            ? squareRank(move.to) === 0 // white castle
              ? san.startsWith('O-O-O')
                ? 'c1'
                : 'g1'
              : san.startsWith('O-O-O')
                ? 'c8'
                : 'g8'
            : makeSquare(move.to);
          const symbol = glyph.symbol;
          const stackedNumber =
            idx + existingShapes.filter(shape => shape.orig === destSquare && shape.customSvg).length;
          const prerendered = glyphToSvg[symbol] ? glyphToSvg[symbol](stackedNumber) : undefined;
          return {
            orig: destSquare,
            brush: prerendered ? '' : undefined,
            customSvg: prerendered ? { html: prerendered } : undefined,
            label: prerendered ? undefined : { text: symbol, fill: 'purple' },
            // keep some purple just to keep feedback forum on their toes
          };
        })
        // needed so that the right-most (and first) glyph is at the top of the stack
        .reverse()
    );
  } else return [];
}

export const glyphToSvg: Record<string, (stackedNumber: number) => string> = cgGlyphs.glyphToSvg(maxGlyphs);

export const analysisGlyphs: Record<string, (stackedNumber: number) => string> =
  cgGlyphs.analysisGlyphs(maxGlyphs);

export type EndgameGlyph = cgGlyphs.EndgameOutcome;

export const endgameGlyphs: Record<EndgameGlyph, (stackedNumber: number) => string> =
  cgGlyphs.endgameGlyphs(maxGlyphs);
