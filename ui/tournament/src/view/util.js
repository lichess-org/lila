var m = require('mithril');

var boardContent = m('div.cg-board-wrap', m('div.cg-board'));

function miniBoard(game) {
  return m('a', {
    key: game.id,
    href: '/' + game.id + (game.color === 'white' ? '' : '/black'),
    class: 'mini_board live_' + game.id + ' parse_fen is2d',
    'data-color': game.color,
    'data-fen': game.fen,
    'data-lastmove': game.lastMove,
    config: function(el, isUpdate) {
      if (!isUpdate) lichess.parseFen($(el));
    }
  }, boardContent);
}

function ratio2percent(r) {
  return Math.round(100 * r) + '%';
}

module.exports = {
  player: function(p) {
    var ratingDiff;
    if (p.ratingDiff > 0) ratingDiff = m('span.positive[data-icon=N]', p.ratingDiff);
    else if (p.ratingDiff < 0) ratingDiff = m('span.negative[data-icon=M]', -p.ratingDiff);
    var rating = p.rating + p.ratingDiff + (p.provisional ? '?' : '');
    var fullName = (p.title ? p.title + ' ' : '') + p.name;
    var attrs = {
      class: 'ulpt user_link' + (fullName.length > 15 ? ' long' : ''),
      config: function(el, isUpdate, ctx) {
        if (!isUpdate) ctx.onunload = function() {
          $.powerTip.destroy(el);
        };
      },
      href: '/@/' + p.name
    };
    return {
      tag: 'a',
      attrs: attrs,
      children: [
        m('span.name', fullName),
        m('span.progress', [rating, ratingDiff])
      ]
    };
  },
  miniBoard: miniBoard,
  ratio2percent: ratio2percent,
  numberRow: function(name, value, typ) {
    return m('tr', [m('th', name), m('td',
      typ === 'raw' ? value : (typ === 'percent' ? (
        value[1] > 0 ? ratio2percent(value[0] / value[1]) : 0
      ) : lichess.numberFormat(value))
    )]);
  }
};
