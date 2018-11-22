import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Draughtsground } from 'draughtsground';
import { opposite } from 'draughtsground/util';
import * as draughtsUtil from 'draughts';
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
    ] : [
      h('div.name', preview.name),
      makeCg(preview)
    ];
    return h('a.mini_board', {
      attrs: { title: preview.name },
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
        Draughtsground(vnode.elm as HTMLElement, {
          coordinates: 0,
          drawable: { enabled: false, visible: false },
          resizable: false,
          viewOnly: true,
          orientation: preview.orientation,
          fen: preview.fen,
          lastMove: lm ? draughtsUtil.decomposeUci(lm) : undefined
        });
      }
    }
  }, [ h('div.cg-board') ])
}
