var m = require('mithril');
var status = require('game').status;

var boardContent = m('div.cg-board-wrap', m('div.cg-board'));

function player(game, color) {
  var p = game[color];
  var title = p.title ? m('span.title', p.title) : '';
  return [
    p.name,
    m('br'),
    color == 'white' ? [title, p.rating] : [p.rating, title],
    m('br'),
    m('span', {
      class: 'clock' + (game.status < status.ids.mate && color === game.color ? ' turn' : '')
    }, '00:00')
  ]
}

function miniGame(game) {
  var result = '*';
  if (game.status >= status.ids.mate) switch (game.winner) {
    case 'white':
      result = '1-0';
      break;
    case 'black':
      result = '0-1';
      break;
    default:
      result = '½-½';
  }
  return m('div', [
    m('a', {
      key: game.id,
      href: '/' + game.id + (game.color === 'white' ? '' : '/black'),
      class: 'mini_board live_' + game.id + ' parse_fen is2d',
      'data-color': 'white',
      'data-fen': game.fen,
      'data-lastmove': game.lastMove,
      config: function(el, isUpdate) {
        if (!isUpdate) lichess.parseFen($(el));
      }
    }, boardContent),
    m('div.vstext.clearfix', [
      m('div.left', player(game, 'white')),
      m('div.right', player(game, 'black')),
      m('div.result', result)
    ])
  ]);
}

module.exports = function(games) {
  return m('div.game_list.playing', games.map(miniGame));
}
