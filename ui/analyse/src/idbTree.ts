import type { TreeNode, TreePath } from 'lib/tree/types';
import type AnalyseCtrl from './ctrl';
import { objectStorage, type ObjectStorage } from 'lib/objectStorage';
import * as treeOps from 'lib/tree/ops';

export type DiscloseState = undefined | 'expanded' | 'collapsed';

export class IdbTree {
  private dirty = false;
  private moveDb?: ObjectStorage<MoveState>;
  private collapseDb?: ObjectStorage<TreePath[]>;

  constructor(private ctrl: AnalyseCtrl) {}

  someCollapsedOf(collapsed: boolean, path = ''): boolean {
    return (
      this.ctrl.disclosureMode() &&
      this.ctrl.tree.walkUntilTrue(
        (n, m) => this.isCollapsible(n, m) && collapsed === Boolean(n.collapsed),
        path,
        path !== '',
      )
    );
  }

  // getCollapseTarget(path: TreePath): TreePath | undefined {
  //   if (this.ctrl.legacyVariationsProp()) return undefined;
  //   const { tree } = this.ctrl;
  //   const depth = (n: TreeNode) => n.ply - tree.root.ply;

  //   for (const node of tree
  //     .getNodeList(path)
  //     .slice(depth(tree.lastMainlineNode(path)))
  //     .reverse()) {
  //     if (!node.collapsed && this.isCollapsible(node)) return path.slice(0, depth(node) * 2);
  //   }
  //   return undefined;
  // }

  stepLine(fromPath: TreePath = this.ctrl.path, which: 'prev' | 'next' = 'next'): TreePath {
    let [path, kids] = this.familyOf(fromPath);
    while (path && kids.length < 2 && !this.ctrl.tree.pathIsMainline(path)) {
      [path, kids] = this.familyOf(path);
    }
    const i = kids.findIndex(k => fromPath.slice(path.length).startsWith(k.id));
    const stepTo = which === 'next' ? (kids[i + 1] ?? kids[0]) : (kids[i - 1] ?? kids[kids.length - 1]);
    return !stepTo ? fromPath : path + stepTo.id;
  }

  setCollapsed(path: TreePath, collapsed: boolean): void {
    this.ctrl.tree.updateAt(path, n => (n.collapsed = collapsed));
    this.saveCollapsed();
    this.ctrl.redraw();
  }

  setCollapsedFrom(from: TreePath, collapsed: boolean, thisBranchOnly = false): void {
    this.ctrl.tree.walkUntilTrue(
      (n, m) => {
        if (this.isCollapsible(n, m)) n.collapsed = collapsed;
        return false;
      },
      from,
      thisBranchOnly,
    );
    this.saveCollapsed();
    this.ctrl.redraw();
  }

  revealNode(path?: string): void {
    let save = false;
    const nodes = path === undefined ? this.ctrl.nodeList : this.ctrl.tree.getNodeList(path);
    for (let i = 0; i < nodes.length; i++) {
      const kid = nodes[i].children[0];
      if (nodes[i].collapsed && kid && nodes[i + 1] && kid !== nodes[i + 1]) {
        nodes[i].collapsed = false;
        save = true;
      }
    }
    if (save) this.saveCollapsed();
  }

  discloseOf(node: TreeNode | undefined, isMainline: boolean): DiscloseState {
    if (!node) return undefined;
    return this.isCollapsible(node, isMainline)
      ? this.ctrl.disclosureMode() && node.collapsed
        ? 'collapsed'
        : 'expanded'
      : undefined;
  }

  onAddNode(node: TreeNode, path: TreePath): void {
    if (this.ctrl.study || this.ctrl.synthetic || this.dirty) return;
    this.dirty = !this.ctrl.tree.pathExists(path + node.id);
  }

  clear = async (): Promise<void> => {
    await this.collapseDb?.remove(this.id);
    if (!this.ctrl.study && !this.ctrl.synthetic) await this.moveDb?.put(this.id, { root: undefined });
    site.reload();
  };

  async saveMoves(force = false): Promise<IDBValidKey | undefined> {
    if (this.ctrl.study || this.ctrl.synthetic || !(this.dirty || force)) return;
    return this.moveDb?.put(this.id, { root: this.ctrl.tree.root });
  }

  async merge(): Promise<void> {
    if (!('indexedDB' in window) || !window.indexedDB) return;
    try {
      if (!this.ctrl.study && !this.ctrl.synthetic) {
        this.moveDb ??= await objectStorage<MoveState>({ store: 'analyse-state', db: 'lichess' });
        const state = await this.moveDb.get(this.ctrl.data.game.id);
        if (state?.root) {
          this.ctrl.tree.merge(state.root);
          this.dirty = true;
        }
      }
      this.collapseDb ??= await objectStorage<TreePath[]>({ store: 'analyse-collapse' });
      const collapsedPaths = await this.collapseDb.getOpt(this.id);
      if (!collapsedPaths) return this.collapseDefault();
      for (const path of collapsedPaths) {
        this.ctrl.tree.updateAt(path, n => (n.collapsed = true));
      }
    } catch (e) {
      console.log('IDB error.', e);
    }
  }

  get isDirty(): boolean {
    return this.dirty;
  }

  private get id(): string {
    return this.ctrl.study?.data.chapter.id ?? this.ctrl.data.game.id;
  }

  private async saveCollapsed() {
    return this.collapseDb?.put(this.id, this.getCollapsed());
  }

  private isCollapsible(node: TreeNode, isMainline: boolean): boolean {
    if (!node) return false;
    const [first, second, third] = node.children.filter(n => this.ctrl.showFishnetAnalysis() || !n.comp);
    return Boolean(
      first?.forceVariation ||
        third ||
        (second && treeOps.hasBranching(second, 6)) ||
        (isMainline &&
          this.ctrl.treeView.mode === 'column' &&
          (second || first?.comments?.filter(Boolean).length)),
    );
  }

  private getCollapsed(): TreePath[] {
    const collapsedPaths: TreePath[] = [];
    function traverse(node: TreeNode, path: TreePath): void {
      if (node.collapsed) collapsedPaths.push(path);
      for (const c of node.children) traverse(c, path + c.id);
    }
    traverse(this.ctrl.tree.root, '');
    return collapsedPaths;
  }

  private collapseDefault() {
    const depthThreshold = 1;

    const traverse = (node: TreeNode, depth: number) => {
      if (depth === depthThreshold && this.isCollapsible(node, false)) {
        node.collapsed = true;
      }
      node.children.forEach((n, i) => traverse(n, depth + (i === 0 ? 0 : 1)));
    };
    traverse(this.ctrl.tree.root, 0);
  }

  private familyOf(path: TreePath): [TreePath, TreeNode[]] {
    const parentPath = path.slice(0, -2);
    return [
      parentPath,
      this.ctrl.tree.nodeAtPath(parentPath).children.filter(x => !x.comp || this.ctrl.showFishnetAnalysis()),
    ];
  }
}

interface MoveState {
  root: TreeNode | undefined;
}
