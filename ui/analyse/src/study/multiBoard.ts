import * as domData from 'common/data';
import debounce from 'common/debounce';
import { bind } from 'common/snabbdom';
import { spinner } from '../util';
import { h, VNode } from 'snabbdom';
import { MaybeVNodes } from '../interfaces';
import { multiBoard as xhrLoad } from './studyXhr';
import { opposite } from 'chessground/util';
import { StudyCtrl, ChapterPreview, ChapterPreviewPlayer, Position } from './interfaces';

export class MultiBoardCtrl {
  loading = false;
  page = 1;
  pager?: Paginator<ChapterPreview>;
  playing = false;

  constructor(readonly studyId: string, readonly redraw: () => void, readonly trans: Trans) {}

  addNode = (pos: Position, node: Tree.Node) => {
    const cp = this.pager && this.pager.currentPageResults.find(cp => cp.id == pos.chapterId);
    if (cp && cp.playing) {
      cp.fen = node.fen;
      cp.lastMove = node.uci;
      this.redraw();
    }
  };

  reload = (onInsert?: boolean) => {
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
  };

  reloadEventually = debounce(this.reload, 1000);

  setPage = (page: number) => {
    if (this.page != page) {
      this.page = page;
      this.reload();
    }
  };
  nextPage = () => this.setPage(this.page + 1);
  prevPage = () => this.setPage(this.page - 1);
  lastPage = () => {
    if (this.pager) this.setPage(this.pager.nbPages);
  };

  setPlaying = (v: boolean) => {
    this.playing = v;
    this.reload();
  };
}

export function view(ctrl: MultiBoardCtrl, study: StudyCtrl): VNode | undefined {
  const chapterIds = study.chapters
    .list()
    .map(c => c.id)
    .join('');
  return h(
    'div.study__multiboard',
    {
      class: { loading: ctrl.loading, nopager: !ctrl.pager },
      hook: {
        insert(vnode: VNode) {
          ctrl.reload(true);
          vnode.data!.chapterIds = chapterIds;
        },
        postpatch(old: VNode, vnode: VNode) {
          if (old.data!.chapterIds !== chapterIds) ctrl.reloadEventually();
          vnode.data!.chapterIds = chapterIds;
        },
      },
    },
    ctrl.pager ? renderPager(ctrl.pager, study) : [spinner()]
  );
}

function renderPager(pager: Paginator<ChapterPreview>, study: StudyCtrl): MaybeVNodes {
  const ctrl = study.multiBoard;
  return [
    h('div.top', [renderPagerNav(pager, ctrl), renderPlayingToggle(ctrl)]),
    h('div.now-playing', pager.currentPageResults.map(makePreview(study))),
  ];
}

function renderPlayingToggle(ctrl: MultiBoardCtrl): VNode {
  return h('label.playing', [
    h('input', {
      attrs: { type: 'checkbox' },
      hook: bind('change', e => {
        ctrl.setPlaying((e.target as HTMLInputElement).checked);
      }),
    }),
    ctrl.trans.noarg('playing'),
  ]);
}

function renderPagerNav(pager: Paginator<ChapterPreview>, ctrl: MultiBoardCtrl): VNode {
  const page = ctrl.page,
    from = Math.min(pager.nbResults, (page - 1) * pager.maxPerPage + 1),
    to = Math.min(pager.nbResults, page * pager.maxPerPage);
  return h('div.pager', [
    pagerButton(ctrl.trans.noarg('first'), '', () => ctrl.setPage(1), page > 1, ctrl),
    pagerButton(ctrl.trans.noarg('previous'), '', ctrl.prevPage, page > 1, ctrl),
    h('span.page', `${from}-${to} / ${pager.nbResults}`),
    pagerButton(ctrl.trans.noarg('next'), '', ctrl.nextPage, page < pager.nbPages, ctrl),
    pagerButton(ctrl.trans.noarg('last'), '', ctrl.lastPage, page < pager.nbPages, ctrl),
  ]);
}

function pagerButton(text: string, icon: string, click: () => void, enable: boolean, ctrl: MultiBoardCtrl): VNode {
  return h('button.fbt', {
    attrs: {
      'data-icon': icon,
      disabled: !enable,
      title: text,
    },
    hook: bind('mousedown', click, ctrl.redraw),
  });
}

function makePreview(study: StudyCtrl) {
  return (preview: ChapterPreview) => {
    const contents = preview.players
      ? [
          makePlayer(preview.players[opposite(preview.orientation)]),
          makeCg(preview),
          makePlayer(preview.players[preview.orientation]),
        ]
      : [h('div.name', preview.name), makeCg(preview)];
    return h(
      'a.' + preview.id,
      {
        attrs: { title: preview.name },
        class: {
          active: !study.multiBoard.loading && study.vm.chapterId == preview.id && !study.relay?.tourShow.active,
        },
        hook: bind('mousedown', _ => study.setChapter(preview.id)),
      },
      contents
    );
  };
}

function makePlayer(player: ChapterPreviewPlayer): VNode {
  return h('span.player', [
    player.title ? `${player.title} ${player.name}` : player.name,
    player.rating && h('span', '' + player.rating),
  ]);
}

function makeCg(preview: ChapterPreview): VNode {
  return h('span.mini-board.cg-wrap.is2d', {
    attrs: {
      'data-state': `${preview.fen},${preview.orientation},${preview.lastMove}`,
    },
    hook: {
      insert(vnode) {
        lichess.miniBoard.init(vnode.elm as HTMLElement);
        vnode.data!.fen = preview.fen;
      },
      postpatch(old, vnode) {
        if (old.data!.fen !== preview.fen) {
          const lm = preview.lastMove!;
          domData.get(vnode.elm as HTMLElement, 'chessground').set({
            fen: preview.fen,
            lastMove: [lm[0] + lm[1], lm[2] + lm[3]],
          });
        }
        vnode.data!.fen = preview.fen;
      },
    },
  });
}
