var m = require('mithril');

function yep(ctrl) {
  ctrl.study.notif.set({
    text: 'Excellent!',
    class: 'ok',
    duration: 2000
  });
}

function nope(ctrl) {
  ctrl.study.notif.set({
    text: 'Guess again!',
    class: 'error',
    duration: 2000
  });
}

module.exports = function(ctrl, parentPath, node) {
  var conceal = ctrl.study.data.chapter.conceal;
  if (!conceal || node.ply < conceal) return;
  if (!ctrl.tree.pathIsMainline(parentPath)) return;
  if (ctrl.tree.pathIsMainline(parentPath + node.id)) yep(ctrl);
  else nope(ctrl);
};
