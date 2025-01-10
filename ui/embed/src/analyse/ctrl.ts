import { update } from 'common/mini-board';
import type { Source, Status } from 'game';
import { makeNotation } from 'shogi/notation';
import { type TreeWrapper, build as makeTree, ops as treeOps, path as treePath } from 'tree';

export class AnalyseCtrl {
  tree: TreeWrapper;
  initialPath: Tree.Path;
  path: Tree.Path;
  nodeList: Tree.Node[];
  node: Tree.Node;

  groundElement: HTMLElement | undefined;

  autoScrollRequested = true;
  constructor(
    public data: AnalyseData,
    public study: StudyData | undefined,
    public redraw: () => void,
  ) {
    this.tree = makeTree(treeOps.reconstruct(this.data.treeParts));
    this.initialPath = treePath.root;

    this.jump(this.initialPath);

    this.initNotation();
  }

  jump = (path: Tree.Path): void => {
    this.path = path;
    this.nodeList = this.tree.getNodeList(path);
    this.node = treeOps.last(this.nodeList)!;
    this.autoScrollRequested = true;

    if (this.groundElement) update(this.groundElement, this.node.sfen, this.node.usi);
  };

  prev = (): void => {
    this.jump(treePath.init(this.path));
  };

  next = (): void => {
    const child = this.node.children[0];
    if (child) this.jump(this.path + child.id);
  };

  private captureRegex = /[a-z]/gi;
  private initNotation = (): void => {
    const variant = this.data.game.variant.key,
      captureRegex = this.captureRegex;
    function update(node: Tree.Node, prev?: Tree.Node) {
      if (prev && node.usi && !node.notation) {
        node.notation = makeNotation(prev.sfen, variant, node.usi, prev.usi);
        node.capture =
          (prev.sfen.split(' ')[0].match(captureRegex) || []).length >
          (node.sfen.split(' ')[0].match(captureRegex) || []).length;
      }
      node.children.forEach(c => update(c, node));
    }
    update(this.tree.root);
  };
}

export interface AnalyseData {
  treeParts: Tree.Node[];
  game: Game;
  orientation: Color;
}

export interface Game {
  id: string;
  status: Status;
  player: Color;
  plies: number;
  startedAtPly: number;
  startedAtStep: number;
  source: Source;
  speed: Speed;
  variant: Variant;
  winner?: Color;
  moveCentis?: number[];
  initialSfen?: string;
  importedBy?: string;
  perf: string;
  rated?: boolean;
}

export interface StudyData {
  id: string;
  name: string;
  chapter: { id: string };
  chapters: { id: string; name: string }[];
}
