var m = require('mithril');
var partial = require('chessground').util.partial;
var xhr = require('../xhr');

function orJoinSpinner(ctrl, f) {
  return ctrl.vm.joinSpinner ? m.trust(lichess.spinnerHtml) : f();
}

function withdraw(ctrl) {
  return orJoinSpinner(ctrl, function() {
    return m('button.button.right.text', {
      'data-icon': 'b',
      onclick: ctrl.withdraw
    }, ctrl.trans('withdraw'));
  });
}

function join(ctrl) {
  return orJoinSpinner(ctrl, function() {
    var joinable = ctrl.data.verdicts.accepted;
    return m('button.button.right.text', {
      class: joinable ? 'glowed' : 'disabled',
      'data-icon': 'G',
      onclick: ctrl.join
    }, ctrl.trans('join'));
  });
}

module.exports = {
  withdraw: withdraw,
  join: join,
  joinWithdraw: function(ctrl) {
    return (!ctrl.userId || ctrl.data.isFinished) ? null : (
      ctrl.data.me && !ctrl.data.me.withdraw ? withdraw(ctrl) : join(ctrl));

  }
};
