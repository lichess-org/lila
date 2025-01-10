import { i18n, i18nFormatCapitalized } from 'i18n';
import { colorName } from 'shogi/color-name';
import type { Status, StatusId, StatusName } from '../interfaces';
import { statusIdToName } from '../status';

export default function status(
  status: Status | StatusName | StatusId,
  winner: Color | undefined,
  isHandicap: boolean,
): string {
  const name: StatusName | undefined =
    typeof status === 'object'
      ? status.name
      : typeof status === 'number'
        ? statusIdToName(status)
        : status;
  switch (name) {
    case 'created':
      return '-'; // good enough, shouldn't happen
    case 'started':
      return i18n('playingRightNow');
    case 'paused':
      return i18n('gameAdjourned');
    case 'aborted':
      return i18n('gameAborted');
    case 'mate':
      return i18n('checkmate');
    case 'resign':
      return winner
        ? i18nFormatCapitalized(
            'xResigned',
            colorName(winner === 'sente' ? 'gote' : 'sente', isHandicap),
          )
        : i18n('finished');
    case 'stalemate':
      return i18n('stalemate');
    case 'impasse27':
      return i18n('impasse');
    case 'tryRule':
      return 'Try rule';
    case 'perpetualCheck':
      return i18n('perpetualCheck');
    case 'repetition':
      return i18n('repetition');
    case 'timeout':
      switch (winner) {
        case 'sente':
        case 'gote':
          return i18nFormatCapitalized(
            'xLeftTheGame',
            colorName(winner === 'sente' ? 'gote' : 'sente', isHandicap),
          );
        default:
          return i18n('draw');
      }
    case 'draw':
      return i18n('draw');
    case 'outoftime':
      return i18n('timeOut');
    case 'noStart':
      return winner
        ? i18nFormatCapitalized(
            'xDidntMove',
            colorName(winner === 'sente' ? 'gote' : 'sente', isHandicap),
          )
        : i18n('finished');
    case 'cheat':
      return i18n('cheatDetected');
    case 'unknownFinish':
      return i18n('finished');
    case 'royalsLost':
      return i18n('royalsLost');
    case 'bareKing':
      return i18n('bareKing');
    case 'specialVariantEnd':
      return i18n('check'); // enough for now
    default:
      return name || '?';
  }
}
