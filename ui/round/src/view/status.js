module.exports = function(ctrl) {
  switch (ctrl.data.game.status.name) {
    case 'started':
      return ctrl.trans('playingRightNow');
    case 'aborted':
      return ctrl.trans('gameAborted');
    case 'mate':
      return ctrl.trans('checkmate');
    case 'resign':
      return ctrl.trans(ctrl.data.game.winner == 'white' ? 'blackResigned' : 'whiteResigned');
    case 'stalemate':
      return ctrl.trans('stalemate');
    case 'timeout':
      switch (ctrl.data.game.winner) {
        case 'white':
          return ctrl.trans('blackLeftTheGame');
        case 'black':
          return ctrl.trans('whiteLeftTheGame');
        default:
          return ctrl.trans('draw');
      }
      break;
    case 'draw':
      return ctrl.trans('draw');
    case 'outoftime':
      return ctrl.trans('timeOut');
    case 'noStart':
      return (ctrl.data.game.winner == 'white' ? 'Black' : 'White') + ' didn\'t move';
    case 'cheat':
      return 'Cheat detected';
    case 'variantEnd':
      switch (ctrl.data.game.variant.key) {
        case 'kingOfTheHill':
          return ctrl.trans('kingInTheCenter');
        case 'threeCheck':
          return ctrl.trans('threeChecks');
        default:
          return ctrl.trans('variantEnding');
      }
      break;
    default:
      return ctrl.data.game.status.name;
  }
};
