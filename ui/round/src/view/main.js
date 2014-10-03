var m = require('mithril');
var chessground = require('chessground');
var renderTable = require('./table');

module.exports = function(ctrl) {
  return m('div', {
    config: function(el, isUpdate, context) {
      if (isUpdate) return;
      $('body').trigger('lichess.content_loaded');
    },
    class: 'lichess_game not_spectator pov_' + ctrl.data.player.color
  }, [
    ctrl.data.blindMode ? m('div#lichess_board_blind') : null,
    m('div.lichess_board_wrap', ctrl.data.blindMode ? null : [
      m('div.lichess_board.' + ctrl.data.game.variant.key, chessground.view(ctrl.chessground)),
      m('div#premove_alert', ctrl.trans('premoveEnabledClickAnywhereToCancel'))
    ]),
    m('div.lichess_ground', renderTable(ctrl))
  ]);
};
