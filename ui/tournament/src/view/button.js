var m = require('mithril');
var partial = require('chessground').util.partial;
var tournament = require('../tournament');
var xhr = require('../xhr');

function withdraw(ctrl) {
  return m('button.button.right.text', {
    'data-icon': 'b',
    onclick: partial(xhr.withdraw, ctrl)
  }, ctrl.trans('withdraw'));
}

function deleteTournament(ctrl) {
  return m('button.button.right.text', {
    'data-icon': 'L',
    onclick: partial(xhr.withdraw, ctrl)
  }, 'Delete');
}

function join(ctrl) {
  return m('button.button.right.text', {
    'data-icon': 'G',
    onclick: partial(xhr.join, ctrl)
  }, ctrl.trans('join'));
}

function start(ctrl) {
  return ctrl.data.enoughPlayersToStart ? m('button.button.right.text.glowing[data-icon=E]', {
    onclick: partial(xhr.start, ctrl)
  }, 'Start now') : m('button.button.right[disabled]', 'Start now');
}

module.exports = {
  start: start,
  withdraw: withdraw,
  join: join,
  deleteTournament: deleteTournament,
  joinWithdraw: function(ctrl) {
    return (!ctrl.userId || ctrl.data.isFinished) ? null : (
      tournament.containsMe(ctrl) ? withdraw(ctrl) : join(ctrl));

  }
};
