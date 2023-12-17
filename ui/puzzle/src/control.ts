import { path as treePath } from 'tree';
import PuzzleCtrl from './ctrl';

export function canGoForward(ctrl: PuzzleCtrl): boolean {
  return ctrl.vm.node.children.length > 0;
}

export function next(ctrl: PuzzleCtrl): void {
  const child = ctrl.vm.node.children[0];
  if (!child) return;
  ctrl.userJump(ctrl.vm.path + child.id);
}

export function prev(ctrl: PuzzleCtrl): void {
  ctrl.userJump(treePath.init(ctrl.vm.path));
}

export function last(ctrl: PuzzleCtrl): void {
  const toInit = !treePath.contains(ctrl.vm.path, ctrl.vm.initialPath);
  ctrl.userJump(toInit ? ctrl.vm.initialPath : treePath.fromNodeList(ctrl.vm.mainline));
}

export function first(ctrl: PuzzleCtrl): void {
  const toInit = ctrl.vm.path !== ctrl.vm.initialPath && treePath.contains(ctrl.vm.path, ctrl.vm.initialPath);
  ctrl.userJump(toInit ? ctrl.vm.initialPath : treePath.root);
}
