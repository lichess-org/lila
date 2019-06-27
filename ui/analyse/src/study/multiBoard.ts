import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Draughtsground } from 'draughtsground';
import { opposite } from 'draughtsground/util';
import { resultOf } from './studyChapters';
import * as draughtsUtil from 'draughts';
import { StudyCtrl, ChapterPreview, ChapterPreviewPlayer } from './interfaces';
import { MaybeVNodes } from '../interfaces';
import { multiBoard as xhrLoad } from './studyXhr';
import { bind, spinner } from '../util';

export class MultiBoardCtrl {

  loading: boolean = false;
  page: number = 1;
  pager?: Paginator<ChapterPreview>;
  playing: boolean = false;

  constructor(readonly studyId: string, readonly redraw: () => void, readonly trans: Trans) {}

  addNode(pos, node) {
    const cp = this.pager && this.pager.currentPageResults.find(cp => cp.id == pos.chapterId);
    if (cp && (!cp.result || cp.result === "*")) {
      cp.fen = node.fen;
      cp.lastMove = node.uci;
      this.redraw();
    }
  }

  setResult(chapterId, result?: string) {
    if (!result) return;
    const cp = this.pager && this.pager.currentPageResults.find(cp => cp.id == chapterId);
    if (cp && cp.result !== result) {
      cp.result = result;
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

  return h('div.study__multiboard', {
    class: { loading: ctrl.loading, nopager: !ctrl.pager },
    hook: {
      insert() { ctrl.reload(true) }
    }
  }, ctrl.pager ? renderPager(ctrl.pager, study) : [spinner()]);
}

function renderPager(pager: Paginator<ChapterPreview>, study: StudyCtrl): MaybeVNodes {
  const ctrl = study.multiBoard;
  return [
    h('div.top', [
      renderPagerNav(pager, ctrl),
      study.relay ? renderPlayingToggle(ctrl) : null
    ]),
    h('div.now-playing', pager.currentPageResults.map(makePreview(study)))
  ];
}

function renderPlayingToggle(ctrl: MultiBoardCtrl): VNode {
  return h('label.playing', {
    attrs: { title: ctrl.trans.noarg('onlyOngoingGames') }
  }, [
    h('input', {
      attrs: { 
        type: 'checkbox',
        checked: ctrl.playing
      },
      hook: bind('change', e => {
        ctrl.setPlaying((e.target as HTMLInputElement).checked);
      })
    }),
    ctrl.trans.noarg('playing')
  ]);
}

function renderPagerNav(pager: Paginator<ChapterPreview>, ctrl: MultiBoardCtrl): VNode {
  const page = ctrl.page,
  from = Math.min(pager.nbResults, (page - 1) * pager.maxPerPage + 1),
  to = Math.min(pager.nbResults, page * pager.maxPerPage);
  return h('div.pager', [
    pagerButton(ctrl.trans.noarg('first'), 'W', () => ctrl.setPage(1), page > 1, ctrl),
    pagerButton(ctrl.trans.noarg('previous'), 'Y', ctrl.prevPage, page > 1, ctrl),
    h('span.page', `${from}-${to} / ${pager.nbResults}`),
    pagerButton(ctrl.trans.noarg('next'), 'X', ctrl.nextPage, page < pager.nbPages, ctrl),
    pagerButton(ctrl.trans.noarg('last'), 'V', ctrl.lastPage, page < pager.nbPages, ctrl)
  ]);
}

function pagerButton(text: string, icon: string, click: () => void, enable: boolean, ctrl: MultiBoardCtrl): VNode {
  return h('button.fbt', {
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
      preview.result ? h('span.player-result', [resultOf([['result', preview.result]], opposite(preview.orientation) == 'white', true)]) : undefined,
      makePlayer(preview.players[opposite(preview.orientation)]),
      makeCg(preview),
      preview.result ? h('span.player-result', [resultOf([['result', preview.result]], preview.orientation == 'white', true)]) : undefined,
      makePlayer(preview.players[preview.orientation])
    ] : [
      h('div.name', preview.name),
      makeCg(preview)
    ];
    return h('a.' + preview.id, {
      attrs: { title: preview.name },
      class: { active: !study.multiBoard.loading && study.vm.chapterId == preview.id && (!study.relay || !study.relay.intro.active) },
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
  return lm ? draughtsUtil.decomposeUci(lm) : undefined;
}

function makeCg(preview: ChapterPreview): VNode {
  return h('div.mini-board.cg-wrap', {
    hook: {
      insert(vnode) {
        const cg = Draughtsground(vnode.elm as HTMLElement, {
          coordinates: 0,
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
  })
}

export class MultiBoardMenuCtrl {
  open: boolean = false;
  toggle = () => this.open = !this.open;
  view = (study?: StudyCtrl) => study && h('div.action-menu.multiboard-menu', [view(study.multiBoard, study)]);
}
