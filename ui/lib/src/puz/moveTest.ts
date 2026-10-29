import { normalizeMove, Position } from 'chessops/chess';
import { makeUci, parseUci } from 'chessops/util';

export const playAndTestMutatingPosition = (
  pos: Position,
  expectedUci: Uci,
  orig: Key,
  dest: Key,
  promotion?: Role,
): boolean => {
  const playedUci = `${orig}${dest}${promotion ? (promotion === 'knight' ? 'n' : promotion[0]) : ''}`;
  return playAndTestUciMutatingPosition(pos, expectedUci, playedUci);
};

export const playAndTestUciMutatingPosition = (pos: Position, expectedUci: Uci, playedUci: Uci): boolean => {
  const move = normalizeMove(pos, parseUci(playedUci)!);
  const uci = makeUci(move);
  const normalizedExpected = makeUci(normalizeMove(pos, parseUci(expectedUci)!));
  pos.play(move);
  return pos.isCheckmate() || uci === normalizedExpected;
};
