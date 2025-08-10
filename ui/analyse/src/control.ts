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

export function nextLine(ctrl: AnalyseCtrl): boolean {
  const { path } = ctrl;
  let iterPath = treePath.init(path);
  let iter = ctrl.tree.nodeAtPath(iterPath);
  while (iterPath && iter.children.length < 2 && !ctrl.tree.pathIsMainline(iterPath)) {
    iterPath = treePath.init(iterPath);
    iter = ctrl.tree.nodeAtPath(iterPath);
  }
  const kids = iter?.children ?? [];
  const nextKid =
    kids[kids.findIndex(k => k.id === path.slice(iterPath.length, iterPath.length + 2)) + 1] ?? kids[0];
  if (!nextKid || iterPath + nextKid.id === path) return toggleDisclosure(ctrl);
  ctrl.userJumpIfCan(iterPath + nextKid.id, true);
  return true;
}

export function toggleDisclosure(ctrl: AnalyseCtrl): boolean {
  const parentPath = treePath.init(ctrl.path);
  const disclose = ctrl.idbTree.discloseOf(ctrl.tree.nodeAtPath(parentPath));
  if (!disclose) return false;

  ctrl.idbTree.setCollapsed(parentPath, disclose === 'expanded');
  return true;
}
