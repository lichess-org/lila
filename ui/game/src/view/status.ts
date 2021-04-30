import { Ctrl } from '../interfaces';

export default function status(ctrl: Ctrl): string {
  const noarg = ctrl.trans.noarg,
    d = ctrl.data;
  switch (d.game.status.name) {
    case 'started':
      return noarg('playingRightNow');
    case 'aborted':
      return noarg('gameAborted');
    case 'mate':
      return noarg('checkmate');
    case 'resign':
      return noarg(d.game.winner == 'white' ? 'blackResigned' : 'whiteResigned');
    case 'stalemate':
      return noarg('stalemate');
    case 'timeout':
      switch (d.game.winner) {
        case 'white':
          return noarg('blackLeftTheGame');
        case 'black':
          return noarg('whiteLeftTheGame');
      }
      return noarg('draw');
    case 'draw':
      return noarg('draw');
    case 'outoftime':
      return `${d.game.turns % 2 === 0 ? noarg('whiteTimeOut') : noarg('blackTimeOut')}${
        d.game.winner ? '' : ` • ${noarg('draw')}`
      }`;
    case 'noStart':
      return (d.game.winner == 'white' ? 'Black' : 'White') + " didn't move";
    case 'cheat':
      return noarg('Cheat detected');
    case 'variantEnd':
      switch (d.game.variant.key) {
        case 'kingOfTheHill':
          return noarg('kingInTheCenter');
        case 'threeCheck':
          return noarg('threeChecks');
      }
      return noarg('variantEnding');
    case 'unknownFinish':
      return 'Finished';
    default:
      return d.game.status.name;
  }
}
