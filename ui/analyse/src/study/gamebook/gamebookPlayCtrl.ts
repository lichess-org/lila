import AnalyseCtrl from '../../ctrl';
import { StudyCtrl } from '../interfaces';
import { readOnlyProp } from '../../util';
import { path as treePath, ops as treeOps } from 'tree';
import Mascot from './mascot';

type Feedback = 'play' | 'good' | 'bad' | 'end';

export interface State {
  feedback: Feedback;
  comment?: string;
  hint?: string;
  showHint: boolean;
}

export default class GamebookPlayCtrl {

  mascot = new Mascot();
  state: State;

  constructor(readonly root: AnalyseCtrl, readonly chapterId: string, readonly redraw: () => void) {

    this.makeState();

    // ensure all original nodes have a gamebook entry,
    // so we can differentiate original nodes from user-made ones
    treeOps.updateAll(root.tree.root, n => {
      n.gamebook = n.gamebook || {};
      if (n.shapes) n.gamebook.shapes = n.shapes.slice(0);
    });
  }

  private makeState = (): void => {
    const node = this.root.node,
    nodeComment = (node.comments || [])[0],
    state: Partial<State> = {
      comment: nodeComment ? nodeComment.text : undefined
    },
    parPath = treePath.init(this.root.path),
    parNode = this.root.tree.nodeAtPath(parPath);
    if (!this.root.onMainline && !this.root.tree.pathIsMainline(parPath)) return;
    if (this.root.onMainline && !node.children[0]) {
      state.feedback = 'end';
    }
    else if (this.isMyMove()) {
      state.feedback = 'play';
      state.hint = (node.gamebook || {}).hint;
    } else if (this.root.onMainline) {
      state.feedback = 'good';
    } else {
      state.feedback = 'bad';
      if (!state.comment) {
        state.comment = parNode.children[0].gamebook!.deviation;
      }
    }
    this.state = state as State;
    if (state.feedback === 'good' && !state.comment) setTimeout(() => {
      this.next();
      this.redraw();
    }, this.root.path ? 1000 : 0);
  }

  isMyMove = () => this.root.turnColor() === this.root.data.orientation;

  retry = () => {
    let path = this.root.path;
    while (path && !this.root.tree.pathIsMainline(path)) path = treePath.init(path);
    this.root.userJump(path);
  }

  next = (force?: boolean) => {
    if (!force && this.isMyMove()) return;
    const child = this.root.node.children[0];
    if (child) this.root.userJump(this.root.path + child.id);
    this.redraw();
  }

  hint = () => {
    if (this.state.hint) this.state.showHint = !this.state.showHint;
  }

  solution = () => this.next(true);

  canJumpTo = (path: Tree.Path) => treePath.contains(this.root.path, path);

  onJump = this.makeState;

  onShapeChange = shapes => {
    const node = this.root.node;
    if (node.gamebook && node.gamebook.shapes && !shapes.length) {
      node.shapes = node.gamebook.shapes.slice(0);
      this.root.jump(this.root.path);
    }
  };
}
