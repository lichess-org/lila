import { path as treePath } from 'lib/tree/tree';
import type { TreeNode } from 'lib/tree/types';

import type AnalyseCtrl from './ctrl';

export default class Navigate {
  constructor(private readonly ctrl: AnalyseCtrl) {}

  next = (): void => {
    if (this.ctrl.retro?.preventGoingToNextMove()) return;
    if (this.ctrl.fork.proceed()) return;
    const child = this.ctrl.node.children[0];
    if (child) this.ctrl.userJumpIfCan(this.ctrl.path + child.id);
  };

  prev = (): void => this.ctrl.userJumpIfCan(treePath.init(this.ctrl.path));

  last = (): void => this.ctrl.userJumpIfCan(treePath.fromNodeList(this.ctrl.mainline));

  first = (): void => this.ctrl.userJump(treePath.root);

  previousBranch = (): void => {
    let path = treePath.init(this.ctrl.path),
      parent = this.ctrl.tree.nodeAtPath(path);
    while (path.length && parent && this.ctrl.visibleChildren(parent).length < 2) {
      path = treePath.init(path);
      parent = this.ctrl.tree.nodeAtPath(path);
    }
    this.ctrl.userJumpIfCan(path);
  };

  nextBranch = (): void => {
    let child = this.ctrl.visibleChildren()[this.ctrl.fork.selectedIndex];
    let path = this.ctrl.path;
    while (child && child.children.length < 2) {
      path += child.id;
      child = child.children[0];
    }
    if (child) this.ctrl.userJumpIfCan(path + child.id);
    else if (this.ctrl.tree.pathIsMainline(this.ctrl.path)) this.last();
    else this.exitVariation();
  };

  private exitVariation = (): void => {
    if (this.ctrl.onMainline) return;
    let found,
      path = treePath.root;
    this.ctrl.nodeList.slice(1, -1).forEach((n: TreeNode) => {
      path += n.id;
      if (n.children[1]) found = path;
    });
    if (found) this.ctrl.userJump(found);
  };
}
