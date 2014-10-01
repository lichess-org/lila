var map = require('lodash-node/modern/collections/map');
var chessground = require('chessground');
var opposite = chessground.util.opposite;
var classSet = chessground.util.classSet;
var partial = chessground.util.partial;
var clockView = require('./clock/view');
var m = require('mithril');

module.exports = function(ctrl) {
  var clockRunningColor = ctrl.isClockRunning() ? ctrl.data.game.player : null;
  return m('div', {
    class: 'lichess_game clearfix not_spectator pov_' + ctrl.data.player.color
  }, [
    ctrl.data.blindMode ? m('div#lichess_board_blind') : null,
    m('div.lichess_board_wrap', ctrl.data.blindMode ? null : [
      m('div.lichess_board.' + ctrl.data.game.variant, chessground.view(ctrl.chessground)),
      m('div#premove_alert', ctrl.trans('premoveEnabledClickAnywhereToCancel'))
    ]),
    m('div.lichess_ground',
      m('div.lichess_table_wrap', [
        (ctrl.clock && !ctrl.data.blindMode) ? clockView(ctrl.clock, opposite(ctrl.data.player.color), "top", clockRunningColor) : null,
        m('div', {
          class: 'lichess_table onbg ' + classSet({
            'table_with_clock': ctrl.clock,
            'finished': ctrl.data.game.finished
          })
        }), (ctrl.clock && !ctrl.data.blindMode) ? clockView(ctrl.clock, ctrl.data.player.color, "bottom", clockRunningColor) : null,
      ])
    )
  ]);
};
