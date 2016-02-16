var m = require('mithril');
var partial = require('chessground').util.partial;
var xhr = require('../xhr');

function orJoinLoader(ctrl, f) {
  return ctrl.vm.joinLoader ? m.trust(lichess.spinnerHtml) : f();
}

function withdraw(ctrl) {
  return orJoinLoader(ctrl, function() {
    return m('button.button.right.text', {
      'data-icon': 'b',
      onclick: ctrl.withdraw
    }, ctrl.trans('withdraw'));
  });
}

function join(ctrl) {
  return orJoinLoader(ctrl, function() {
    return m('button.button.right.text.glowed', {
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
