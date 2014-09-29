var partial = require('lodash-node/modern/functions/partial');
var map = require('lodash-node/modern/collections/map');
var chessground = require('chessground');
var m = require('mithril');

module.exports = function(ctrl) {
  return m('div', {
    class: 'lichess_game clearfix not_spectator pov_' + ctrl.data.player.color
  }, [
    ctrl.data.blindMode ? m('div#lichess_board_blind') : null,
    m('div.lichess_board_wrap', ctrl.data.blindMode ? null : [
      m('div.lichess_board.' + ctrl.data.game.variant, chessground.view(ctrl.chessground)),
      m('div#premove_alert', ctrl.trans('premoveEnabledClickAnywhereToCancel'))
    ])
  ]);
};
