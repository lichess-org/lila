import type { Move } from 'chessops';
import { makeUci } from 'chessops/util';

import { prop, type Prop } from 'lib';
import type { TreeNode } from 'lib/tree/types';

import type AnalyseCtrl from '@/ctrl';

const MAINLINE_WEIGHT = 2;
const REPLY_DELAY = 350;
const REPLY_DELAY_WITH_SHAPES = 3000;

type Feedback = 'right' | 'wrong';

export default class RecallCtrl {
  feedback: Prop<Feedback | null> = prop(null);
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
    this.maybePlayReply();
  };

  onJump = () => {
    this.clearReplyTimeout();
    this.maybePlayReply();
  };

  movableColor = (): Color | undefined => (this.isPlayerTurn() ? this.root.bottomColor() : undefined);

  isPlayerTurn = (): boolean => this.root.turnColor() === this.root.bottomColor();

  currentNode = (): TreeNode => this.root.node;

  normalizeUci = (uci: string): string => {
    if (!uci.includes('@') && uci.length === 4 && this.root.variantKey !== 'chess960') {
      const orig = uci.slice(0, 2) as Key;
      const dest = uci.slice(2, 4) as Key;
      if (
        this.root.chessground.state.pieces.get(dest)?.role === 'king' &&
        orig[0] === 'e' &&
        ['c', 'g'].includes(dest[0])
      ) {
        uci = orig + (dest[0] === 'g' ? 'h' : 'a') + dest[1];
      }
    }
    return uci;
  };

  onMove = (move: Move): void => {
    if (!this.isPlayerTurn()) {
      this.onWrongMove();
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

  onWrongMove = () => {
    this.clearReplyTimeout();
    this.feedback('wrong');
    site.sound.play('practiceFailure');
    this.redraw();
  };

  private onRightMove = () => {
    this.feedback('right');
    site.sound.play('practiceSuccess');
  };

  private maybePlayReply = () => {
    this.clearReplyTimeout();
    if (this.isPlayerTurn()) return;

    const node = this.currentNode();
    const weighted = Array(MAINLINE_WEIGHT).fill(node.children[0]).concat(node.children.slice(1));
    const reply = weighted[Math.floor(Math.random() * weighted.length)];
    if (!reply) return;

    const delay = node.shapes?.length ? REPLY_DELAY_WITH_SHAPES : REPLY_DELAY;

    this.replyTimeout = window.setTimeout(() => {
      this.replyTimeout = undefined;
      this.feedback(null);
      this.root.jump(this.root.path + reply.id);
      this.redraw();
    }, delay);
  };

  private clearReplyTimeout = () => {
    if (this.replyTimeout !== undefined) {
      window.clearTimeout(this.replyTimeout);
      this.replyTimeout = undefined;
    }
  };
}
