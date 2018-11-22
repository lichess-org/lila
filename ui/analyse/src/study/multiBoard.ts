import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Chessground } from 'chessground';
import { opposite } from 'chessground/util';
import { StudyCtrl, ChapterPreview, ChapterPreviewPlayer } from './interfaces';
import { multiBoard as xhrLoad } from './studyXhr';
import { bind, spinner } from '../util';

interface MultiBoardData {
  previews: [ChapterPreview]
}

export class MultiBoardCtrl {

  data?: MultiBoardData;

  constructor(readonly studyId: string, readonly redraw: () => void) {
  }

  reload() {
    xhrLoad(this.studyId).then(d => {
      this.data = d;
      this.redraw();
    });
  }
}

export function view(ctrl: MultiBoardCtrl, study: StudyCtrl): VNode | undefined {

  return h('div#now_playing', {
    hook: {
      insert() { ctrl.reload(); }
    }
  }, ctrl.data ? ctrl.data.previews.map(makePreview(study)) : [spinner()]);
}

function makePreview(study: StudyCtrl) {
  return (preview: ChapterPreview) => {
    const contents = preview.players ? [
      makePlayer(preview.players[opposite(preview.orientation)]),
      makeCg(preview),
      makePlayer(preview.players[preview.orientation])
    ] : [makeCg(preview)];
    return h('a.mini_board', {
      class: { active: study.vm.chapterId == preview.id },
      hook: bind('mousedown', _ => study.setChapter(preview.id))
    }, contents);
  };
}

function makePlayer(player: ChapterPreviewPlayer): VNode {
  return h('div.player', [
    player.title ? `${player.title} ${player.name}` : player.name,
    player.rating && h('span', '' + player.rating)
  ]);
}

function makeCg(preview: ChapterPreview): VNode {
  return h('div.cg-board-wrap', {
    hook: {
      insert(vnode) {
        const lm = preview.lastMove;
        Chessground(vnode.elm as HTMLElement, {
          coordinates: false,
          drawable: { enabled: false, visible: false },
          resizable: false,
          viewOnly: true,
          orientation: preview.orientation,
          fen: preview.fen,
          lastMove: lm ? ([lm[0] + lm[1], lm[2] + lm[3]] as Key[]) : undefined
        });
      }
    }
  }, [ h('div.cg-board') ])
}
