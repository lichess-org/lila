import { povChances } from 'lib/ceval/winningChances';
import type { Glyph, TreeNode } from 'lib/tree/types';

const glyphs = {
  inaccuracy: { id: 6, symbol: '?!', name: 'Inaccuracy' } as Glyph,
  mistake: { id: 2, symbol: '?', name: 'Mistake' } as Glyph,
  blunder: { id: 4, symbol: '??', name: 'Blunder' } as Glyph,
};

export function liveGlyph(parentEval: EvalScore, currentEval: EvalScore, ply: Ply): Glyph | undefined {
  const color: Color = ply % 2 === 1 ? 'white' : 'black';
  const loss = povChances(color, parentEval) - povChances(color, currentEval);
  if (loss > 0.3) return glyphs.blunder;
  if (loss > 0.2) return glyphs.mistake;
  if (loss > 0.1) return glyphs.inaccuracy;
  return undefined;
}

export function liveNodeGlyph(node: TreeNode, parentNode: TreeNode): Glyph | undefined {
  return parentNode.ceval && node.ceval && liveGlyph(parentNode.ceval, node.ceval, node.ply);
}
