var m = require('mithril');

var boardContent = m('div.cg-board-wrap', m('div.cg-board'));

function miniGame(game) {
  return m('div', [
    m('a', {
      key: game.id,
      href: '/' + game.id + (game.color === 'white' ? '' : '/black'),
      class: 'mini-board live-' + game.id + ' parse-fen is2d',
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

module.exports = function(ctrl) {

  if (!ctrl.vm.answer) return;

  return m('div.game-sample.box', [
    m('div.top', 'Some of the games used to generate this insight'),
    m('div.boards.game_list', ctrl.vm.answer.games.map(miniGame))
  ]);
}
