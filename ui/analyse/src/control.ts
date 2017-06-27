import AnalyseController from './ctrl';

import { path as treePath } from 'tree';

export function canGoForward(ctrl: AnalyseController): boolean {
  return ctrl.node.children.length > 0;
}

export function next(ctrl: AnalyseController): void {
  var child = ctrl.node.children[0];
  if (!child) return;
  ctrl.userJumpIfCan(ctrl.path + child.id);
}

export function prev(ctrl: AnalyseController): void {
  ctrl.userJumpIfCan(treePath.init(ctrl.path));
}

export function last(ctrl: AnalyseController): void {
  ctrl.userJumpIfCan(treePath.fromNodeList(ctrl.mainline));
}

export function first(ctrl: AnalyseController): void {
  ctrl.userJump(treePath.root);
}

export function enterVariation(ctrl: AnalyseController): void {
  var child = ctrl.node.children[1];
  if (!child) return;
  ctrl.userJump(ctrl.path + child.id);
}

export function exitVariation(ctrl: AnalyseController): void {
  if (ctrl.onMainline) return;
  var found, path = treePath.root;
  ctrl.nodeList.slice(1, -1).forEach(function(n: Tree.Node) {
    path += n.id;
    if (n.children[1]) found = path;
  });
  if (found) ctrl.userJump(found);
}
