var m = require('mithril');

var boardContent = m('div.cg-board-wrap', m('div.cg-board'));

module.exports = function(ctrl) {
  return m('div#now_playing',
    ctrl.data.nowPlaying.map(function(pov) {
      return m('a', {
        href: '/' + pov.fullId,
        class: (pov.isMyTurn ? 'my_turn' : '')
      }, [
        m('span', {
          class: 'mini_board mini_board_' + pov.id + ' parse_fen is2d',
          'data-color': pov.color,
          'data-fen': pov.fen,
          'data-lastmove': pov.lastMove,
          config: function(el, isUpdate) {
            if (isUpdate) return;
            lichess.parseFen($(el));
          }
        }, boardContent),
        m('span.meta', [
          pov.opponent.aiLevel ? ctrl.trans('aiNameLevelAiLevel', 'Stockfish', pov.opponent.aiLevel) : pov.opponent.username,
          m('span.indicator',
            pov.isMyTurn ? (pov.secondsLeft ? m('time.moment-from-now', {
              config: function(el, isUpdate) {
                if (!isUpdate) el.setAttribute('datetime', moment().add(pov.secondsLeft, 'seconds'));
              }
            }) : ctrl.trans('yourTurn')) : m.trust('&nbsp;'))
        ])
      ])
    })
  );
};
