import debounce from 'common/debounce';
import * as licon from 'common/licon';
import { renderClock, fenColor } from 'common/miniBoard';
import { bind, MaybeVNodes, looseH as h } from 'common/snabbdom';
import { spinnerVdom as spinner } from 'common/spinner';
import { VNode } from 'snabbdom';
import { multiBoard as xhrLoad } from './studyXhr';
import { opposite as CgOpposite } from 'chessground/util';
import { opposite as oppositeColor } from 'chessops/util';
import { ChapterPreview, ChapterPreviewPlayer, Position, StudyChapterMeta } from './interfaces';
import StudyCtrl from './studyCtrl';
import { EvalHitMulti } from '../interfaces';
import { povChances } from 'ceval/src/winningChances';
import { Prop, defined } from 'common';
import { storedBooleanPropWithEffect } from 'common/storage';

interface CloudEval extends EvalHitMulti {
  chances: number;
}

export class MultiBoardCtrl {
  loading = false;
  page = 1;
  pager?: Paginator<ChapterPreview>;
  playing = false;
  showEval: Prop<boolean>;

  private cloudEvals: Map<Fen, CloudEval> = new Map();

  constructor(
    readonly studyId: string,
    readonly redraw: () => void,
    readonly trans: Trans,
    private readonly send: SocketSend,
    private readonly variant: () => VariantKey,
  ) {
    this.showEval = storedBooleanPropWithEffect('analyse.multiboard.showEval', true, redraw);
  }

  addNode = (pos: Position, node: Tree.Node) => {
    const cp = this.pager?.currentPageResults.find(cp => cp.id == pos.chapterId);
    if (cp?.playing) {
      cp.fen = node.fen;
      cp.lastMove = node.uci;
      const playerWhoMoved = cp.players && cp.players[oppositeColor(fenColor(cp.fen))];
      playerWhoMoved && (playerWhoMoved.clock = node.clock);
      // at this point `(cp: ChapterPreview).lastMoveAt` becomes outdated but should be ok since not in use anymore
      // to mitigate bad usage, setting it as `undefined`
      cp.lastMoveAt = undefined;
      this.requestCloudEvals();
      this.redraw();
    }
  };

  addResult = (metas: StudyChapterMeta[]) => {
    let changed = false;
    for (const meta of metas) {
      const cp = this.pager && this.pager.currentPageResults.find(cp => cp.id == meta.id);
      if (cp?.playing) {
        const oldOutcome = cp.outcome;
        cp.outcome = meta.res !== '*' ? meta.res : undefined;
        changed = changed || cp.outcome !== oldOutcome;
      }
    }
    if (changed) this.redraw();
  };

  reload = async (onInsert?: boolean) => {
    if (this.pager && !onInsert) {
      this.loading = true;
      this.redraw();
    }
    this.pager = await xhrLoad(this.studyId, this.page, this.playing);
    if (this.pager.nbPages < this.page) {
      if (!this.pager.nbPages) this.page = 1;
      else this.setPage(this.pager.nbPages);
    }
    this.loading = false;
    this.redraw();

    this.requestCloudEvals();
  };

  private requestCloudEvals = () => {
    if (this.pager?.currentPageResults.length) {
      this.send('evalGetMulti', {
        fens: this.pager?.currentPageResults.map(c => c.fen),
        ...(this.variant() != 'standard' ? { variant: this.variant() } : {}),
      });
    }
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

  onCloudEval = (d: EvalHitMulti) => {
    this.cloudEvals.set(d.fen, { ...d, chances: povChances('white', d) });
    this.redraw();
  };

  getCloudEval = (preview: ChapterPreview): CloudEval | undefined => this.cloudEvals.get(preview.fen);

  onLocalCeval = (node: Tree.Node, ev: Tree.ClientEval) => {
    const cur = this.cloudEvals.get(node.fen);
    if (!cur || cur.depth < ev.depth)
      this.cloudEvals.set(node.fen, { ...ev, chances: povChances('white', ev) });
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
    ctrl.pager ? renderPager(ctrl.pager, study) : [spinner()],
  );
}

function renderPager(pager: Paginator<ChapterPreview>, study: StudyCtrl): MaybeVNodes {
  const ctrl = study.multiBoard;
  const cloudEval = ctrl.showEval() && study.ctrl.ceval?.enabled() ? ctrl.getCloudEval : undefined;
  return [
    h('div.study__multiboard__top', [
      renderPagerNav(pager, ctrl),
      h('div.study__multiboard__options', [renderEvalToggle(ctrl), renderPlayingToggle(ctrl)]),
    ]),
    h('div.now-playing', pager.currentPageResults.map(makePreview(study, cloudEval))),
  ];
}

function renderPlayingToggle(ctrl: MultiBoardCtrl): VNode {
  return h('label.playing', [
    h('input', {
      attrs: { type: 'checkbox', checked: ctrl.playing },
      hook: bind('change', e => ctrl.setPlaying((e.target as HTMLInputElement).checked)),
    }),
    ctrl.trans.noarg('playing'),
  ]);
}

function renderEvalToggle(ctrl: MultiBoardCtrl): VNode {
  return h('label.eval', [
    h('input', {
      attrs: { type: 'checkbox', checked: ctrl.showEval() },
      hook: bind('change', e => ctrl.showEval((e.target as HTMLInputElement).checked)),
    }),
    ctrl.trans.noarg('showEvalBar'),
  ]);
}

function renderPagerNav(pager: Paginator<ChapterPreview>, ctrl: MultiBoardCtrl): VNode {
  const page = ctrl.page,
    from = Math.min(pager.nbResults, (page - 1) * pager.maxPerPage + 1),
    to = Math.min(pager.nbResults, page * pager.maxPerPage);
  return h('div.study__multiboard__pager', [
    pagerButton(ctrl.trans.noarg('first'), licon.JumpFirst, () => ctrl.setPage(1), page > 1, ctrl),
    pagerButton(ctrl.trans.noarg('previous'), licon.JumpPrev, ctrl.prevPage, page > 1, ctrl),
    h('span.page', `${from}-${to} / ${pager.nbResults}`),
    pagerButton(ctrl.trans.noarg('next'), licon.JumpNext, ctrl.nextPage, page < pager.nbPages, ctrl),
    pagerButton(ctrl.trans.noarg('last'), licon.JumpLast, ctrl.lastPage, page < pager.nbPages, ctrl),
    h('button.fbt', {
      attrs: { 'data-icon': licon.Search, title: 'Search' },
      hook: bind('click', () => lichess.pubsub.emit('study.search.open')),
    }),
  ]);
}

function pagerButton(
  text: string,
  icon: string,
  click: () => void,
  enable: boolean,
  ctrl: MultiBoardCtrl,
): VNode {
  return h('button.fbt', {
    attrs: { 'data-icon': icon, disabled: !enable, title: text },
    hook: bind('mousedown', click, ctrl.redraw),
  });
}

type GetCloudEval = (preview: ChapterPreview) => CloudEval | undefined;

const makePreview = (study: StudyCtrl, cloudEval?: GetCloudEval) => (preview: ChapterPreview) =>
  h(
    `a.mini-game.mini-game-${preview.id}.mini-game--init.is2d`,
    {
      attrs: { 'data-state': `${preview.fen},${preview.orientation},${preview.lastMove}` },
      class: {
        active: !study.multiBoard.loading && study.vm.chapterId == preview.id && !study.relay?.tourShow(),
      },
      hook: {
        insert(vnode) {
          const el = vnode.elm as HTMLElement;
          lichess.miniGame.init(el);
          vnode.data!.fen = preview.fen;
          el.addEventListener('mousedown', _ => study.setChapter(preview.id));
        },
        postpatch(old, vnode) {
          if (old.data!.fen !== preview.fen) {
            if (preview.outcome) {
              lichess.miniGame.finish(
                vnode.elm as HTMLElement,
                preview.outcome === '1-0' ? 'white' : preview.outcome === '0-1' ? 'black' : undefined,
              );
            } else {
              lichess.miniGame.update(vnode.elm as HTMLElement, {
                lm: preview.lastMove!,
                fen: preview.fen,
                wc: computeTimeLeft(preview, 'white'),
                bc: computeTimeLeft(preview, 'black'),
              });
            }
          }
          vnode.data!.fen = preview.fen;
        },
      },
    },
    [
      boardPlayer(preview, CgOpposite(preview.orientation)),
      h('span.cg-gauge', [
        h('span.mini-game__board', h('span.cg-wrap')),
        cloudEval && evalGauge(preview, cloudEval),
      ]),
      boardPlayer(preview, preview.orientation),
    ],
  );

const evalGauge = (chap: ChapterPreview, cloudEval: GetCloudEval): VNode =>
  h(
    'span.mini-game__gauge',
    h('span.mini-game__gauge__black', {
      hook: {
        postpatch(old, vnode) {
          const prevNodeCloud = old.data?.cloud;
          const cev = cloudEval(chap) || prevNodeCloud;
          if (cev?.chances != prevNodeCloud?.chances) {
            const elm = vnode.elm as HTMLElement;
            const gauge = elm.parentNode as HTMLElement;
            elm.style.height = `${((1 - (cev?.chances || 0)) / 2) * 100}%`;
            if (cev) {
              gauge.title = `Depth ${cev.depth}: ${renderScore(cev)}`;
              gauge.classList.add('mini-game__gauge--set');
            }
          }
          vnode.data!.cloud = cev;
        },
      },
    }),
  );

const renderScore = (s: EvalScore) =>
  s.mate ? '#' + s.mate : defined(s.cp) ? `${s.cp >= 0 ? '+' : ''}${s.cp / 100}` : '?';

const userName = (u: ChapterPreviewPlayer) =>
  u.title ? [h('span.utitle', u.title), ' ' + u.name] : [u.name];

function renderPlayer(player: ChapterPreviewPlayer | undefined): VNode | undefined {
  return (
    player &&
    h('span.mini-game__player', [
      h('span.mini-game__user', [
        h('span.name', userName(player)),
        player.rating && h('span.rating', ' ' + player.rating),
      ]),
    ])
  );
}

const computeTimeLeft = (preview: ChapterPreview, color: Color): number | undefined => {
  const player = preview.players && preview.players[color];
  if (player && player.clock) {
    if (preview.lastMoveAt && fenColor(preview.fen) == color) {
      const spent = (Date.now() - preview.lastMoveAt) / 1000;
      return Math.max(0, player.clock / 100 - spent);
    } else {
      return player.clock / 100;
    }
  } else {
    return;
  }
};

const boardPlayer = (preview: ChapterPreview, color: Color) => {
  const player = preview.players && preview.players[color];
  const result = preview.outcome?.split('-')[color === 'white' ? 0 : 1];
  const resultNode = result && h('span.mini-game__result', result);
  const timeleft = computeTimeLeft(preview, color);
  const clock = timeleft && renderClock(color, timeleft);
  return h('span.mini-game__player', [
    h('span.mini-game__user', [renderPlayer(player)]),
    resultNode ?? clock,
  ]);
};
