import { Status } from '../interfaces';
import { transWithColorName } from 'common/colorName';

export default function status(
  trans: Trans,
  status: Status,
  winner: Color | undefined,
  initialSfen: Sfen | undefined
): string {
  const noarg = trans.noarg;
  switch (status.name) {
    case 'started':
      return noarg('playingRightNow');
    case 'aborted':
      return noarg('gameAborted');
    case 'mate':
      return noarg('checkmate');
    case 'resign':
      return winner
        ? transWithColorName(trans, 'xResigned', winner === 'sente' ? 'gote' : 'sente', initialSfen)
        : noarg('finished');
    case 'stalemate':
      return noarg('stalemate');
    case 'impasse27':
      return noarg('impasse');
    case 'tryRule':
      return 'Try rule';
    case 'perpetualCheck':
      return noarg('perpetualCheck');
    case 'timeout':
      switch (winner) {
        case 'sente':
        case 'gote':
          return transWithColorName(trans, 'xLeftTheGame', winner, initialSfen);
        default:
          return noarg('draw');
      }
    case 'draw':
      return noarg('draw');
    case 'outoftime':
      return noarg('timeOut');
    case 'noStart':
      return winner ? transWithColorName(trans, 'xDidntMove', winner, initialSfen) : noarg('finished');
    case 'cheat':
      return noarg('cheatDetected');
    case 'unknownFinish':
      return noarg('finished');
    case 'royalsLost':
      return noarg('royalsLost');
    case 'bareKing':
      return noarg('bareKing');
    default:
      return status.name;
  }
}
