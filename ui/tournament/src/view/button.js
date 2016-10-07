var m = require('mithril');
var partial = require('chessground').util.partial;
var xhr = require('../xhr');
var isIn = require('../tournament').isIn;

function orJoinSpinner(ctrl, f) {
  return ctrl.vm.joinSpinner ? m.trust(lichess.spinnerHtml) : f();
}

function withdraw(ctrl) {
  return orJoinSpinner(ctrl, function() {
    return m('button.fbt.text', {
      'data-icon': 'b',
      onclick: ctrl.withdraw
    }, ctrl.trans('withdraw'));
  });
}

function join(ctrl) {
  return orJoinSpinner(ctrl, function() {
    var joinable = ctrl.data.verdicts.accepted;
    return m('button.fbt.text', {
      class: joinable ? 'highlight' : 'disabled',
      'data-icon': 'G',
      onclick: function() {
        if (ctrl.data.private) {
          var p = prompt('Password');
          if (p !== null) ctrl.join(p);
        } else ctrl.join();
      }
    }, ctrl.trans('join'));
  });
}

module.exports = {
  withdraw: withdraw,
  join: join,
  joinWithdraw: function(ctrl) {
    if (!ctrl.userId) return m('a.fbt.text.highlight', {
      href: '/login?autoref=1',
      'data-icon': 'G'
    }, ctrl.trans('signIn'));
    if (ctrl.data.isFinished) return null;
    return isIn(ctrl) ? withdraw(ctrl) : join(ctrl);
  }
};
