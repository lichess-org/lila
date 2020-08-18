const m = require('mithril');
const util = require('./util');
const status = require('game/status');
const opposite = require('chessground/util').opposite;

function miniPairing(ctrl) {
  return function(pairing) {
    const game = pairing.game,
      player = pairing.player;
    return m(`span.mini-game.mini-game-${game.id}.is2d`, {
      class: ctrl.data.host.gameId === game.id ? 'host' : '',
      'data-live': game.clock ? game.id : '',
      config(el, isUpdate) {
        if (!isUpdate) {
          window.lichess.miniGame.init(el, `${game.fen},${game.orient},${game.lastMove}`)
          window.lichess.powertip.manualUserIn(el);
        }
      }
    }, [
      m('span.mini-game__player', [
        m('a.mini-game__user.ulpt', {
          href: `/@/${player.name}`
        }, [
          m('span.name', player.title ? [m('span.title', player.title), ' ', player.name] : [player.name]),
          ' ',
          m('span.rating', player.rating)
        ]),
        game.clock ?
        m(`span.mini-game__clock.mini-game__clock--${opposite(game.orient)}`, {
          'data-time': game.clock[opposite(game.orient)]
        }) :
        m('span.mini-game__result', game.winner ? (game.winner == game.orient ? 0 : 1) : '½'),
      ]),
      m('a.cg-wrap', {
        href: `/${game.id}/${game.orient}`
      }),
      m('span.mini-game__player', [
        m('span'),
        game.clock ?
        m(`span.mini-game__clock.mini-game__clock--${game.orient}`, {
          'data-time': game.clock[game.orient]
        }) :
        m('span.mini-game__result', game.winner ? (game.winner == game.orient ? 1 : 0) : '½'),
      ]),
    ])
  };
}

module.exports = function(ctrl) {
  return m('div.game-list.now-playing.box__pad', ctrl.data.pairings.map(miniPairing(ctrl)));
};
