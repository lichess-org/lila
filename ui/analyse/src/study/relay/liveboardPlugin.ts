import { hl, type VNode } from 'lib/view';
import { getChessground, initMiniBoardWith, spinnerVdom } from 'lib/view';
import { fenColor, uciToMove } from 'lib/game/chess';
import { type ChatPlugin } from 'lib/chat/interfaces';
import type AnalyseCtrl from '@/ctrl';
import { mainlineNodeList } from 'lib/tree/ops';
import { type ChapterId } from '../interfaces';

type BoardConfig = CgConfig & { lastUci?: Uci };

export class LiveboardPlugin implements ChatPlugin {
  private animate = false;
  private board: BoardConfig | undefined;
  key = 'liveboard';
  name = i18n.broadcast.liveboard;
  kidSafe = true;
  redraw: Redraw;

  constructor(
    readonly ctrl: AnalyseCtrl,
    readonly isDisabled: () => boolean,
    private chapter: ChapterId | undefined,
  ) {}

  reset = () => {
    this.chapter = undefined;
    this.board = undefined;
    this.animate = false;
  };

  setChapterId(id: ChapterId) {
    if (id === this.chapter) return;
    this.reset();
    this.chapter = id;
  }

  view(): VNode {
    const path = this.ctrl.study?.data.chapter.relayPath;
    const tree = this.ctrl.tree;
    const localMainline = mainlineNodeList(tree.root);
    const node = localMainline[localMainline.length - 1];
    if (path) {
      const node = tree.nodeAtPath(path);
      this.board = { fen: node.fen, check: !!node.check() && fenColor(node.fen), lastUci: node.uci };
    } else if (this.chapter && !this.board) {
      const preview = this.ctrl.study?.chapters.list.get(this.chapter);
      if (!preview) return spinnerVdom();
      this.board = {
        fen: preview.fen,
        lastUci: preview.lastMove,
        check: !!preview.check && fenColor(preview.fen),
      };
    }
    this.board ??= { fen: node.fen, lastUci: node.uci, check: !!node.check() && fenColor(node.fen) };
    this.board.animation = { enabled: this.animate };
    this.board.lastMove = uciToMove(this.board.lastUci);
    this.board.orientation = this.ctrl.bottomColor();
    this.animate = true;

    return hl('div.chat-liveboard.is2d', {
      hook: {
        insert: (vn: VNode) => initMiniBoardWith(vn.elm as HTMLElement, this.board!),
        update: (_, vn: VNode) => {
          getChessground(vn.elm as HTMLElement)?.set(this.board!);
          this.animate = true;
        },
      },
    });
  }
}
