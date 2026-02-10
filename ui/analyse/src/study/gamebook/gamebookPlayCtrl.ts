import type AnalyseCtrl from '@/ctrl';
import { path as treePath } from 'lib/tree/tree';
import { makeShapesFromUci } from '@/autoShape';
import type { Shape, TreePath } from 'lib/tree/types';

export type Feedback = 'play' | 'good' | 'bad' | 'end';

export interface State {
  feedback: Feedback;
  comment?: string;
  hint?: string;
  showHint: boolean;
  init: boolean; // on root path
}

export default class GamebookPlayCtrl {
  state: State;

  constructor(
    readonly root: AnalyseCtrl,
    readonly chapterId: string,
    readonly redraw: () => void,
  ) {
    this.makeState();
  }

  private makeState = (): void => {
    const node = this.root.node,
      nodeComment = (node.comments || [])[0],
      state: Partial<State> = {
        init: this.root.path === '',
        comment: nodeComment ? nodeComment.text : undefined,
        showHint: false,
      },
      parPath = treePath.init(this.root.path),
      parNode = this.root.tree.nodeAtPath(parPath);
    if (
      (this.root.onMainline && !node.children[0]) ||
      (!this.root.onMainline && !this.root.tree.pathIsMainline(parPath))
    )
      state.feedback = 'end';
    else if (this.isMyMove()) {
      state.feedback = 'play';
      state.hint = (node.gamebook || {}).hint;
    } else if (this.root.onMainline) state.feedback = 'good';
    else {
      state.feedback = 'bad';
      if (!state.comment) state.comment = parNode.children[0].gamebook?.deviation;
    }
    this.state = state as State;
    if (!state.comment) {
      if (state.feedback === 'good') setTimeout(this.next, this.root.path ? 1000 : 300);
      else if (state.feedback === 'bad') setTimeout(this.retry, 800);
    }
  };

  isMyMove = () => this.root.turnColor() === this.root.data.orientation;

  movableColor = () =>
    ['play', 'good'].includes(this.state.feedback) ? this.root.data.orientation : undefined;

  retry = () => {
    let path = this.root.path;
    while (path && !this.root.tree.pathIsMainline(path)) path = treePath.init(path);
    this.root.userJump(path);
    this.redraw();
  };

  next = () => {
    if (!this.isMyMove()) {
      const child = this.root.node.children[0];
      if (child) this.root.userJump(this.root.path + child.id);
    }
    this.redraw();
  };

  onSpace = () => {
    switch (this.state.feedback) {
      case 'bad':
        this.retry();
        break;
      case 'end': {
        this.root.study!.goToNextChapter();
        break;
      }
      default:
        this.next();
    }
  };

  onPremoveSet = () => {
    this.next();
  };

  hint = () => {
    if (this.state.hint) this.state.showHint = !this.state.showHint;
  };

  solution = () => {
    this.root.chessground.setShapes(
      makeShapesFromUci(this.root.turnColor(), this.root.node.children[0].uci!, 'green'),
    );
  };

  canJumpTo = (path: TreePath) => treePath.contains(this.root.path, path);

  onJump = () => {
    this.makeState();
    // wait for the root ctrl to make the move
    setTimeout(() => this.root.withCg(cg => cg.playPremove()), 100);
  };

  onShapeChange = (shapes: Shape[]) => {
    const node = this.root.node;
    if (node.gamebook && node.gamebook.shapes && !shapes.length) {
      node.shapes = node.gamebook.shapes.slice(0);
      this.root.jump(this.root.path);
    }
  };
}
