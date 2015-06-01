var m = require('mithril');
var partial = require('chessground').util.partial;

var boardContent = m('div.cg-board-wrap', m('div.cg-board'));

function miniGame(game) {
  return m('div', [
    m('a', {
      key: game.id,
      href: '/' + game.id,
      class: 'mini_board live_' + game.id + ' parse_fen is2d',
      'data-color': game.color,
      'data-fen': game.fen,
      'data-lastmove': game.lastMove,
      config: function(el, isUpdate) {
        if (!isUpdate) lichess.parseFen($(el));
      }
    }, boardContent),
    m('div.vstext.clearfix', [
      m('div.left', [
        game.user1.name,
        m('br'),
        game.user1.title ? game.user1.title + ' ' : '',
        game.user1.rating
      ]),
      m('div.right', [
        game.user2.name,
        m('br'),
        game.user2.rating,
        game.user2.title ? ' ' + game.user2.title : ''
      ])
    ])
  ]);
}

module.exports = {
  title: function(ctrl) {
    return m('h1', {
      class: 'text',
      'data-icon': ctrl.data.isFinished ? '' : 'g'
    }, [
      ctrl.data.fullName,
      ctrl.data.private ? [
        ' ',
        m('span.text[data-icon=a]', ctrl.trans('isPrivate'))
      ] : null
    ]);
  },
  currentPlayer: function(ctrl) {
    if (!ctrl.userId) return null;
    return ctrl.data.players.filter(function(p) {
      return (p.name || '').toLowerCase() === ctrl.userId;
    })[0] || null;
  },
  player: function(p) {
    var perf;
    if (p.perf > 0) perf = m('span.positive[data-icon=N]', p.perf);
    else if (p.perf < 0) perf = m('span.negative[data-icon=M]', -p.perf);
    return {
      tag: 'a',
      attrs: {
        class: 'text ulpt user_link',
        href: '/@/' + p.name
      },
      children: [
        (p.title ? p.title + ' ' : '') + p.name,
        m('span.progress', [p.rating, perf])
      ]
    };
  },
  games: function(games) {
    return m('div.game_list.playing', games.map(miniGame));
  },
  clock: function(time) {
    return function(el, isUpdate) {
      if (!isUpdate) $(el).clock({
        time: time
      });
    };
  }
};
