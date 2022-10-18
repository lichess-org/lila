import { AnalyseData, Game } from './interfaces';
import { makeFen } from 'chessops/fen';
import { makeSan, makeSanAndPlay, parseSan } from 'chessops/san';
import { makeUci, Rules } from 'chessops';
import { makeVariant, parsePgn, parseVariant, startingPosition } from 'chessops/pgn';
import { Player } from 'game';
import { scalachessCharPair } from 'chessops/compat';
import { makeSquare } from 'chessops/util';

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
  const pos = start;
  let node = game.moves;
  let turns = 0;
  while (node.children.length) {
    turns += 1;
    const ply = initialPly + turns;
    const [mainlineMove, ...children] = node.children;
    const move = parseSan(pos, mainlineMove.data.san);
    if (!move) throw `Can't replay parent move ${mainlineMove.data.san} at ply ${ply}`;
    treeParts.push({
      id: scalachessCharPair(move),
      ply,
      fen: makeFen(pos.toSetup()),
      children: children.map(child => {
        const move = parseSan(pos, child.data.san);
        if (!move) throw `Can't replay child move ${child.data.san} at ply ${ply}`;
        return {
          id: scalachessCharPair(move),
          ply,
          fen,
          san: makeSan(pos, move),
          children: [],
        };
      }),
      san: makeSanAndPlay(pos, move),
      uci: makeUci(move),
      check: pos.isCheck() ? makeSquare(pos.toSetup().board.kingOf(pos.turn)!) : undefined,
    });
    node = mainlineMove;
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
      turns,
      variant: {
        key: variantKey,
        name: variantName,
        short: variantName,
      },
    } as Game,
    player: { color: 'white' } as Player,
    opponent: { color: 'black' } as Player,
    treeParts,
    userAnalysis: true,
  };
}

const rulesToVariantKey: { [key: string]: VariantKey } = {
  chess: 'standard',
  kingofthehill: 'kingOfTheHill',
  '3check': 'threeCheck',
  racingkings: 'racingKings',
};
