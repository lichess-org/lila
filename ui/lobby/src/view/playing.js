var m = require('mithril');

var boardContent = m('div.cg-board-wrap', m('div.cg-board'));

function timer(pov) {
  var time = moment().add(pov.secondsLeft, 'seconds');
  return m('time.moment-from-now', {
    datetime: time.format()
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
            class: 'mini_board is2d',
            config: function(el, isUpdate, ctx) {
              var lm = pov.lastMove;
              var config = {
                coordinates: false,
                viewOnly: true,
                minimalDom: true,
                orientation: pov.color,
                fen: pov.fen,
                lastMove: lm ? [lm[0] + lm[1], lm[2] + lm[3]] : []
              };
              if (ctx.ground) ctx.ground.set(config);
              else ctx.ground = Chessground(el, config);
            }
          },
          boardContent),
        m('span.meta', [
          pov.opponent.ai ? ctrl.trans('aiNameLevelAiLevel', 'Stockfish', pov.opponent.ai) : pov.opponent.username,
          m('span.indicator',
            pov.isMyTurn ? (pov.secondsLeft ? timer(pov) : ctrl.trans('yourTurn')) : m.trust('&nbsp;'))
        ])
      ])
    })
  );
};
