var m = require('mithril');
var game = require('../game');
var renderUser = require('./user');

function blursOf(ctrl, player) {
  if (player.blurs) return m('p', [
    renderUser(ctrl, player, player.color),
    ' ' + player.blurs.nb + '/' + game.nbMoves(ctrl.data, player.color) + ' blurs = ',
    m('strong', player.blurs.percent + '%')
  ]);
}

function holdOf(ctrl, player) {
  var h = player.hold;
  if (h) return m('p', [
    renderUser(ctrl, player, player.color),
    ' hold alert',
    m('br'),
    'ply=' + h.ply + ', mean=' + h.mean + ' ms, SD=' + h.sd
  ]);
}

module.exports = {
  blursOf: blursOf,
  holdOf: holdOf
};
