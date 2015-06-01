var m = require('mithril');
var partial = require('chessground').util.partial;
var util = require('./util');
var status = require('game').status;

function user(pairing, username) {
  var id = username.toLowerCase();
  return {
    tag: 'span',
    attrs: {
      class: pairing.st >= status.ids.mate ? (
        pairing.wi ? (pairing.wi === id ? 'win' : 'loss') : 'draw'
      ) : 'playing'
    },
    children: [username]
  };
}

var vs = m('em', ' vs ');

module.exports = function(ctrl) {
  var pairing = function(p) {
    return m('a', {
      key: p.id,
      href: '/' + p.id
    }, [
      user(p, p.u1),
      vs,
      user(p, p.u2)
    ]);
  };
  return ctrl.data.pairings.map(pairing);
};
