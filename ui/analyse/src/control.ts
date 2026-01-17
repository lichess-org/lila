import type { TreeNode } from 'lib/tree/types';
import type AnalyseCtrl from './ctrl';

import { path as treePath } from 'lib/tree/tree';

export function next(ctrl: AnalyseCtrl): void {
  if (ctrl.retro?.preventGoingToNextMove()) return;
  if (ctrl.fork.proceed()) return;
  const child = ctrl.node.children[0];
  if (child) ctrl.userJumpIfCan(ctrl.path + child.id);
}

export const prev = (ctrl: AnalyseCtrl): void => ctrl.userJumpIfCan(treePath.init(ctrl.path));

export const last = (ctrl: AnalyseCtrl): void => ctrl.userJumpIfCan(treePath.fromNodeList(ctrl.mainline));

export const first = (ctrl: AnalyseCtrl): void => ctrl.userJump(treePath.root);

function exitVariation(ctrl: AnalyseCtrl): void {
  if (ctrl.onMainline) return;
  let found,
    path = treePath.root;
  ctrl.nodeList.slice(1, -1).forEach(function (n: TreeNode) {
    path += n.id;
    if (n.children[1]) found = path;
  });
  if (found) ctrl.userJump(found);
}

export function previousBranch(ctrl: AnalyseCtrl): void {
  let path = treePath.init(ctrl.path),
    parent = ctrl.tree.nodeAtPath(path);
  while (path.length && parent && ctrl.visibleChildren(parent).length < 2) {
    path = treePath.init(path);
    parent = ctrl.tree.nodeAtPath(path);
  }
  ctrl.userJumpIfCan(path);
}

export function nextBranch(ctrl: AnalyseCtrl): void {
  let child = ctrl.visibleChildren()[ctrl.fork.selectedIndex];
  let path = ctrl.path;
  while (child && child.children.length < 2) {
    path += child.id;
    child = child.children[0];
  }
  if (child) ctrl.userJumpIfCan(path + child.id);
  else if (ctrl.tree.pathIsMainline(ctrl.path)) last(ctrl);
  else exitVariation(ctrl);
}
