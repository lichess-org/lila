import { type RelayViewContext } from '../../view/components';
import { looseH as h, VNode, onInsert } from 'lib/snabbdom';
import { getChessground, initMiniBoardWith, fenColor, uciToMove } from 'lib/miniBoard';
import { type ChatPlugin, makeChat } from 'lib/chat/chat';
import { type TreeWrapper } from 'lib/tree/tree';
import { mainlineNodeList } from 'lib/tree/ops';
import { watchers } from 'lib/watchers';
import { frag } from 'lib';
import { type ChapterId } from '../interfaces';
import { type StudyChapters } from '../studyChapters';
import { spinnerVdom } from 'lib/controls';

type BoardConfig = CgConfig & { lastUci?: Uci };

export function relayChatView({ ctrl, relay }: RelayViewContext): VNode | undefined {
  if (ctrl.isEmbed || !ctrl.opts.chat) return undefined;
  return h('section.mchat.mchat-optional', {
    hook: onInsert(() => {
      ctrl.opts.chat.instance = makeChat({
        ...ctrl.opts.chat,
        plugin: relay.chatCtrl,
        persistent: true,
        enhance: { plies: true, boards: true },
      });
      const members = frag<HTMLElement>('<div class="chat__members">');
      document.querySelector('.relay-tour__side')?.append(members);
      watchers(members, false);
    }),
  });
}

export class RelayChatPlugin implements ChatPlugin {
  private animate = false;
  private board: BoardConfig | undefined;
  private chapter: ChapterId | undefined;
  key = 'liveboard';
  name = i18n.broadcast.liveboard;
  kidSafe = true;
  redraw: Redraw;
  isDisabled = () => true;

  constructor(
    readonly previews: () => StudyChapters,
    readonly localTree: () => TreeWrapper,
    readonly relayPath: () => Tree.Path | undefined,
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

  get hidden(): boolean {
    return this.isDisabled();
  }

  view(): VNode {
    const path = this.relayPath();
    const tree = this.localTree();
    const localMainline = mainlineNodeList(tree.root);
    const node = localMainline[localMainline.length - 1];
    if (path) {
      const node = tree.nodeAtPath(path);
      this.board = { fen: node.fen, check: !!node.check && fenColor(node.fen), lastUci: node.uci };
    } else if (this.chapter && !this.board) {
      const preview = this.previews().get(this.chapter);
      if (!preview) return spinnerVdom();
      this.board = {
        fen: preview.fen,
        lastUci: preview.lastMove,
        check: !!preview.check && fenColor(preview.fen),
      };
    }
    this.board ??= { fen: node.fen, lastUci: node.uci, check: !!node.check && fenColor(node.fen) };
    this.board.animation = { enabled: this.animate };
    this.board.lastMove = uciToMove(this.board.lastUci);
    this.animate = true;

    return h('div.chat-liveboard', {
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
