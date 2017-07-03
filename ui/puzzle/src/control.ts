import { path as treePath } from 'tree';

export function canGoForward(ctrl) {
  return ctrl.vm.node.children.length > 0;
}

export function next(ctrl) {
  var child = ctrl.vm.node.children[0];
  if (!child) return;
  ctrl.userJump(ctrl.vm.path + child.id);
}

export function prev(ctrl) {
  ctrl.userJump(treePath.init(ctrl.vm.path));
}

export function last(ctrl) {
  var toInit = !treePath.contains(ctrl.vm.path, ctrl.vm.initialPath);
  ctrl.userJump(
    toInit ? ctrl.vm.initialPath : treePath.fromNodeList(ctrl.vm.mainline)
  );
}

export function first(ctrl) {
  var toInit = ctrl.vm.path !== ctrl.vm.initialPath && treePath.contains(ctrl.vm.path, ctrl.vm.initialPath);
  ctrl.userJump(
    toInit ? ctrl.vm.initialPath : treePath.root
  );
}
