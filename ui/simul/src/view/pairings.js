var m = require('mithril');
var util = require('./util');
var status = require('game/status');

var boardContent = m('div.cg-board-wrap', m('div.cg-board'));

function miniPairing(ctrl) {
  return function(pairing) {
    var game = pairing.game;
    var player = pairing.player;
    var result = pairing.game.status >= status.ids.mate ? (
      pairing.winnerColor === 'white' ? '1-0' : (pairing.winnerColor === 'black' ? '0-1' : '½/½')
    ) : '*';
    return m('div', [
      m('a', {
        href: '/' + game.id + '/' + game.orient,
        class: 'mini-board live-' + game.id + ' parse-fen is2d',
        'data-color': game.orient,
        'data-fen': game.fen,
        'data-lastmove': game.lastMove,
        config: function(el, isUpdate) {
          if (!isUpdate) lichess.parseFen($(el));
        }
      }, boardContent),
      m('div', {
        class: 'vstext clearfix' + (ctrl.data.host.gameId === game.id ? ' host' : '')
      }, [
        m('div.left', [
          util.playerVariant(ctrl, player).name,
          m('br'),
          result
        ]),
        m('div.right', [
          player.username,
          m('br'),
          player.title ? player.title + ' ' : '',
          player.rating
        ])
      ])
    ]);
  };
}

module.exports = function(ctrl) {
  return m('div.game_list.playing', ctrl.data.pairings.map(miniPairing(ctrl)));
};
