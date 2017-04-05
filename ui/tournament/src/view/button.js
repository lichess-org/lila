var m = require('mithril');
var isIn = require('../tournament').isIn;

function orJoinSpinner(ctrl, f) {
  return ctrl.vm.joinSpinner ? m.trust(lichess.spinnerHtml) : f();
}

function withdraw(ctrl) {
  return orJoinSpinner(ctrl, function() {
    var pause = ctrl.data.isStarted;
    return m('button.fbt.text', {
      'data-icon': pause ? 'Z' : 'b',
      onclick: ctrl.withdraw
    }, ctrl.trans.noarg(pause ? 'pause' : 'withdraw'));
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
