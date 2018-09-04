import { Ctrl } from '../interfaces';

export default function(ctrl: Ctrl): string {
  var noarg = ctrl.trans.noarg;
  switch (ctrl.data.game.status.name) {
    case 'started':
      return noarg('playingRightNow');
    case 'aborted':
      return noarg('gameAborted');
    case 'mate':
      return ''; //noarg('checkmate');
    case 'resign':
      return noarg(ctrl.data.game.winner == 'white' ? 'blackResigned' : 'whiteResigned');
    case 'stalemate':
      return noarg('stalemate');
    case 'timeout':
      switch (ctrl.data.game.winner) {
        case 'white':
          return noarg('blackLeftTheGame');
        case 'black':
          return noarg('whiteLeftTheGame');
      }
      return noarg('draw');
    case 'draw':
      return noarg('draw');
    case 'outoftime':
      return noarg('timeOut');
    case 'noStart':
      return (ctrl.data.game.winner == 'white' ? 'Black' : 'White') + ' didn\'t move';
    case 'cheat':
      return 'Cheat detected';
    case 'variantEnd':
      return noarg('variantEnding');
    default:
      return ctrl.data.game.status.name;
  }
}
