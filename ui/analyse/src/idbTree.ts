import { memoize } from 'lib';
import { objectStorage } from 'lib/objectStorage';
import { completeNode } from 'lib/tree/node';
import * as treeOps from 'lib/tree/ops';
import type { TreeNodeLite, TreePath } from 'lib/tree/types';

import type AnalyseCtrl from './ctrl';

export type DiscloseState = undefined | 'expanded' | 'collapsed';
export class IdbTree {
  private readonly cacheMap = new Map<string, State>();
  private readonly collapseDb = memoize(() => objectStorage<TreePath[]>({ store: 'analyse-collapse' }));
  private readonly moveDb = memoize(() =>
    objectStorage<{ root: TreeNodeLite | undefined }>({ store: 'analyse-state', db: 'lichess' }),
  );

  constructor(private readonly ctrl: AnalyseCtrl) {}

  someCollapsedOf(collapsed: boolean, path = ''): boolean {
    return (
      this.ctrl.settings.disclosureMode &&
      this.ctrl.tree.walkUntilTrue(
        (n, m) => this.isCollapsible(n, m) && collapsed === Boolean(n.collapsed),
        path,
        path !== '',
      )
    );
  }

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

  discloseOf(node: TreeNodeLite | undefined, isMainline: boolean): DiscloseState {
    if (!node) return undefined;
    return this.isCollapsible(node, isMainline)
      ? this.ctrl.settings.disclosureMode && node.collapsed
        ? 'collapsed'
        : 'expanded'
      : undefined;
  }

  onAddNode(node: TreeNodeLite, path: TreePath): void {
    if (this.noop || this.cache.movesDirty) return;
    this.cache.movesDirty = !this.ctrl.tree.pathExists(path + node.id);
  }

  clear = async (what?: 'analysis' | 'collapse' | 'moves'): Promise<void> => {
    if (this.noop) return;
    await Promise.all([
      (!what || what === 'collapse') && this.collapseDb().then(db => db.remove(this.id)),
      !this.ctrl.study && (!what || what === 'moves') && this.moveDb().then(db => db.remove(this.id)),
    ]);
    site.reload();
  };

  async saveMoves(force = false): Promise<IDBValidKey | undefined> {
    if (this.noop || this.ctrl.study || !(this.cache.movesDirty || force)) return;
    return this.moveDb().then(db =>
      db.put(this.id, { root: treeOps.structuredCloneLite(this.ctrl.tree.root) }),
    );
  }

  async merge(): Promise<void> {
    if (this.noop || !('indexedDB' in window) || !window.indexedDB) return;
    try {
      this.cacheMap.set(this.id, { movesDirty: false });
      await Promise.all([
        this.collapseDb()
          .then(db => db.getOpt(this.id))
          .then(collapsedPaths => {
            if (!collapsedPaths) return this.collapseDefault();
            for (const path of collapsedPaths) {
              this.ctrl.tree.updateAt(path, n => (n.collapsed = true));
            }
          }),
        !this.ctrl.study &&
          this.moveDb()
            .then(db => db.getOpt(this.id))
            .then(moves => {
              if (moves?.root) {
                this.ctrl.tree.merge(completeNode(this.ctrl.variantKey)(moves.root));
                this.cache.movesDirty = true;
              }
            }),
      ]);
    } catch (e) {
      console.log('IDB error.', e);
    }
  }

  get movesDirty(): boolean {
    return this.cache.movesDirty;
  }

  private get id(): string {
    return this.ctrl.study?.data.chapter.id ?? this.ctrl.data.game.id;
  }

  private get noop(): boolean {
    return this.id === 'synthetic';
  }

  private get cache() {
    if (this.cacheMap.has(this.id)) return this.cacheMap.get(this.id)!;
    const state: State = { movesDirty: false };
    this.cacheMap.set(this.id, state);
    return state;
  }

  private async saveCollapsed() {
    return this.collapseDb().then(db => db.put(this.id, this.getCollapsed()));
  }

  private isCollapsible(node: TreeNodeLite, isMainline: boolean): boolean {
    const [first, second, third] = node.children.filter(
      n => this.ctrl.settings.showStaticAnalysis || !n.comp,
    );
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
    function traverse(node: TreeNodeLite, path: TreePath): void {
      if (node.collapsed) collapsedPaths.push(path);
      for (const c of node.children) traverse(c, path + c.id);
    }
    traverse(this.ctrl.tree.root, '');
    return collapsedPaths;
  }

  private collapseDefault() {
    const depthThreshold = 1;

    const traverse = (node: TreeNodeLite, depth: number) => {
      if (depth === depthThreshold && this.isCollapsible(node, false)) {
        node.collapsed = true;
      }
      node.children.forEach((n, i) => traverse(n, depth + (i === 0 ? 0 : 1)));
    };
    traverse(this.ctrl.tree.root, 0);
  }

  private familyOf(path: TreePath): [TreePath, TreeNodeLite[]] {
    const parentPath = path.slice(0, -2);
    return [
      parentPath,
      this.ctrl.tree
        .nodeAtPath(parentPath)
        .children.filter(x => !x.comp || this.ctrl.settings.showStaticAnalysis),
    ];
  }
}

type State = { movesDirty: boolean };
