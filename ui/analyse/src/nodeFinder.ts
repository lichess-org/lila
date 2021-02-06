import { winningChances } from 'ceval';
import { defined } from 'common';

function hasCompChild(node: Tree.Node): boolean {
  return !!node.children.find(function (c) {
    return !!c.comp;
  });
}

export function nextGlyphSymbol(
  color: Color,
  symbol: string,
  mainline: Tree.Node[],
  fromPly: number
): Tree.Node | undefined {
  const len = mainline.length;
  if (!len) return;
  const fromIndex = fromPly - mainline[0].ply;
  for (var i = 1; i < len; i++) {
    const node = mainline[(fromIndex + i) % len];
    const found =
      node.ply % 2 === (color === 'white' ? 1 : 0) &&
      node.glyphs &&
      node.glyphs.find(function (g) {
        return g.symbol === symbol;
      });
    if (found) return node;
  }
  return;
}

export function evalSwings(mainline: Tree.Node[], nodeFilter: (node: Tree.Node) => boolean): Tree.Node[] {
  const found: Tree.Node[] = [];
  const threshold = 0.1;
  for (var i = 1; i < mainline.length; i++) {
    var node = mainline[i];
    var prev = mainline[i - 1];
    if (nodeFilter(node) && node.eval && prev.eval) {
      var diff = Math.abs(winningChances.povDiff('white', prev.eval, node.eval));
      if (diff > threshold && hasCompChild(prev)) found.push(node);
    }
  }
  return found;
}

function threefoldFen(fen: Fen) {
  return fen.split(' ').slice(0, 4).join(' ');
}

export function detectThreefold(nodeList: Tree.Node[], node: Tree.Node): void {
  if (defined(node.threefold)) return;
  const currentFen = threefoldFen(node.fen);
  let nbSimilarPositions = 0,
    i;
  for (i in nodeList) if (threefoldFen(nodeList[i].fen) === currentFen) nbSimilarPositions++;
  node.threefold = nbSimilarPositions > 2;
}
