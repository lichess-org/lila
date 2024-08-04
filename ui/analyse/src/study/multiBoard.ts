import * as licon from 'common/licon';
import { otbClockIsRunning, formatMs } from 'common/clock';
import { fenColor } from 'common/miniBoard';
import { MaybeVNode, VNode, bind, onInsert } from 'common/snabbdom';
import { opposite as CgOpposite, uciToMove } from 'chessground/util';
import { ChapterId, ChapterPreview, ChapterPreviewPlayer } from './interfaces';
import StudyCtrl from './studyCtrl';
import { CloudEval, MultiCloudEval, renderEvalToggle, renderScoreAtDepth } from './multiCloudEval';
import { Toggle, defined, notNull, toggle } from 'common';
import { Color } from 'chessops';
import { StudyChapters, gameLinkAttrs, gameLinksListener } from './studyChapters';
import { playerFed } from './playerBars';
import { userTitle } from 'common/userLink';
import { h } from 'snabbdom';

export class MultiBoardCtrl {
  playing: Toggle;
  page: number = 1;
  maxPerPage: number = 12;

  constructor(
    readonly chapters: StudyChapters,
    readonly multiCloudEval: MultiCloudEval,
    readonly redraw: () => void,
    readonly trans: Trans,
  ) {
    this.playing = toggle(false, this.redraw);
  }

  private chapterFilter = (c: ChapterPreview) => !this.playing() || c.playing;

  pager = (): Paginator<ChapterPreview> => {
    const filteredResults = this.chapters.all().filter(this.chapterFilter);
    const currentPageResults = filteredResults.slice(
      (this.page - 1) * this.maxPerPage,
      this.page * this.maxPerPage,
    );
    const nbResults = filteredResults.length;
    const nbPages = Math.floor((nbResults + this.maxPerPage - 1) / this.maxPerPage);
    return {
      currentPage: this.page,
      maxPerPage: this.maxPerPage,
      currentPageResults,
      nbResults,
      previousPage: this.page > 1 ? this.page - 1 : undefined,
      nextPage: this.page < nbPages && currentPageResults.length ? this.page + 1 : undefined,
      nbPages,
    };
  };
  setPage = (page: number) => {
    if (this.page != page) {
      this.page = page;
      this.redraw();
    }
  };
  nextPage = () => this.setPage(this.page + 1);
  prevPage = () => this.setPage(this.page - 1);
  lastPage = () => this.setPage(this.pager().nbPages);
}

export function view(ctrl: MultiBoardCtrl, study: StudyCtrl): MaybeVNode {
  const pager = ctrl.pager();
  const cloudEval = ctrl.multiCloudEval.thisIfShowEval();
  const baseUrl = study.embeddablePath(study.relay?.roundPath() || study.baseUrl());
  return h('div.study__multiboard', [
    h('div.study__multiboard__top', [
      renderPagerNav(pager, ctrl),
      h('div.study__multiboard__options', [
        h('label.eval', [renderEvalToggle(ctrl.multiCloudEval), ctrl.trans.noarg('showEvalBar')]),
        renderPlayingToggle(ctrl),
      ]),
    ]),
    h(
      'div.now-playing',
      {
        hook: {
          insert: gameLinksListener(study.chapterSelect),
        },
      },
      pager.currentPageResults.map(makePreview(baseUrl, study.vm.chapterId, cloudEval)),
    ),
  ]);
}

function renderPagerNav(pager: Paginator<ChapterPreview>, ctrl: MultiBoardCtrl): VNode {
  const page = ctrl.page,
    from = Math.min(pager.nbResults, (page - 1) * pager.maxPerPage + 1),
    to = Math.min(pager.nbResults, page * pager.maxPerPage);
  return h('div.study__multiboard__pager', [
    pagerButton('first', licon.JumpFirst, () => ctrl.setPage(1), page > 1, ctrl),
    pagerButton('previous', licon.JumpPrev, ctrl.prevPage, page > 1, ctrl),
    h('span.page', `${from}-${to} / ${pager.nbResults}`),
    pagerButton('next', licon.JumpNext, ctrl.nextPage, page < pager.nbPages, ctrl),
    pagerButton('last', licon.JumpLast, ctrl.lastPage, page < pager.nbPages, ctrl),
  ]);
}

function pagerButton(
  transKey: string,
  icon: string,
  click: () => void,
  enable: boolean,
  ctrl: MultiBoardCtrl,
): VNode {
  return h('button.fbt', {
    attrs: { 'data-icon': icon, disabled: !enable, title: ctrl.trans.noarg(transKey) },
    hook: bind('mousedown', click, ctrl.redraw),
  });
}

const renderPlayingToggle = (ctrl: MultiBoardCtrl): MaybeVNode =>
  h('label.playing', [
    h('input', {
      attrs: { type: 'checkbox', checked: ctrl.playing() },
      hook: bind('change', e => ctrl.playing((e.target as HTMLInputElement).checked)),
    }),
    ctrl.trans.noarg('playing'),
  ]);

const previewToCgConfig = (cp: ChapterPreview): CgConfig => ({
  fen: cp.fen,
  lastMove: uciToMove(cp.lastMove),
  turnColor: fenColor(cp.fen),
  check: !!cp.check,
});

const makePreview =
  (roundPath: string, current: ChapterId, cloudEval?: MultiCloudEval) => (preview: ChapterPreview) => {
    const orientation = preview.orientation || 'white';
    return h(
      `a.mini-game.is2d.chap-${preview.id}`,
      {
        class: { active: preview.id === current },
        attrs: gameLinkAttrs(roundPath, preview),
      },
      [
        boardPlayer(preview, CgOpposite(orientation)),
        h('span.cg-gauge', [
          cloudEval && verticalEvalGauge(preview, cloudEval),
          h(
            'span.mini-game__board',
            h('span.cg-wrap', {
              hook: {
                insert(vnode) {
                  const el = vnode.elm as HTMLElement;
                  vnode.data!.cg = site.makeChessground(el, {
                    ...previewToCgConfig(preview),
                    coordinates: false,
                    viewOnly: true,
                    orientation,
                    drawable: {
                      enabled: false,
                      visible: false,
                    },
                  });
                  vnode.data!.fen = preview.fen;
                },
                postpatch(old, vnode) {
                  if (old.data!.fen !== preview.fen) {
                    old.data!.cg?.set(previewToCgConfig(preview));
                  }
                  vnode.data!.fen = preview.fen;
                  vnode.data!.cg = old.data!.cg;
                },
              },
            }),
          ),
        ]),
        boardPlayer(preview, orientation),
      ],
    );
  };

export const verticalEvalGauge = (chap: ChapterPreview, cloudEval: MultiCloudEval): MaybeVNode => {
  const tag = `span.mini-game__gauge${chap.orientation == 'black' ? ' mini-game__gauge--flip' : ''}${
    chap.check == '#' ? ' mini-game__gauge--set' : ''
  }`;
  return chap.check == '#'
    ? h(tag, { attrs: { 'data-id': chap.id, title: 'Checkmate' } }, [
        h('span.mini-game__gauge__black', {
          attrs: { style: `height: ${fenColor(chap.fen) == 'white' ? 100 : 0}%` },
        }),
        h('tick'),
      ])
    : h(
        tag,
        {
          attrs: { 'data-id': chap.id },
          hook: {
            ...onInsert(cloudEval.observe),
            postpatch(old, vnode) {
              const elm = vnode.elm as HTMLElement;
              const prevNodeCloud: CloudEval | undefined = old.data?.cloud;
              const cev = cloudEval.getCloudEval(chap.fen) || prevNodeCloud;
              if (cev?.chances != prevNodeCloud?.chances) {
                (elm.firstChild as HTMLElement).style.height = `${Math.round(
                  ((1 - (cev?.chances || 0)) / 2) * 100,
                )}%`;
                if (cev) {
                  elm.title = renderScoreAtDepth(cev);
                  elm.classList.add('mini-game__gauge--set');
                }
              }
              vnode.data!.cloud = cev;
            },
          },
        },
        [h('span.mini-game__gauge__black'), h('tick')],
      );
};

const renderUser = (player: ChapterPreviewPlayer): VNode =>
  h('span.mini-game__user', [
    playerFed(player.fed),
    h('span.name', [userTitle(player), player.name || '?']),
    player.rating ? h('span.rating', player.rating.toString()) : undefined,
  ]);

export const renderClock = (chapter: ChapterPreview, color: Color) => {
  const turnColor = fenColor(chapter.fen);
  const timeleft = computeTimeLeft(chapter, color);
  const ticking = turnColor == color && otbClockIsRunning(chapter.fen);
  return defined(timeleft)
    ? h(
        'span.mini-game__clock.mini-game__clock',
        { class: { 'clock--run': ticking } },
        formatMs(timeleft * 1000),
      )
    : undefined;
};

const computeTimeLeft = (preview: ChapterPreview, color: Color): number | undefined => {
  const clock = preview.players?.[color]?.clock;
  if (notNull(clock)) {
    if (defined(preview.lastMoveAt) && fenColor(preview.fen) == color) {
      const spent = (Date.now() - preview.lastMoveAt) / 1000;
      return Math.max(0, clock / 100 - spent);
    } else {
      return clock / 100;
    }
  } else return;
};

const boardPlayer = (preview: ChapterPreview, color: Color) => {
  const outcome = preview.status && preview.status !== '*' ? preview.status : undefined;
  const player = preview.players?.[color],
    score = outcome?.split('-')[color === 'white' ? 0 : 1];
  return h('span.mini-game__player', [
    player && renderUser(player),
    score ? h('span.mini-game__result', score) : renderClock(preview, color),
  ]);
};
