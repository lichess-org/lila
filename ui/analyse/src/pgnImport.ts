import type { AnalyseData, Game } from './interfaces';
import { makeFen } from 'chessops/fen';
import { makeSanAndPlay, parseSan } from 'chessops/san';
import { makeUci } from 'chessops';
import { makeVariant, parsePgn, startingPosition, type ChildNode, type PgnNodeData } from 'chessops/pgn';
import { IllegalSetup, type Position } from 'chessops/chess';
import type { Player } from 'lib/game';
import type { TreeNode } from 'lib/tree/types';
import { completeNode } from 'lib/tree/node';

const readNode = (
  variant: VariantKey,
  node: ChildNode<PgnNodeData>,
  pos: Position,
  ply: Ply,
  withChildren = true,
): TreeNode => {
  const move = parseSan(pos, node.data.san);
  if (!move) throw new Error(`Can't play ${node.data.san} at move ${Math.ceil(ply / 2)}, ply ${ply}`);
  return completeNode(variant)({
    ply,
    san: makeSanAndPlay(pos, move),
    fen: makeFen(pos.toSetup()),
    uci: makeUci(move),
    children: withChildren ? node.children.map(child => readNode(variant, child, pos.clone(), ply + 1)) : [],
  });
};

export default function (pgn: string): Partial<AnalyseData> {
  const game = parsePgn(pgn)[0];
  const start = startingPosition(game.headers).unwrap();
  const fen = makeFen(start.toSetup());
  const variant: VariantKey = rulesToVariantKey[start.rules] || start.rules;
  const initialPly = (start.toSetup().fullmoves - 1) * 2 + (start.turn === 'white' ? 0 : 1);
  const treeParts: TreeNode[] = [
    completeNode(variant)({
      id: '',
      ply: initialPly,
      fen,
      children: [],
    }),
  ];
  let tree = game.moves;
  const pos = start;
  const sidelines: TreeNode[][] = [[]];
  let index = 0;
  while (tree.children.length) {
    const [mainline, ...variations] = tree.children;
    const ply = initialPly + index + 1;
    sidelines.push(variations.map(variation => readNode(variant, variation, pos.clone(), ply)));
    treeParts.push(readNode(variant, mainline, pos, ply, false));
    tree = mainline;
    index += 1;
  }
  const variantName = makeVariant(start.rules) || variant;
  // TODO Improve types so that analysis data != game data
  return {
    game: {
      fen,
      initialFen: fen,
      id: 'synthetic',
      opening: undefined, // TODO
      player: start.turn,
      status: { id: 20, name: 'started' },
      turns: treeParts.length,
      variant: {
        key: variant,
        name: variantName,
        short: variantName,
      },
    } as Game,
    player: { color: 'white' } as Player,
    opponent: { color: 'black' } as Player,
    treeParts,
    sidelines,
    userAnalysis: true,
  };
}

const rulesToVariantKey: { [key: string]: VariantKey } = {
  chess: 'standard',
  kingofthehill: 'kingOfTheHill',
  '3check': 'threeCheck',
  racingkings: 'racingKings',
};

export const renderPgnError = (error: string = '') =>
  `PGN error: ${
    {
      [IllegalSetup.Empty]: 'empty board',
      [IllegalSetup.OppositeCheck]: 'king in check',
      [IllegalSetup.PawnsOnBackrank]: 'pawns on back rank',
      [IllegalSetup.Kings]: 'king(s) missing',
      [IllegalSetup.Variant]: 'invalid Variant header',
    }[error] ?? error
  }`;
