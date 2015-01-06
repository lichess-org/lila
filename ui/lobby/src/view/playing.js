var m = require('mithril');

var boardContent = m('div.cg-board-wrap', m('div.cg-board'));

function timer(pov) {
  var time = moment().add(pov.secondsLeft, 'seconds');
  return m('time.moment-from-now', {
    datetime: time
  }, time.fromNow());
}

module.exports = function(ctrl) {
  return m('div#now_playing',
    ctrl.data.nowPlaying.map(function(pov) {
      return m('a', {
        key: pov.gameId,
        href: '/' + pov.fullId,
        class: pov.isMyTurn ? 'my_turn' : ''
      }, [
        m('span', {
          class: 'mini_board live_' + pov.id + ' parse_fen is2d',
          'data-color': pov.color,
          'data-fen': pov.fen,
          'data-lastmove': pov.lastMove,
          config: function(el, isUpdate) {
            if (!isUpdate) lichess.parseFen($(el));
          }
        }, boardContent),
        m('span.meta', [
          pov.opponent.aiLevel ? ctrl.trans('aiNameLevelAiLevel', 'Stockfish', pov.opponent.aiLevel) : pov.opponent.username,
          m('span.indicator',
            pov.isMyTurn ? (pov.secondsLeft ? timer(pov) : ctrl.trans('yourTurn')) : m.trust('&nbsp;'))
        ])
      ])
    })
  );
};
