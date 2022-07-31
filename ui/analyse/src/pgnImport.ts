import { AnalyseData, Game } from './interfaces';
import { makeFen } from 'chessops/fen';
import { makeSanAndPlay, parseSan } from 'chessops/san';
import { makeUci, Rules } from 'chessops';
import { makeVariant, parsePgn, parseVariant, startingPosition } from 'chessops/pgn';
import { Player } from 'game';
import { scalachessCharPair } from 'chessops/compat';

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
  const mainline = Array.from(game.moves.mainline());
  const pos = start;
  mainline.forEach((node, index) => {
    const ply = initialPly + index + 1;
    const move = parseSan(pos, node.san);
    if (!move) throw `Can't replay move ${node.san} at ply ${ply}`;
    const san = makeSanAndPlay(pos, move);
    treeParts.push({
      id: scalachessCharPair(move),
      ply,
      fen: makeFen(pos.toSetup()),
      children: [],
      san,
      uci: makeUci(move),
    });
  });
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
      turns: mainline.length,
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
