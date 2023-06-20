import { AnalyseData, Game } from './interfaces';
import { makeFen } from 'chessops/fen';
import { makeSanAndPlay, parseSan } from 'chessops/san';
import { makeUci, Rules } from 'chessops';
import { makeVariant, parsePgn, parseVariant, startingPosition, ChildNode, PgnNodeData } from 'chessops/pgn';
import { Position } from 'chessops/chess';
import { Player } from 'game';
import { scalachessCharPair } from 'chessops/compat';
import { makeSquare } from 'chessops/util';

const readNode = (
  node: ChildNode<PgnNodeData>,
  pos: Position,
  ply: number,
  withChildren = true
): Tree.Node => {
  const move = parseSan(pos, node.data.san);
  if (!move) throw `Can't replay move ${node.data.san} at ply ${ply}`;
  return {
    id: scalachessCharPair(move),
    ply,
    san: makeSanAndPlay(pos, move),
    fen: makeFen(pos.toSetup()),
    uci: makeUci(move),
    children: withChildren ? node.children.map(child => readNode(child, pos.clone(), ply + 1)) : [],
    check: pos.isCheck() ? makeSquare(pos.toSetup().board.kingOf(pos.turn)!) : undefined,
  };
};

export default function (pgn: string): Partial<AnalyseData> {
  const game = parsePgn(pgn)[0];
  const headers = new Map(Array.from(game.headers, ([key, value]) => [key.toLowerCase(), value]));
  const start = startingPosition(game.headers).unwrap();
  const fen = makeFen(start.toSetup());
  const initialPly = (start.toSetup().fullmoves - 1) * 2 + (start.turn === 'white' ? 0 : 1);
  const treeParts: Tree.Node[] = [
    {
      id: '',
      ply: initialPly,
      fen,
      children: [],
    },
  ];
  let tree = game.moves;
  const pos = start;
  const sidelines: Tree.Node[][] = [[]];
  let index = 0;
  while (tree.children.length) {
    const [mainline, ...variations] = tree.children;
    const ply = initialPly + index + 1;
    sidelines.push(variations.map(variation => readNode(variation, pos.clone(), ply)));
    treeParts.push(readNode(mainline, pos, ply, false));
    tree = mainline;
    index += 1;
  }
  const rules: Rules = parseVariant(headers.get('variant')) || 'chess';
  const variantKey: VariantKey = rulesToVariantKey[rules] || rules;
  const variantName = makeVariant(rules) || variantKey;
  // TODO Improve types so that analysis data != game data
  return {
    game: {
      fen,
      id: 'synthetic',
      opening: undefined, // TODO
      player: start.turn,
      status: { id: 20, name: 'started' },
      turns: treeParts.length,
      variant: {
        key: variantKey,
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
