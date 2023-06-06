import { path as treePath } from 'tree';
import PuzzleController from './ctrl';

export function canGoForward(ctrl: PuzzleController): boolean {
  return ctrl.vm.node.children.length > 0;
}

export function next(ctrl: PuzzleController): void {
  const child = ctrl.vm.node.children[0];
  if (!child) return;
  ctrl.userJump(ctrl.vm.path + child.id);
}

export function prev(ctrl: PuzzleController): void {
  ctrl.userJump(treePath.init(ctrl.vm.path));
}

export function last(ctrl: PuzzleController): void {
  const toInit = !treePath.contains(ctrl.vm.path, ctrl.vm.initialPath);
  ctrl.userJump(toInit ? ctrl.vm.initialPath : treePath.fromNodeList(ctrl.vm.mainline));
}

export function first(ctrl: PuzzleController): void {
  const toInit = ctrl.vm.path !== ctrl.vm.initialPath && treePath.contains(ctrl.vm.path, ctrl.vm.initialPath);
  ctrl.userJump(toInit ? ctrl.vm.initialPath : treePath.root);
}
