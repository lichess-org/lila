import { AnalyseData, Game } from './interfaces';
import { Player } from 'game';

const readNode = (
  node: co.ChildNode<co.PgnNodeData>,
  pos: co.Position,
  ply: number,
  withChildren = true,
): Tree.Node => {
  const move = co.san.parseSan(pos, node.data.san);
  if (!move) throw `Can't replay move ${node.data.san} at ply ${ply}`;
  return {
    id: co.compat.scalachessCharPair(move),
    ply,
    san: co.san.makeSanAndPlay(pos, move),
    fen: co.fen.makeFen(pos.toSetup()),
    uci: co.makeUci(move),
    children: withChildren ? node.children.map(child => readNode(child, pos.clone(), ply + 1)) : [],
    check: pos.isCheck() ? co.makeSquare(pos.toSetup().board.kingOf(pos.turn)!) : undefined,
  };
};

export default function (pgn: string): Partial<AnalyseData> {
  const game = co.pgn.parsePgn(pgn)[0];
  const headers = new Map(Array.from(game.headers, ([key, value]) => [key.toLowerCase(), value]));
  const start = co.pgn.startingPosition(game.headers).unwrap();
  const fen = co.fen.makeFen(start.toSetup());
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
  const rules: co.Rules = co.pgn.parseVariant(headers.get('variant')) || 'chess';
  const variantKey: VariantKey = rulesToVariantKey[rules] || rules;
  const variantName = co.pgn.makeVariant(rules) || variantKey;
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
