var treePath = require('./tree/path');

function canEnterVariation(ctrl) {
  return ctrl.vm.node.children.length > 1;
}

function jumpToPath(ctrl, path) {
  if (ctrl.canJumpTo(path)) ctrl.userJump(path);
}

function sharedStart(p1, p2) {
  var L = p1.length,
    i = 0;
  while (i < L && p1.charAt(i) === p2.charAt(i)) i++;
  return p1.substring(0, i);
}

module.exports = {

  canGoForward: function(ctrl) {
    return ctrl.vm.node.children.length > 0;
  },

  next: function(ctrl) {
    var child = ctrl.vm.node.children[0];
    if (!child) return;
    jumpToPath(ctrl, ctrl.vm.path + child.id);
  },

  prev: function(ctrl) {
    jumpToPath(ctrl, treePath.init(ctrl.vm.path));
  },

  last: function(ctrl) {
    jumpToPath(ctrl, treePath.fromNodeList(ctrl.vm.mainline));
  },

  first: function(ctrl) {
    ctrl.userJump(treePath.root);
  },

  enterVariation: function(ctrl) {
    var parentPath = treePath.init(ctrl.vm.path);
    var parent = ctrl.tree.nodeAtPath(parentPath);
    var child = parent.children[1];
    if (!child) return;
    ctrl.userJump(parentPath + child.id);
  },

  exitVariation: function(ctrl) {
    var commonPath = sharedStart(ctrl.vm.path, treePath.fromNodeList(ctrl.vm.mainline));
    var node = ctrl.tree.nodeAtPath(commonPath).children[0];
    node && ctrl.userJump(commonPath + node.id);
  }
};
