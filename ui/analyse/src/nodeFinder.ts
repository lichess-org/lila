import { winningChances } from 'ceval';
import { FEN } from 'chessground/types';
import { defined } from 'common';
import { zip } from 'common/algo';

const hasCompChild = (node: Tree.Node): boolean => !!node.children.find(c => !!c.comp);

export function nextGlyphSymbol(
  color: Color,
  symbol: string,
  mainline: Tree.Node[],
  fromPly: number,
): Tree.Node | undefined {
  const len = mainline.length;
  if (!len) return;
  const fromIndex = fromPly - mainline[0].ply;
  for (let i = 1; i < len; i++) {
    const node = mainline[(fromIndex + i) % len];
    const found =
      node.ply % 2 === (color === 'white' ? 1 : 0) &&
      node.glyphs &&
      node.glyphs.find(function(g) {
        return g.symbol === symbol;
      });
    if (found) return node;
  }
  return;
}

export function evalSwings(mainline: Tree.Node[], nodeFilter: (node: Tree.Node) => boolean): Tree.Node[] {
  const found: Tree.Node[] = [];
  const threshold = 0.1;
  for (let i = 1; i < mainline.length; i++) {
    const node = mainline[i];
    const prev = mainline[i - 1];
    if (nodeFilter(node) && node.eval && prev.eval) {
      const diff = Math.abs(winningChances.povDiff('white', prev.eval, node.eval));
      if (
        hasCompChild(prev) &&
        (diff > threshold || (prev.eval.mate && !node.eval.mate && Math.abs(prev.eval.mate) <= 3))
      ) {
        found.push(node);
      }
    }
  }
  return found;
}

// Extended Position Description
const epd = (fen: FEN) => fen.split(' ').slice(0, 4).join(' ');

export function detectThreefold(nodeList: Tree.Node[], node: Tree.Node): void {
  if (defined(node.threefold)) return;
  const currentEpd = epd(node.fen);
  let nbSimilarPositions = 0,
    i;
  for (i in nodeList) if (epd(nodeList[i].fen) === currentEpd) nbSimilarPositions++;
  node.threefold = nbSimilarPositions > 2;
}

// can be 3fold or 5fold
export function add3or5FoldGlyphs(mainlineNodes: Tree.Node[]): void {
  const threefoldMap = new Map<string, Tree.Node[]>();
  for (const node of mainlineNodes) {
    const previousOccurences = threefoldMap.get(epd(node.fen)) || [];
    previousOccurences.push(node);
    threefoldMap.set(epd(node.fen), previousOccurences);
  }
  // only the last positition can be source of three/five-fold
  const lastEpd = epd(mainlineNodes[mainlineNodes.length - 1].fen);
  const repetitions = threefoldMap.get(lastEpd);
  if (repetitions && repetitions.length > 2) {
    const unicodeList = ['①', '②', '③', '④', '⑤'];
    for (const [i, [node, unicode]] of zip(repetitions, unicodeList).entries()) {
      const glyph = { symbol: unicode, name: `repetition number ${i+1}`, id: 9 };
      if (!node.glyphs) node.glyphs = [glyph];
      else node.glyphs.push(glyph);
    }
  }
}
