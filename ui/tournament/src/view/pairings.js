var m = require('mithril');
var partial = require('chessground').util.partial;
var util = require('./util');
var status = require('game').status;

function user(pairing, user) {
  return {
    tag: 'span',
    attrs: {
      class: pairing.status >= status.ids.mate ? (
        pairing.winner ? (pairing.winner === user[0] ? 'win' : 'loss') : 'draw'
      ) : 'playing'
    },
    children: [user[1] || user[0]]
  };
}

var vs = m('em', ' vs ');

module.exports = function(ctrl) {
  var pairing = function(p) {
    return m('a', {
      key: p.gameId,
      href: '/' + p.gameId
    }, [
      user(p, p.user1),
      vs,
      user(p, p.user2)
    ]);
  };
  return ctrl.data.pairings.map(pairing);
};
