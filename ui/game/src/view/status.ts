import { Status } from '../interfaces';

export default function status(status: Status, winner: Color | undefined, trans: Trans): string {
  const noarg = trans.noarg;
  switch (status.name) {
    case 'started':
      return noarg('playingRightNow');
    case 'aborted':
      return noarg('gameAborted');
    case 'mate':
      return noarg('checkmate');
    case 'resign':
      return noarg(winner == 'sente' ? 'whiteResigned' : 'blackResigned');
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
          return noarg('whiteLeftTheGame');
        case 'gote':
          return noarg('blackLeftTheGame');
        default:
          return noarg('draw');
      }
    case 'draw':
      return noarg('draw');
    case 'outoftime':
      return noarg('timeOut');
    case 'noStart':
      return (winner == 'sente' ? 'Gote' : 'Sente') + " didn't move";
    case 'cheat':
      return noarg('cheatDetected');
    case 'unknownFinish':
      return 'Finished';
    case 'royalsLost':
      return noarg('royalsLost');
    case 'bareKing':
      return noarg('bareKing');
    default:
      return status.name;
  }
}
