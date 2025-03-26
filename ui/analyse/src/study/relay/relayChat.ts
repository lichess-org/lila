import { type RelayViewContext } from '../../view/components';
import type { StudyChapters } from '../studyChapters';
import { spinnerVdom } from 'lib/controls';
import { looseH as h, VNode, onInsert } from 'lib/snabbdom';
import { getChessground, initMiniBoardWith } from 'lib/miniBoard';
import { type ChatPlugin, makeChat } from 'lib/chat/chat';
import { watchers } from 'lib/watchers';
import { uciToMove } from 'chessground/util';
import { frag } from 'lib';
import { ChapterId } from '../interfaces';

export function relayChatView({ ctrl, relay }: RelayViewContext): VNode | undefined {
  if (ctrl.isEmbed || !ctrl.opts.chat) return undefined;
  return h('section.mchat.mchat-optional', {
    hook: onInsert(el => {
      ctrl.opts.chat.instance?.destroy();
      ctrl.opts.chat.instance = makeChat({
        ...ctrl.opts.chat,
        plugin: relay.chatCtrl,
        enhance: { plies: true, boards: true },
      });
      const members = frag<HTMLElement>('<div class="chat__members">');
      el.parentElement?.append(members);
      watchers(members);
    }),
  });
}

export class RelayChatPlugin implements ChatPlugin {
  private chapter: ChapterId | undefined;
  private animate = false;

  key = 'liveboard';
  name = i18n.broadcast.liveboard;
  kidSafe = true;
  redraw: Redraw;

  constructor(
    readonly chapters: StudyChapters,
    readonly isDisabled: () => boolean,
  ) {}

  set chapterId(id: ChapterId) {
    if (id === this.chapter) return;
    this.chapter = id;
    this.animate = false;
  }

  get hidden(): boolean {
    return this.isDisabled() || !this.chapter;
  }

  view(): VNode {
    const preview = this.chapters.get(this.chapter || 0);
    return preview
      ? h('div.chat-liveboard', {
          hook: {
            insert: (vn: VNode) =>
              initMiniBoardWith(vn.elm as HTMLElement, preview.fen, 'white', preview.lastMove),
            update: (_, vn: VNode) => {
              getChessground(vn.elm as HTMLElement)?.set({
                fen: preview.fen,
                lastMove: uciToMove(preview.lastMove),
                animation: { enabled: this.animate },
              });
              this.animate = true;
            },
          },
        })
      : spinnerVdom();
  }
}
