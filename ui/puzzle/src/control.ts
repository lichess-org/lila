import { path as treePath } from 'tree';
import { MoveController } from './interfaces';

export function canGoForward(ctrl: MoveController): boolean {
  return ctrl.vm.node.children.length > 0;
}

export function next(ctrl: MoveController): void {
  const child = ctrl.vm.node.children[0];
  if (!child) return;
  ctrl.userJump(ctrl.vm.path + child.id);
}

export function prev(ctrl: MoveController): void {
  ctrl.userJump(treePath.init(ctrl.vm.path));
}

export function last(ctrl: MoveController): void {
  const toInit = !treePath.contains(ctrl.vm.path, ctrl.vm.initialPath);
  ctrl.userJump(toInit ? ctrl.vm.initialPath : treePath.fromNodeList(ctrl.vm.mainline));
}

export function first(ctrl: MoveController): void {
  const toInit = ctrl.vm.path !== ctrl.vm.initialPath && treePath.contains(ctrl.vm.path, ctrl.vm.initialPath);
  ctrl.userJump(toInit ? ctrl.vm.initialPath : treePath.root);
}
