import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Chessground } from 'chessground';
import { opposite } from 'chessground/util';
import { StudyCtrl, ChapterPreview, ChapterPreviewPlayer } from './interfaces';
import { MaybeVNodes } from '../interfaces';
import { multiBoard as xhrLoad } from './studyXhr';
import { bind, spinner } from '../util';

export class MultiBoardCtrl {

  loading: boolean = false;
  page: number = 1;
  pager?: Paginator<ChapterPreview>;
  playing: boolean = false;

  constructor(readonly studyId: string, readonly redraw: () => void) {}

  addNode(pos, node) {
    const cp = this.pager && this.pager.currentPageResults.find(cp => cp.id == pos.chapterId);
    if (cp && cp.playing) {
      cp.fen = node.fen;
      cp.lastMove = node.uci;
      this.redraw();
    }
  }

  reload(onInsert?: boolean) {
    if (this.pager && !onInsert) {
      this.loading = true;
      this.redraw();
    }
    xhrLoad(this.studyId, this.page, this.playing).then(p => {
      this.pager = p;
      if (p.nbPages < this.page) {
        if (!p.nbPages) this.page = 1;
        else this.setPage(p.nbPages);
      }
      this.loading = false;
      this.redraw();
    });
  }

  setPage = (page: number) => {
    if (this.page != page) {
      this.page = page;
      this.reload();
    }
  };
  nextPage = () => this.setPage(this.page + 1);
  prevPage = () => this.setPage(this.page - 1);
  lastPage = () => { if (this.pager) this.setPage(this.pager.nbPages); };

  setPlaying = (v: boolean) => {
    this.playing = v;
    this.reload();
  };
}

export function view(ctrl: MultiBoardCtrl, study: StudyCtrl): VNode | undefined {

  return h('div.multi_board', {
    class: { loading: ctrl.loading },
    hook: {
      insert() { ctrl.reload(true); }
    }
  }, ctrl.pager ? renderPager(ctrl.pager, study) : [spinner()]);
}

function renderPager(pager: Paginator<ChapterPreview>, study: StudyCtrl): MaybeVNodes {
  const ctrl = study.multiBoard;
  return [
    h('div.top', [
      renderPagerNav(pager, ctrl),
      renderPlayingToggle(ctrl)
    ]),
    h('div#now_playing', pager.currentPageResults.map(makePreview(study)))
  ];
}

function renderPlayingToggle(ctrl: MultiBoardCtrl): VNode {
  return h('label.playing', {
    attrs: { title: 'Only ongoing games' }
  }, [
    h('input', {
      attrs: { type: 'checkbox' },
      hook: bind('change', e => {
        ctrl.setPlaying((e.target as HTMLInputElement).checked);
      })
    }),
    'Playing'
  ]);
}

function renderPagerNav(pager: Paginator<ChapterPreview>, ctrl: MultiBoardCtrl): VNode {
  const page = ctrl.page,
  from = Math.min(pager.nbResults, (page - 1) * pager.maxPerPage + 1),
  to = Math.min(pager.nbResults, page * pager.maxPerPage);
  return h('div.pager', [
    pagerButton('First', 'W', () => ctrl.setPage(1), page > 1, ctrl),
    pagerButton('Prev', 'Y', ctrl.prevPage, page > 1, ctrl),
    h('span.page', `${from}-${to} / ${pager.nbResults}`),
    pagerButton('Next', 'X', ctrl.nextPage, page < pager.nbPages, ctrl),
    pagerButton('Last', 'V', ctrl.lastPage, page < pager.nbPages, ctrl)
  ]);
}

function pagerButton(text: string, icon: string, click: () => void, enable: boolean, ctrl: MultiBoardCtrl): VNode {
  return h('button.fbt.is', {
    attrs: {
      'data-icon': icon,
      disabled: !enable,
      title: text
    },
    hook: bind('mousedown', click, ctrl.redraw)
  });
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
    return h('.mini-board.' + preview.id, {
      attrs: { title: preview.name },
      class: { active: !study.multiBoard.loading && study.vm.chapterId == preview.id },
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

function uciToLastMove(lm?: string): Key[] | undefined {
  return lm ? ([lm[0] + lm[1], lm[2] + lm[3]] as Key[]) : undefined;
}

function makeCg(preview: ChapterPreview): VNode {
  return h('div.cg-board-wrap', {
    hook: {
      insert(vnode) {
        const cg = Chessground(vnode.elm as HTMLElement, {
          coordinates: false,
          drawable: { enabled: false, visible: false },
          resizable: false,
          viewOnly: true,
          orientation: preview.orientation,
          fen: preview.fen,
          lastMove: uciToLastMove(preview.lastMove)
        });
        vnode.data!.cp = { cg, fen: preview.fen };
      },
      postpatch(old, vnode) {
        if (old.data!.cp.fen !== preview.fen) {
          old.data!.cp.cg.set({
            fen: preview.fen,
            lastMove: uciToLastMove(preview.lastMove)
          });
          old.data!.cp.fen = preview.fen;
        }
        vnode.data!.cp = old.data!.cp;
      }
    }
  }, [h('div.cg-board')])
}
