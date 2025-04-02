import { type RelayViewContext } from '../../view/components';
import { looseH as h, VNode, onInsert } from 'lib/snabbdom';
import { getChessground, initMiniBoardWith, fenColor } from 'lib/miniBoard';
import { type ChatPlugin, makeChat } from 'lib/chat/chat';
import { watchers } from 'lib/watchers';
import { frag } from 'lib';
import * as co from 'chessops';
import { normalMove } from 'lib/chess/chess';
import { pathToUcis } from 'lib/chess/uciCharPair';
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
    readonly localMainline: () => Tree.Node[],
    readonly relayPath: () => Tree.Path | undefined,
  ) {}

  set chapterId(id: ChapterId | undefined) {
    if (id === this.chapter) return;
    this.chapter = id;
    this.animate = false;
    this.board = undefined;
  }

  get hidden(): boolean {
    return this.isDisabled();
  }

  view(): VNode {
    const [mainline, path] = [this.localMainline(), this.relayPath()];
    if (path) {
      this.board = this.fromUcis(mainline[0], pathToUcis(path));
    } else if (this.chapter && !this.board) {
      const preview = this.previews().get(this.chapter);
      if (!preview) return spinnerVdom();
      this.board = {
        fen: preview.fen,
        lastUci: preview.lastMove,
        check: !!preview.check && fenColor(preview.fen),
      };
    }
    this.board ??= this.fromUcis(
      mainline[0],
      mainline.slice(1).map(x => x.uci!),
    );
    this.board.animation = { enabled: this.animate };
    this.animate = true;

    return h('div.chat-liveboard', {
      key: this.board.fen,
      hook: {
        insert: (vn: VNode) => initMiniBoardWith(vn.elm as HTMLElement, this.board!),
        update: (_, vn: VNode) => {
          getChessground(vn.elm as HTMLElement)?.set(this.board!);
          this.animate = true;
        },
      },
    });
  }

  private fromUcis(root: Tree.Node, ucis: Uci[]): BoardConfig {
    const chess = co.Chess.fromSetup(co.fen.parseFen(root.fen).unwrap()).unwrap();
    ucis.forEach(uci => chess.play(normalMove(chess, uci)!.move));
    return {
      fen: co.fen.makeFen(chess.toSetup()),
      lastUci: ucis.length > 0 ? ucis[ucis.length - 1] : undefined,
      check: chess.isCheck() && chess.turn,
    };
  }
}
