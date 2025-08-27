import { winningChances } from 'lib/ceval/ceval';
import { fenToEpd } from 'lib/game/chess';
import { defined } from 'lib';
import { zip } from 'lib/algo';

const hasCompChild = (node: Tree.Node): boolean => !!node.children.find(c => !!c.comp);

export const nextGlyphSymbol = (
  color: Color,
  symbol: string,
  mainline: Tree.Node[],
  fromPly: number,
): Tree.Node | undefined =>
  mainline
    .map((_, i) => mainline[(fromPly - mainline[0].ply + i + 1) % mainline.length])
    .find(n => n.ply % 2 === (color === 'white' ? 1 : 0) && n.glyphs?.some(g => g.symbol === symbol));

export const evalSwings = (mainline: Tree.Node[], nodeFilter: (node: Tree.Node) => boolean): Tree.Node[] =>
  mainline.slice(1).filter((curr, i) => {
    const prev = mainline[i];
    return (
      nodeFilter(curr) &&
      curr.eval &&
      prev.eval &&
      hasCompChild(prev) &&
      (Math.abs(winningChances.povDiff('white', prev.eval, curr.eval)) > 0.1 ||
        (prev.eval.mate && !curr.eval.mate && Math.abs(prev.eval.mate) <= 3))
    );
  });

export function detectThreefold(nodeList: Tree.Node[], node: Tree.Node): void {
  if (defined(node.threefold)) return;
  const currentEpd = fenToEpd(node.fen);
  node.threefold = nodeList.filter(n => fenToEpd(n.fen) === currentEpd).length > 2;
}

// can be 3fold or 5fold
export function add3or5FoldGlyphs(mainlineNodes: Tree.Node[]): boolean {
  // only the last positition can be source of three/five-fold
  const lastEpd = fenToEpd(mainlineNodes[mainlineNodes.length - 1].fen);
  const repetitions = mainlineNodes.filter(n => fenToEpd(n.fen) === lastEpd);
  if (repetitions.length > 2) {
    const unicodeList = ['①', '②', '③', '④', '⑤'];
    for (const [i, [node, unicode]] of zip(repetitions, unicodeList).entries()) {
      const glyph = { symbol: unicode, name: `repetition number ${i + 1}`, id: 9 };
      if (!node.glyphs) node.glyphs = [glyph];
      else node.glyphs.push(glyph);
    }
    return true;
  }
  return false;
}
