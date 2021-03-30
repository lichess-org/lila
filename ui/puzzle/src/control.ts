import { path as treePath } from 'tree';
import { KeyboardController } from './interfaces';

export function canGoForward(ctrl: KeyboardController): boolean {
  return ctrl.vm.node.children.length > 0;
}

export function next(ctrl: KeyboardController): void {
  const child = ctrl.vm.node.children[0];
  if (!child) return;
  ctrl.userJump(ctrl.vm.path + child.id);
}

export function prev(ctrl: KeyboardController): void {
  ctrl.userJump(treePath.init(ctrl.vm.path));
}

export function last(ctrl: KeyboardController): void {
  const toInit = !treePath.contains(ctrl.vm.path, ctrl.vm.initialPath);
  ctrl.userJump(toInit ? ctrl.vm.initialPath : treePath.fromNodeList(ctrl.vm.mainline));
}

export function first(ctrl: KeyboardController): void {
  const toInit = ctrl.vm.path !== ctrl.vm.initialPath && treePath.contains(ctrl.vm.path, ctrl.vm.initialPath);
  ctrl.userJump(toInit ? ctrl.vm.initialPath : treePath.root);
}
