import type AnalyseCtrl from './ctrl';
import type {
  LocalAnalysisResult,
  ServerAnalysisDocument,
  AnalysisEngineInfo,
} from './local/localAnalysisEngine';
import { objectStorage } from 'lib/objectStorage';
import * as treeOps from 'lib/tree/ops';
import { memoize } from 'lib';

export type DiscloseState = undefined | 'expanded' | 'collapsed';

export class IdbTree {
  private cacheMap = new Map<string, State>(); // not persistent across page loads

  private moveDb = memoize(() =>
    objectStorage<{ root: Tree.Node | undefined }>({ store: 'analyse-state', db: 'lichess' }),
  );
  private collapseDb = memoize(() => objectStorage<Tree.Path[]>({ store: 'analyse-collapse' }));
  private analysisDb = memoize(() => objectStorage<LocalAnalysisResult>({ store: 'analyse-static' }));

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

  stepLine(fromPath: Tree.Path = this.ctrl.path, which: 'prev' | 'next' = 'next'): Tree.Path {
    let [path, kids] = this.familyOf(fromPath);
    while (path && kids.length < 2 && !this.ctrl.tree.pathIsMainline(path)) {
      [path, kids] = this.familyOf(path);
    }
    const i = kids.findIndex(k => fromPath.slice(path.length).startsWith(k.id));
    const stepTo = which === 'next' ? (kids[i + 1] ?? kids[0]) : (kids[i - 1] ?? kids[kids.length - 1]);
    return !stepTo ? fromPath : path + stepTo.id;
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

  discloseOf(node: Tree.Node | undefined, isMainline: boolean): DiscloseState {
    if (!node) return undefined;
    return this.isCollapsible(node, isMainline)
      ? this.ctrl.disclosureMode() && node.collapsed
        ? 'collapsed'
        : 'expanded'
      : undefined;
  }

  onAddNode(node: Tree.Node, path: Tree.Path): void {
    if (this.noop || this.cache.movesDirty) return;
    this.cache.movesDirty = !this.ctrl.tree.pathExists(path + node.id);
  }

  clear = async (what?: 'analysis' | 'collapse' | 'moves'): Promise<void> => {
    if (this.noop) return;
    await Promise.all([
      (!what || what === 'analysis') && this.analysisDb().then(db => db.remove(this.id)),
      (!what || what === 'collapse') && this.collapseDb().then(db => db.remove(this.id)),
      !this.ctrl.study && (!what || what === 'moves') && this.moveDb().then(db => db.remove(this.id)),
    ]);
    if (what !== 'analysis') site.reload();
    else this.cache.localAnalysisInfo = undefined;
  };

  async saveMoves(force = false): Promise<IDBValidKey | undefined> {
    if (this.noop || this.ctrl.study || !(this.cache.movesDirty || force)) return;
    return this.moveDb().then(db => db.put(this.id, { root: this.ctrl.tree.root }));
  }

  async saveAnalysis(analysis: LocalAnalysisResult) {
    if (this.noop) return;
    this.cache.localAnalysisInfo = analysis.serverDocument.engine;
    return this.analysisDb().then(db => db.put(this.id, analysis));
  }

  async serverDocument(): Promise<ServerAnalysisDocument> {
    return this.analysisDb()
      .then(db => db.get(this.id))
      .then(result => result.serverDocument);
  }

  async merge(): Promise<void> {
    if (this.noop || !('indexedDB' in window) || !window.indexedDB) return;
    try {
      this.cacheMap.set(this.id, { movesDirty: false });
      await Promise.all([
        this.analysisDb()
          .then(db => db.getOpt(this.id))
          .then(analysis => {
            if (analysis) {
              this.ctrl.mergeAnalysisData(analysis.localAnalysis, false);
              this.cache.localAnalysisInfo = analysis.localAnalysis.engine;
            }
          }),
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
                this.ctrl.tree.merge(moves.root);
                this.cache.movesDirty = true;
              }
            }),
      ]);
    } catch (e) {
      console.log('IDB error.', e);
    }
  }

  get hasLocalAnalysis(): boolean {
    return Boolean(this.cache.localAnalysisInfo?.nodesPerMove);
  }

  get localAnalysisInfo(): AnalysisEngineInfo | undefined {
    return this.cache.localAnalysisInfo;
  }

  get localAnalysisNpm(): number | undefined {
    return this.cache.localAnalysisInfo?.nodesPerMove;
  }

  get localAnalysisEngineId(): string | undefined {
    return this.cache.localAnalysisInfo?.id;
  }

  get localAnalysisIsBetter(): boolean {
    return (
      (this.cache.localAnalysisInfo?.nodesPerMove ?? 0) >
      (this.ctrl.data.analysis?.engine?.nodesPerMove ?? 0) + 200_000
    );
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

  private isCollapsible(node: Tree.Node, isMainline: boolean): boolean {
    if (!node) return false;
    const [first, second, third] = node.children.filter(n => this.ctrl.showStaticAnalysis() || !n.comp);
    return Boolean(
      first?.forceVariation ||
        third ||
        (second && treeOps.hasBranching(second, 6)) ||
        (isMainline &&
          this.ctrl.treeView.mode === 'column' &&
          (second || first?.comments?.filter(Boolean).length)),
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

  private collapseDefault() {
    const depthThreshold = 1;

    const traverse = (node: Tree.Node, depth: number) => {
      if (depth === depthThreshold && this.isCollapsible(node, false)) {
        node.collapsed = true;
      }
      node.children.forEach((n, i) => traverse(n, depth + (i === 0 ? 0 : 1)));
    };
    traverse(this.ctrl.tree.root, 0);
  }

  private familyOf(path: Tree.Path): [Tree.Path, Tree.Node[]] {
    const parentPath = path.slice(0, -2);
    return [
      parentPath,
      this.ctrl.tree.nodeAtPath(parentPath).children.filter(x => !x.comp || this.ctrl.showStaticAnalysis()),
    ];
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
}

type State = { movesDirty: boolean; localAnalysisInfo?: AnalysisEngineInfo };
