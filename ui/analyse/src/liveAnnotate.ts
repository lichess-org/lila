import { povChances } from 'lib/ceval/winningChances';
import type { TreeWrapper } from 'lib/tree';
import type { Glyph, TreeNode, TreePath } from 'lib/tree/types';

const glyphs = {
  inaccuracy: { id: 6, symbol: '?!', name: 'Inaccuracy' } as Glyph,
  mistake: { id: 2, symbol: '?', name: 'Mistake' } as Glyph,
  blunder: { id: 4, symbol: '??', name: 'Blunder' } as Glyph,
};

export default class LiveAnnotate {
  private readonly glyphs = new Map<TreePath, Glyph>();

  readonly get = this.glyphs.get.bind(this.glyphs);

  readonly onNewCeval = (path: TreePath, node: TreeNode, tree: TreeWrapper): void => {
    const parent = tree.parentNode(path);
    this.update(path, node, parent);
    node.children.forEach(child => this.update(path + child.id, child, node));
  };

  private readonly liveGlyph = (
    parentEval: EvalScore,
    currentEval: EvalScore,
    ply: Ply,
  ): Glyph | undefined => {
    const color: Color = ply % 2 === 1 ? 'white' : 'black';
    const loss = povChances(color, parentEval) - povChances(color, currentEval);
    if (loss > 0.3) return glyphs.blunder;
    if (loss > 0.2) return glyphs.mistake;
    if (loss > 0.1) return glyphs.inaccuracy;
    return undefined;
  };

  private readonly update = (path: TreePath, node: TreeNode, parent: TreeNode): void => {
    if (!path.length) return;
    const glyph = parent.ceval && node.ceval && this.liveGlyph(parent.ceval, node.ceval, node.ply);
    if (glyph) this.glyphs.set(path, glyph);
    else this.glyphs.delete(path);
  };
}
