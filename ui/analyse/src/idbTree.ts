import type AnalyseCtrl from './ctrl';
import { objectStorage, type ObjectStorage } from 'lib/objectStorage';

export type DiscloseState = undefined | 'expanded' | 'collapsed';

export class IdbTree {
  private dirty = false;
  private moveDb?: ObjectStorage<MoveState>;
  private collapseDb?: ObjectStorage<Tree.Path[]>;

  constructor(private ctrl: AnalyseCtrl) {}

  someCollapsedOf(collapsed: boolean, path = ''): boolean {
    if (!this.ctrl.disclosureMode()) return false;
    return this.ctrl.tree.walkUntilTrue(
      n => this.isCollapsible(n) && collapsed === Boolean(n.collapsed),
      path,
      path !== '',
    );
  }

  // getCollapseTarget(path: Tree.Path): Tree.Path | undefined {
  //   if (this.ctrl.legacyVariationsProp()) return undefined;
  //   const { tree } = this.ctrl;
  //   const depth = (n: Tree.Node) => n.ply - tree.root.ply;

  //   for (const node of tree
  //     .getNodeList(path)
  //     .slice(depth(tree.lastMainlineNode(path)))
  //     .reverse()) {
  //     if (!node.collapsed && this.isCollapsible(node)) return path.slice(0, depth(node) * 2);
  //   }
  //   return undefined;
  // }

  nextLine(fromPath: Tree.Path = this.ctrl.path): Tree.Path {
    let [path, node, kids] = this.familyOf(fromPath);
    while (path && kids.length < 2 && !this.ctrl.tree.pathIsMainline(path)) {
      [path, node, kids] = this.familyOf(path);
    }
    const lineIndex = kids.findIndex(k => fromPath.slice(path.length).startsWith(k.id));
    const nextKid = kids[lineIndex + 1] ?? kids[0];
    if (!nextKid || path + nextKid.id === fromPath) return fromPath;
    return path + nextKid.id;
  }

  setCollapsed(path: Tree.Path, collapsed: boolean): void {
    this.ctrl.tree.updateAt(path, n => (n.collapsed = collapsed));
    this.saveCollapsed();
    this.ctrl.redraw();
  }

  setCollapsedFrom(from: Tree.Path, collapsed: boolean, thisBranchOnly = false): void {
    this.ctrl.tree.walkUntilTrue(
      n => {
        if (this.isCollapsible(n)) n.collapsed = collapsed;
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

  discloseOf(node: Tree.Node | undefined): DiscloseState {
    if (!this.ctrl.disclosureMode() || !node) return undefined;
    return node.collapsed ? 'collapsed' : this.isCollapsible(node) ? 'expanded' : undefined;
  }

  onAddNode(node: Tree.Node, path: Tree.Path): void {
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
      this.collapseDb ??= await objectStorage<Tree.Path[]>({ store: 'analyse-collapse' });
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

  private isCollapsible(node: Tree.Node): boolean {
    if (!this.ctrl.disclosureMode() || !node) return false;
    const [first, second] = node.children;
    return Boolean(second || first?.comments?.length || first?.forceVariation);
  }

  private getCollapsed(): Tree.Path[] {
    const collapsedPaths: Tree.Path[] = [];
    function traverse(node: Tree.Node, path: Tree.Path): void {
      if (node.collapsed) collapsedPaths.push(path);
      for (const c of node.children) traverse(c, path + c.id);
    }
    traverse(this.ctrl.tree.root, '');
    return collapsedPaths;
  }

  private collapseDefault() {
    const depthThreshold = 1;

    const traverse = (node: Tree.Node, depth: number) => {
      if (depth === depthThreshold && this.isCollapsible(node)) {
        node.collapsed = true;
      }
      node.children.forEach((n, i) => traverse(n, depth + (i === 0 ? 0 : 1)));
    };
    traverse(this.ctrl.tree.root, 0);
  }

  private familyOf(path: Tree.Path): [Tree.Path, Tree.Node, Tree.Node[]] {
    const parentPath = path.slice(0, -2);
    const parentNode = this.ctrl.tree.nodeAtPath(parentPath);
    return [parentPath, parentNode, parentNode.children.filter(x => !x.comp || this.ctrl.showComputer())];
  }
}

interface MoveState {
  root: Tree.Node | undefined;
}
