import { winningChances } from 'ceval';
import { FEN } from 'chessground/types';
import { defined } from 'common';

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
export function add35FoldGlyphs(mainlineNodes: Tree.Node[]): void {
  // key: epd, values: indice of nodes
  const threefoldMap = new Map<string, number[]>();
  for (let i = 0; i < mainlineNodes.length; i++) {
    const node = mainlineNodes[i];
    const previousOccurences = threefoldMap.get(epd(node.fen)) || [];
    previousOccurences.push(i);
    threefoldMap.set(epd(node.fen), previousOccurences);
  }
  for (const indices of threefoldMap.values()) {
    if (indices.length > 2) {
      console.log
      for (let i = 0; i < indices.length; i++) {
        const fraction = toUniCode(i, indices.length);
        // TODO, proper id number? How to choose, what for?
        const glyph = { symbol: fraction, name: `repetition no ${i}`, id: 111 };
        const node = mainlineNodes[indices[i]];
        if (!node.glyphs) node.glyphs = [glyph];
        else node.glyphs.push(glyph);
      }
    }
  }
}

// unicode symbols for third and fifth fraction
// ⅓ ⅔ ³⁄₃ ⅕ ⅖ ⅗ ⁵⁄₅
// @ts-ignore
const toFractionUnicode = (idx: number, denominator: number): string => {
  const numerator = idx + 1;
  if (denominator === 3) {
    if (numerator === 1) return '⅓';
    if (numerator === 2) return '⅔';
    if (numerator === 3) return '³⁄₃';
    throw new Error(`Unexpected numerator for 3fold: ${numerator}`);
  }
  if (denominator === 5) {
    if (numerator === 1) return '⅕';
    if (numerator === 2) return '⅖';
    if (numerator === 3) return '⅗';
    if (numerator === 4) return '⅘';
    if (numerator === 5) return '⁵⁄₅';
    throw new Error(`Unexpected numerator for 5fold: ${numerator}`);
  }
  throw new Error(`Unexpected denominator ${denominator}, numerator: ${numerator}`);
};

const toUniCode = (idx: number, _: number): string => {
    if (idx === 0) return '①';
    if (idx === 1) return '②';
    if (idx === 2) return '③';
    if (idx === 3) return '④';
    if (idx === 4) return '⑤';
  throw new Error(`Unexpected idx ${idx}`);
};
