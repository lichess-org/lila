import type { Move } from 'chessops';
import { makeUci } from 'chessops/util';

import { prop, toggle, type Prop, type Toggle } from 'lib';
import type { TreeNode } from 'lib/tree/types';

import type AnalyseCtrl from '@/ctrl';

const MAINLINE_WEIGHT = 2;
const REPLY_DELAY = 350;
const REPLY_DELAY_WITH_SHAPES = 3000;

type Feedback = 'right' | 'wrong';

export type State = Feedback | 'empty' | 'end' | 'play' | 'wait';

export default class RecallCtrl {
  feedback: Prop<Feedback | null> = prop(null);
  showMoves: Toggle = toggle(false);
  private replyTimeout?: number;

  constructor(
    readonly root: AnalyseCtrl,
    readonly redraw: Redraw,
  ) {
    site.sound.load('practiceSuccess', site.sound.url('other/energy3.mp3'));
    site.sound.load('practiceFailure', site.sound.url('other/failure2.mp3'));
  }

  onLoad = () => {
    this.clearReplyTimeout();
    this.feedback(null);
    this.showMoves(false);
    this.maybePlayReply();
  };

  onJump = () => {
    this.clearReplyTimeout();
    this.maybePlayReply();
  };

  onFlip = () => {
    this.clearReplyTimeout();
    this.feedback(null);
    this.maybePlayReply();
  };

  destroy = () => this.clearReplyTimeout();

  state = (): State =>
    !this.root.tree.root.children.length
      ? 'empty'
      : this.isEndOfLine()
        ? 'end'
        : (this.feedback() ?? (this.isPlayerTurn() ? 'play' : 'wait'));

  isEndOfLine = (): boolean => !this.currentNode().children.length;

  hideMoves = (): boolean => !this.showMoves();

  restart = () => {
    this.feedback(null);
    this.root.userJump('');
  };

  movableColor = (): Color | undefined => (this.canPlay() ? this.root.bottomColor() : undefined);

  canPlay = (): boolean => this.isPlayerTurn() && !this.isEndOfLine();

  isPlayerTurn = (): boolean => this.root.turnColor() === this.root.bottomColor();

  currentNode = (): TreeNode => this.root.node;

  normalizeUci = (uci: string): string => {
    if (!uci.includes('@') && uci.length === 4 && this.root.variantKey !== 'chess960') {
      const orig = uci.slice(0, 2) as Key;
      const dest = uci.slice(2, 4) as Key;
      if (
        this.root.chessground.state.pieces.get(dest)?.role === 'king' &&
        orig.startsWith('e') &&
        ['c', 'g'].includes(dest[0])
      ) {
        uci = orig + (dest.startsWith('g') ? 'h' : 'a') + dest[1];
      }
    }
    return uci;
  };

  onMove = (move: Move): void => {
    if (!this.canPlay()) {
      this.root.jump(this.root.path);
      return;
    }
    const expected = this.currentNode().children[0];
    if (this.normalizeUci(makeUci(move)) !== expected?.uci) {
      this.onWrongMove();
      this.root.jump(this.root.path);
      return;
    }
    this.onRightMove();
    this.root.jump(this.root.path + expected.id);
    this.redraw();
  };

  private readonly onWrongMove = () => {
    this.clearReplyTimeout();
    this.feedback('wrong');
    site.sound.play('practiceFailure');
    this.redraw();
  };

  private readonly onRightMove = () => {
    this.feedback('right');
    site.sound.play('practiceSuccess');
  };

  private readonly maybePlayReply = () => {
    this.clearReplyTimeout();
    if (this.isPlayerTurn() || this.isEndOfLine()) return;
    const node = this.currentNode();
    const weighted = Array(MAINLINE_WEIGHT).fill(node.children[0]).concat(node.children.slice(1));
    const reply = weighted[Math.floor(Math.random() * weighted.length)];
    const delay = node.shapes?.length ? REPLY_DELAY_WITH_SHAPES : REPLY_DELAY;
    this.replyTimeout = window.setTimeout(() => {
      this.replyTimeout = undefined;
      this.feedback(null);
      this.root.jump(this.root.path + reply.id);
      this.redraw();
    }, delay);
  };

  private readonly clearReplyTimeout = () => {
    if (this.replyTimeout !== undefined) {
      window.clearTimeout(this.replyTimeout);
      this.replyTimeout = undefined;
    }
  };
}
