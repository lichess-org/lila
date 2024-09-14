import { Status, StatusId, StatusName } from '../interfaces';
import { transWithColorName } from 'common/colorName';
import { statusIdToName } from '../status';

export default function status(
  trans: Trans,
  status: Status | StatusName | StatusId,
  winner: Color | undefined,
  isHandicap: boolean
): string {
  const noarg = trans.noarg,
    name: StatusName | undefined =
      typeof status === 'object' ? status.name : typeof status === 'number' ? statusIdToName(status) : status;
  switch (name) {
    case 'created':
      return '-'; // Good enough, shouldn't happen
    case 'started':
      return noarg('playingRightNow');
    case 'paused':
      return noarg('gameAdjourned');
    case 'aborted':
      return noarg('gameAborted');
    case 'mate':
      return noarg('checkmate');
    case 'resign':
      return winner
        ? transWithColorName(trans, 'xResigned', winner === 'sente' ? 'gote' : 'sente', isHandicap)
        : noarg('finished');
    case 'stalemate':
      return noarg('stalemate');
    case 'impasse27':
      return noarg('impasse');
    case 'tryRule':
      return 'Try rule';
    case 'perpetualCheck':
      return noarg('perpetualCheck');
    case 'repetition':
      return noarg('repetition');
    case 'timeout':
      switch (winner) {
        case 'sente':
        case 'gote':
          return transWithColorName(trans, 'xLeftTheGame', winner === 'sente' ? 'gote' : 'sente', isHandicap);
        default:
          return noarg('draw');
      }
    case 'draw':
      return noarg('draw');
    case 'outoftime':
      return noarg('timeOut');
    case 'noStart':
      return winner
        ? transWithColorName(trans, 'xDidntMove', winner === 'sente' ? 'gote' : 'sente', isHandicap)
        : noarg('finished');
    case 'cheat':
      return noarg('cheatDetected');
    case 'unknownFinish':
      return noarg('finished');
    case 'royalsLost':
      return noarg('royalsLost');
    case 'bareKing':
      return noarg('bareKing');
    case 'specialVariantEnd':
      return noarg('check'); // enough for now
    default:
      return name || '?';
  }
}
