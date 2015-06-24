var m = require('mithril');

var boardContent = m('div.cg-board-wrap', m('div.cg-board'));

function player(p) {
  return [
    p.name,
    m('br'),
    p.title ? p.title + ' ' : '',
    p.rating
  ]
}

function miniGame(game) {
  return m('div', [
    m('a', {
      key: game.id,
      href: '/' + game.id + (game.color === 'white' ? '' : '/black'),
      class: 'mini_board live_' + game.id + ' parse_fen is2d',
      'data-color': game.color,
      'data-fen': game.fen,
      'data-lastmove': game.lastMove,
      config: function(el, isUpdate) {
        if (!isUpdate) lichess.parseFen($(el));
      }
    }, boardContent),
    m('div.vstext.clearfix', [
      m('div.left', player(game.white)),
      m('div.right', player(game.black))
    ])
  ]);
}

module.exports = function(games) {
  return m('div.game_list.playing', games.map(miniGame));
}
