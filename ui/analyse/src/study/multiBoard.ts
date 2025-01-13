import * as licon from 'common/licon';
import { otbClockIsRunning, formatMs } from 'common/clock';
import { fenColor } from 'common/miniBoard';
import { type MaybeVNode, type VNode, bind, dataIcon, onInsert } from 'common/snabbdom';
import { opposite as cgOpposite, uciToMove } from 'chessground/util';
import type { ChapterId, ChapterPreview, StudyPlayer } from './interfaces';
import type StudyCtrl from './studyCtrl';
import { type CloudEval, type MultiCloudEval, renderEvalToggle, renderScore } from './multiCloudEval';
import { type Prop, type Toggle, defined, notNull, prop, toggle } from 'common';
import type { Color } from 'chessops';
import { type StudyChapters, gameLinkAttrs, gameLinksListener } from './studyChapters';
import { playerFed } from './playerBars';
import { userTitle } from 'common/userLink';
import { h } from 'snabbdom';
import { storage, storedBooleanProp, StoredProp } from 'common/storage';
import { Chessground as makeChessground } from 'chessground';
import { EMPTY_BOARD_FEN } from 'chessops/fen';

export class MultiBoardCtrl {
  playing: Toggle;
  showResults: StoredProp<boolean>;
  teamSelect: Prop<string> = prop('');
  page: number = 1;
  maxPerPageStorage = storage.make('study.multiBoard.maxPerPage');

  constructor(
    readonly chapters: StudyChapters,
    readonly multiCloudEval: MultiCloudEval | undefined,
    private readonly initialTeamSelect: ChapterId | undefined,
    readonly redraw: () => void,
  ) {
    this.playing = toggle(false, this.redraw);
    this.showResults = storedBooleanProp('study.showResults', true);
    if (this.initialTeamSelect) this.onChapterChange(this.initialTeamSelect);
  }

  gameTeam = (id: ChapterId): string | undefined => this.chapters.get(id)?.players?.white.team;

  onChapterChange = (id: ChapterId) => {
    const players = this.chapters.get(id)?.players;
    if (players) {
      const teams = [players.white.team, players.black.team].filter(t => !!t);
      if (!teams.includes(this.teamSelect())) this.teamSelect(teams[0] || '');
    }
  };

  maxPerPage = () => Math.min(32, parseInt(this.maxPerPageStorage.get() || '12'));

  private chapterFilter = (c: ChapterPreview) => {
    const t = this.teamSelect();
    return (
      (!this.playing() || c.playing) && (!t || c.players?.white.team === t || c.players?.black.team === t)
    );
  };

  setMaxPerPage = (nb: string) => {
    this.maxPerPageStorage.set(nb);
    this.redraw();
  };

  pager = (): Paginator<ChapterPreview> => {
    const maxPerPage = this.maxPerPage();
    const filteredResults = this.chapters.all().filter(this.chapterFilter);
    const currentPageResults = filteredResults.slice((this.page - 1) * maxPerPage, this.page * maxPerPage);
    const nbResults = filteredResults.length;
    const nbPages = Math.floor((nbResults + maxPerPage - 1) / maxPerPage);
    return {
      currentPage: this.page,
      maxPerPage: maxPerPage,
      currentPageResults,
      nbResults,
      previousPage: this.page > 1 ? this.page - 1 : undefined,
      nextPage: this.page < nbPages && currentPageResults.length ? this.page + 1 : undefined,
      nbPages,
    };
  };
  setPage = (page: number) => {
    if (this.page !== page) {
      this.page = page;
      this.redraw();
    }
  };
  nextPage = () => this.setPage(this.page + 1);
  prevPage = () => this.setPage(this.page - 1);
  lastPage = () => this.setPage(this.pager().nbPages);

  computeTeamList = () => {
    const teams = new Set<string>();
    this.chapters.all().forEach(c => {
      if (c.players?.white.team) teams.add(c.players.white.team);
      if (c.players?.black.team) teams.add(c.players.black.team);
    });
    return Array.from(teams).sort();
  };
}

export function view(ctrl: MultiBoardCtrl, study: StudyCtrl): MaybeVNode {
  const pager = ctrl.pager();
  const cloudEval = ctrl.multiCloudEval?.thisIfShowEval();
  const baseUrl = study.relay?.roundPath() || study.baseUrl();
  return h('div.study__multiboard', [
    h('div.study__multiboard__top', [
      renderPagerNav(pager, ctrl),
      h('div.study__multiboard__options', [
        ctrl.multiCloudEval &&
          h('label.eval', [renderEvalToggle(ctrl.multiCloudEval), i18n.study.showEvalBar]),
        renderPlayingToggle(ctrl),
        renderShowResultsToggle(ctrl),
      ]),
    ]),
    !ctrl.showResults()
      ? h(
          'div.empty-boards-note.text',
          { attrs: dataIcon(licon.InfoCircle) },
          'Since you chose to hide the results, all the preview boards are empty to avoid spoilers.',
        )
      : undefined,
    h(
      'div.now-playing',
      {
        hook: {
          insert: gameLinksListener(study.chapterSelect),
        },
      },
      pager.currentPageResults.map(makePreview(baseUrl, study.vm.chapterId, cloudEval, ctrl.showResults())),
    ),
  ]);
}

function renderPagerNav(pager: Paginator<ChapterPreview>, ctrl: MultiBoardCtrl): VNode {
  const page = ctrl.page,
    from = Math.min(pager.nbResults, (page - 1) * pager.maxPerPage + 1),
    to = Math.min(pager.nbResults, page * pager.maxPerPage),
    max = ctrl.maxPerPage();
  return h('div.study__multiboard__pager', [
    pagerButton(i18n.study.first, licon.JumpFirst, () => ctrl.setPage(1), page > 1, ctrl),
    pagerButton(i18n.study.previous, licon.JumpPrev, ctrl.prevPage, page > 1, ctrl),
    h('span.page', `${from}-${to} / ${pager.nbResults}`),
    pagerButton(i18n.study.next, licon.JumpNext, ctrl.nextPage, page < pager.nbPages, ctrl),
    pagerButton(i18n.study.last, licon.JumpLast, ctrl.lastPage, page < pager.nbPages, ctrl),
    teamSelector(ctrl),
    h(
      'select.study__multiboard__pager__max-per-page',
      { hook: bind('change', (e: Event) => ctrl.setMaxPerPage((e.target as HTMLOptionElement).value)) },
      [4, 6, 8, 10, 12, 16, 20, 24, 32].map(nb =>
        h('option', { attrs: { value: nb, selected: nb === max } }, i18n.study.perPage(nb)),
      ),
    ),
  ]);
}

const teamSelector = (ctrl: MultiBoardCtrl) => {
  const allTeams = ctrl.computeTeamList();
  const currentTeam = ctrl.teamSelect();
  return allTeams.length
    ? h(
        'select',
        {
          hook: bind('change', e => ctrl.teamSelect((e.target as HTMLOptionElement).value), ctrl.redraw),
        },
        [i18n.broadcast.allTeams, ...allTeams].map((t, i) =>
          h('option', { attrs: { value: i ? t : '', selected: i && t === currentTeam } }, t),
        ),
      )
    : undefined;
};

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

const renderPlayingToggle = (ctrl: MultiBoardCtrl): MaybeVNode =>
  h('label.playing', [
    h('input', {
      attrs: { type: 'checkbox', checked: ctrl.playing() },
      hook: bind('change', e => ctrl.playing((e.target as HTMLInputElement).checked)),
    }),
    i18n.study.playing,
  ]);

const renderShowResultsToggle = (ctrl: MultiBoardCtrl): MaybeVNode =>
  h('label.results', [
    h('input', {
      attrs: { type: 'checkbox', checked: ctrl.showResults() },
      hook: bind('change', e => ctrl.showResults((e.target as HTMLInputElement).checked), ctrl.redraw),
    }),
    i18n.study.showResults,
  ]);

const previewToCgConfig = (cp: ChapterPreview): CgConfig => ({
  fen: cp.fen,
  lastMove: uciToMove(cp.lastMove),
  turnColor: fenColor(cp.fen),
  check: !!cp.check,
});

const makePreview =
  (roundPath: string, current: ChapterId, cloudEval?: MultiCloudEval, showResults?: boolean) =>
  (preview: ChapterPreview) => {
    const orientation = preview.orientation || 'white';
    return h(
      `a.mini-game.is2d.chap-${preview.id}${showResults ? '' : '.no-spoilers'}`,
      {
        class: { active: preview.id === current },
        attrs: gameLinkAttrs(roundPath, preview),
      },
      [
        boardPlayer(preview, cgOpposite(orientation), showResults),
        h('span.cg-gauge', [
          showResults ? cloudEval && verticalEvalGauge(preview, cloudEval) : undefined,
          h(
            'span.mini-game__board',
            h('span.cg-wrap', {
              hook: {
                insert(vnode) {
                  const el = vnode.elm as HTMLElement;
                  vnode.data!.cg = makeChessground(el, {
                    coordinates: false,
                    viewOnly: true,
                    orientation,
                    drawable: { enabled: false, visible: false },
                    ...(showResults ? previewToCgConfig(preview) : { fen: EMPTY_BOARD_FEN }),
                  });
                  vnode.data!.fen = preview.fen;
                },
                postpatch(old, vnode) {
                  if (!showResults) return;
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
        boardPlayer(preview, orientation, showResults),
      ],
    );
  };

export const verticalEvalGauge = (chap: ChapterPreview, cloudEval: MultiCloudEval): MaybeVNode => {
  const tag = `span.mini-game__gauge${chap.orientation === 'black' ? ' mini-game__gauge--flip' : ''}${
    chap.check === '#' ? ' mini-game__gauge--set' : ''
  }`;
  return chap.check === '#'
    ? h(tag, { attrs: { 'data-id': chap.id, title: 'Checkmate' } }, [
        h('span.mini-game__gauge__black', {
          attrs: { style: `height: ${fenColor(chap.fen) === 'white' ? 100 : 0}%` },
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
              if (cev?.chances !== prevNodeCloud?.chances) {
                (elm.firstChild as HTMLElement).style.height = `${Math.round(
                  ((1 - (cev?.chances || 0)) / 2) * 100,
                )}%`;
                if (cev) {
                  elm.title = renderScore(cev);
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

const renderUser = (player: StudyPlayer): VNode =>
  h('span.mini-game__user', [
    playerFed(player.fed),
    h('span.name', [userTitle(player), player.name || '?']),
    player.rating ? h('span.rating', player.rating.toString()) : undefined,
  ]);

export const renderClock = (chapter: ChapterPreview, color: Color) => {
  const turnColor = fenColor(chapter.fen);
  const timeleft = computeTimeLeft(chapter, color);
  const ticking = turnColor === color && otbClockIsRunning(chapter.fen);
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
    if (defined(preview.lastMoveAt) && defined(preview.lastMove) && fenColor(preview.fen) === color) {
      const spent = (Date.now() - preview.lastMoveAt) / 1000;
      return Math.max(0, clock / 100 - spent);
    } else {
      return clock / 100;
    }
  } else return;
};

const boardPlayer = (preview: ChapterPreview, color: Color, showResults?: boolean) => {
  const outcome = preview.status && preview.status !== '*' ? preview.status : undefined;
  const player = preview.players?.[color],
    score = outcome?.split('-')[color === 'white' ? 0 : 1];
  return h('span.mini-game__player', [
    player && renderUser(player),
    showResults ? (score ? h('span.mini-game__result', score) : renderClock(preview, color)) : undefined,
  ]);
};
