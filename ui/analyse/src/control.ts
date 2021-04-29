import AnalyseCtrl from './ctrl';

import { path as treePath } from 'tree';

export function canGoForward(ctrl: AnalyseCtrl): boolean {
  return ctrl.node.children.length > 0;
}

export function next(ctrl: AnalyseCtrl): void {
  const child = ctrl.node.children[0];
  if (child) ctrl.userJumpIfCan(ctrl.path + child.id);
}

export function prev(ctrl: AnalyseCtrl): void {
  ctrl.userJumpIfCan(treePath.init(ctrl.path));
}

export function last(ctrl: AnalyseCtrl): void {
  ctrl.userJumpIfCan(treePath.fromNodeList(ctrl.mainline));
}

export function first(ctrl: AnalyseCtrl): void {
  ctrl.userJump(treePath.root);
}

export function enterVariation(ctrl: AnalyseCtrl): void {
  let child = ctrl.node.children[1];
  if (child) ctrl.userJump(ctrl.path + child.id);
}

export function exitVariation(ctrl: AnalyseCtrl): void {
  if (ctrl.onMainline) return;
  let found,
    path = treePath.root;
  ctrl.nodeList.slice(1, -1).forEach(function (n: Tree.Node) {
    path += n.id;
    if (n.children[1]) found = path;
  });
  if (found) ctrl.userJump(found);
}
