import { path as treePath } from 'tree';
import PuzzleCtrl from './ctrl';

export function canGoForward(ctrl: PuzzleCtrl): boolean {
  return ctrl.node.children.length > 0;
}

export function next(ctrl: PuzzleCtrl): void {
  const child = ctrl.node.children[0];
  if (!child) return;
  ctrl.userJump(ctrl.path + child.id);
}

export function prev(ctrl: PuzzleCtrl): void {
  ctrl.userJump(treePath.init(ctrl.path));
}

export function last(ctrl: PuzzleCtrl): void {
  const toInit = !treePath.contains(ctrl.path, ctrl.initialPath);
  ctrl.userJump(toInit ? ctrl.initialPath : treePath.fromNodeList(ctrl.mainline));
}

export function first(ctrl: PuzzleCtrl): void {
  const toInit = ctrl.path !== ctrl.initialPath && treePath.contains(ctrl.path, ctrl.initialPath);
  ctrl.userJump(toInit ? ctrl.initialPath : treePath.root);
}
