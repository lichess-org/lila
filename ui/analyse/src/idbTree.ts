import type AnalyseCtrl from './ctrl';
import { objectStorage, type ObjectStorage } from 'lib/objectStorage';
import { hasBranching } from 'lib/tree/ops';

export class IdbTree {
  private noCollapse = localStorage.getItem('analyse.disclosure.enabled') === 'false';
  private dirty = false;
  private moveDb?: ObjectStorage<MoveState>;
  private collapseDb?: ObjectStorage<Tree.Path[]>;

  constructor(private ctrl: AnalyseCtrl) {}

  someCollapsedOf(collapsed: boolean, path = ''): boolean {
    if (this.noCollapse) return false;
    return this.ctrl.tree.walkUntilTrue(
      (n, m) => this.isCollapsible(n, m) && collapsed === Boolean(n.collapsed),
      path,
      path !== '',
    );
  }

  getCollapseTarget(path: Tree.Path): Tree.Path | undefined {
    if (this.noCollapse) return undefined;
    const { tree } = this.ctrl;
    const depth = (n: Tree.Node) => n.ply - tree.root.ply;

    for (const node of tree
      .getNodeList(path)
      .slice(depth(tree.lastMainlineNode(path)))
      .reverse()) {
      if (!node.collapsed && this.isCollapsible(node, false)) return path.slice(0, depth(node) * 2);
    }
    return undefined;
  }

  setCollapsed(path: Tree.Path, collapsed: boolean): void {
    this.ctrl.tree.updateAt(path, n => (n.collapsed = collapsed));
    this.saveCollapsed();
    this.ctrl.redraw();
  }

  setCollapsedFrom(from: Tree.Path, collapsed: boolean, thisBranchOnly = false): void {
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

  discloseOf(node: Tree.Node | undefined, mainColumn = false): false | 'expanded' | 'collapsed' {
    if (!node || this.noCollapse) return false;
    return node.collapsed ? 'collapsed' : this.isCollapsible(node, mainColumn) ? 'expanded' : false;
  }

  onAddNode(node: Tree.Node, path: Tree.Path): void {
    if (this.ctrl.study || this.ctrl.synthetic || this.dirty) return;
    this.dirty = !this.ctrl.tree.pathExists(path + node.id);
  }

  clear = async (): Promise<void> => {
    await this.collapseDb?.remove(this.id);
    if (!this.ctrl.study && !this.ctrl.synthetic)
      await this.moveDb?.put(this.id, {
        root: undefined,
      });
    site.reload();
  };

  async saveMoves(force = false): Promise<IDBValidKey | undefined> {
    if (this.ctrl.study || this.ctrl.synthetic || !(this.dirty || force)) return;
    return this.moveDb?.put(this.id, {
      root: this.ctrl.tree.root,
    });
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
      for (const path of (await this.collapseDb.getOpt(this.id)) ?? []) {
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

  private isCollapsible(node: Tree.Node, isMainline: boolean): boolean {
    if (this.noCollapse || !node) return false;
    const { tree, treeView, showComputer } = this.ctrl;
    //if (node === tree.root && treeView.inline()) return false;
    const [main, second, third] = node.children.filter(x => showComputer() || !x.comp);
    return Boolean(
      third ||
        (main && Boolean(main.comments?.length) && isMainline && !treeView.inline()) ||
        (main && main.forceVariation && treeView.inline()) ||
        (second && ((isMainline && !treeView.inline()) || hasBranching(second, 6))),
    );
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
}

export interface MoveState {
  root: Tree.Node | undefined;
}
