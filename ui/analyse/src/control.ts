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
