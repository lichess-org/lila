import { view as cevalView } from 'lib/ceval/ceval';
import { parseFen } from 'chessops/fen';
import { defined, repeater } from 'lib';
import * as licon from 'lib/licon';
import {
  type VNode,
  type LooseVNode,
  type LooseVNodes,
  bind,
  bindNonPassive,
  onInsert,
  dataIcon,
  hl,
} from 'lib/snabbdom';
import { playable } from 'lib/game/game';
import { bindMobileMousedown, isMobile } from 'lib/device';
import * as materialView from 'lib/game/view/material';
import { path as treePath } from 'lib/tree/tree';
import { view as actionMenu } from './actionMenu';
import retroView from '../retrospect/retroView';
import practiceView from '../practice/practiceView';
import explorerView from '../explorer/explorerView';
import { view as forkView } from '../fork';
import renderClocks from './clocks';
import * as control from '../control';
import * as chessground from '../ground';
import type AnalyseCtrl from '../ctrl';
import type { ConcealOf } from '../interfaces';
import * as pgnExport from '../pgnExport';
import { spinnerVdom as spinner, stepwiseScroll } from 'lib/view/controls';
import * as Prefs from 'lib/prefs';
import statusView from 'lib/game/view/status';
import { renderNextChapter } from '../study/nextChapter';
import { render as renderTreeView } from '../treeView/treeView';
import * as gridHacks from './gridHacks';
import { dispatchChessgroundResize } from 'lib/chessgroundResize';
import serverSideUnderboard from '../serverSideUnderboard';
import type StudyCtrl from '../study/studyCtrl';
import type RelayCtrl from '../study/relay/relayCtrl';
import type * as studyDeps from '../study/studyDeps';
import { renderPgnError } from '../pgnImport';
import { storage } from 'lib/storage';
import { backToLiveView } from '../study/relay/relayView';
import { findTag } from '../study/studyChapters';

export interface ViewContext {
  ctrl: AnalyseCtrl;
  deps?: typeof studyDeps;
  study?: StudyCtrl;
  relay?: RelayCtrl;
  allowVideo?: boolean;
  concealOf?: ConcealOf;
  showCevalPvs: boolean;
  gamebookPlayView?: VNode;
  playerBars: VNode[] | undefined;
  playerStrips: [VNode, VNode] | undefined;
  gaugeOn: boolean;
  needsInnerCoords: boolean;
  hasRelayTour: boolean;
}

export interface StudyViewContext extends ViewContext {
  study: StudyCtrl;
  deps: typeof studyDeps;
}

export interface RelayViewContext extends StudyViewContext {
  relay: RelayCtrl;
  allowVideo: boolean;
}

export function viewContext(ctrl: AnalyseCtrl, deps?: typeof studyDeps): ViewContext {
  const playerBars = deps?.renderPlayerBars(ctrl);
  return {
    ctrl,
    deps,
    study: ctrl.study,
    relay: ctrl.study?.relay,
    concealOf: makeConcealOf(ctrl),
    showCevalPvs: !ctrl.retro?.isSolving() && !ctrl.practice,
    gamebookPlayView: ctrl.study?.gamebookPlay && deps?.gbPlay.render(ctrl.study.gamebookPlay),
    playerBars,
    playerStrips: playerBars ? undefined : renderPlayerStrips(ctrl),
    gaugeOn: ctrl.showEvalGauge(),
    needsInnerCoords: ctrl.data.pref.showCaptured || !!ctrl.showEvalGauge() || !!playerBars,
    hasRelayTour: ctrl.study?.relay?.tourShow() || false,
  };
}

export function renderMain(
  { ctrl, playerBars, gaugeOn, gamebookPlayView, needsInnerCoords, hasRelayTour }: ViewContext,
  ...kids: LooseVNodes[]
): VNode {
  const isRelay = defined(ctrl.study?.relay);
  return hl(
    'main.analyse.variant-' + ctrl.data.game.variant.key,
    {
      hook: {
        insert: vn => {
          const elm = vn.elm as HTMLElement;
          forceInnerCoords(ctrl, needsInnerCoords);
          if (!!playerBars !== document.body.classList.contains('header-margin'))
            $('body').toggleClass('header-margin', !!playerBars);
          gridHacks.start(elm);
        },
        update(_, _2) {
          forceInnerCoords(ctrl, needsInnerCoords);
        },
        postpatch(old, vnode) {
          if (old.data!.gaugeOn !== gaugeOn) dispatchChessgroundResize();
          vnode.data!.gaugeOn = gaugeOn;
        },
      },
      class: {
        'comp-off': !ctrl.showComputer(),
        'gauge-on': gaugeOn,
        'has-players': !!playerBars,
        'gamebook-play': !!gamebookPlayView,
        'has-relay-tour': hasRelayTour,
        'is-relay': isRelay,
        'analyse-hunter': ctrl.opts.hunter,
        'analyse--wiki': !!ctrl.wiki && !ctrl.study,
        'relay-in-variation': !!ctrl.study?.isRelayAndInVariation(),
      },
    },
    kids,
  );
}

export function renderTools({ ctrl, deps, concealOf, allowVideo }: ViewContext, embedded?: LooseVNode) {
  return hl(addChapterId(ctrl.study, 'div.analyse__tools'), [
    allowVideo && embedded,
    ctrl.actionMenu()
      ? [actionMenu(ctrl)]
      : [
          cevalView.renderCeval(ctrl),
          !ctrl.retro?.isSolving() && !ctrl.practice && cevalView.renderPvs(ctrl),
          renderMoveList(ctrl, deps, concealOf),
          deps?.gbEdit.running(ctrl) ? deps?.gbEdit.render(ctrl) : undefined,
          backToLiveView(ctrl),
          forkView(ctrl, concealOf),
          retroView(ctrl) || practiceView(ctrl) || explorerView(ctrl),
        ],
  ]);
}

export function renderBoard({ ctrl, study, playerBars, playerStrips }: ViewContext) {
  return hl(
    addChapterId(study, 'div.analyse__board.main-board'),
    {
      hook:
        'ontouchstart' in window || !storage.boolean('scrollMoves').getOrDefault(true)
          ? undefined
          : bindNonPassive(
              'wheel',
              stepwiseScroll((e: WheelEvent, scroll: boolean) => {
                if (ctrl.gamebookPlay()) return;
                const target = e.target as HTMLElement;
                if (
                  target.tagName !== 'PIECE' &&
                  target.tagName !== 'SQUARE' &&
                  target.tagName !== 'CG-BOARD'
                )
                  return;
                if (scroll) {
                  e.preventDefault();
                  if (e.deltaY > 0) control.next(ctrl);
                  else if (e.deltaY < 0) control.prev(ctrl);
                  ctrl.redraw();
                }
              }),
            ),
    },
    [
      playerStrips,
      playerBars?.[ctrl.bottomIsWhite() ? 1 : 0],
      chessground.render(ctrl),
      playerBars?.[ctrl.bottomIsWhite() ? 0 : 1],
      ctrl.promotion.view(ctrl.data.game.variant.key === 'antichess'),
    ],
  );
}

export function renderUnderboard({ ctrl, deps, study }: ViewContext) {
  return hl(
    'div.analyse__underboard',
    {
      hook:
        ctrl.synthetic || playable(ctrl.data) ? undefined : onInsert(elm => serverSideUnderboard(elm, ctrl)),
    },
    study ? deps?.studyView.underboard(ctrl) : [renderInputs(ctrl)],
  );
}

export function renderInputs(ctrl: AnalyseCtrl): VNode | undefined {
  if (ctrl.ongoing || !ctrl.data.userAnalysis) return;
  if (ctrl.redirecting) return spinner();
  return hl('div.copyables', [
    hl('div.pair', [
      hl('label.name', 'FEN'),
      hl('input.copyable', {
        attrs: { spellcheck: 'false', enterkeyhint: 'done' },
        hook: {
          insert: vnode => {
            const el = vnode.elm as HTMLInputElement;
            el.value = defined(ctrl.fenInput) ? ctrl.fenInput : ctrl.node.fen;
            el.addEventListener('change', () => {
              if (el.value !== ctrl.node.fen && el.reportValidity()) ctrl.changeFen(el.value.trim());
            });
            el.addEventListener('input', () => {
              ctrl.fenInput = el.value;
              el.setCustomValidity(parseFen(el.value.trim()).isOk ? '' : 'Invalid FEN');
            });
          },
          postpatch: (_, vnode) => {
            const el = vnode.elm as HTMLInputElement;
            if (!defined(ctrl.fenInput)) {
              el.value = ctrl.node.fen;
              el.setCustomValidity('');
            } else if (el.value !== ctrl.fenInput) el.value = ctrl.fenInput;
          },
        },
      }),
    ]),
    hl('div.pgn', [
      hl('div.pair', [
        hl('label.name', 'PGN'),
        hl('textarea.copyable', {
          attrs: { spellcheck: 'false' },
          class: { 'is-error': !!ctrl.pgnError },
          hook: {
            ...onInsert((el: HTMLTextAreaElement) => {
              el.value = defined(ctrl.pgnInput) ? ctrl.pgnInput : pgnExport.renderFullTxt(ctrl);
              const changePgnIfDifferent = () =>
                el.value !== pgnExport.renderFullTxt(ctrl) && ctrl.changePgn(el.value, true);

              el.addEventListener('input', () => (ctrl.pgnInput = el.value));

              el.addEventListener('keypress', (e: KeyboardEvent) => {
                if (e.key !== 'Enter' || e.shiftKey || e.ctrlKey || e.altKey || e.metaKey || isMobile())
                  return;
                else if (changePgnIfDifferent()) e.preventDefault();
              });
              if (isMobile()) el.addEventListener('focusout', changePgnIfDifferent);
            }),
            postpatch: (_, vnode) => {
              (vnode.elm as HTMLTextAreaElement).value = defined(ctrl.pgnInput)
                ? ctrl.pgnInput
                : pgnExport.renderFullTxt(ctrl);
            },
          },
        }),
        !isMobile() &&
          hl(
            'button.button.button-thin.bottom-item.bottom-action.text',
            {
              attrs: dataIcon(licon.PlayTriangle),
              hook: bind('click', _ => {
                const pgn = $('.copyables .pgn textarea').val() as string;
                if (pgn !== pgnExport.renderFullTxt(ctrl)) ctrl.changePgn(pgn, true);
              }),
            },
            i18n.site.importPgn,
          ),
        hl(
          'div.bottom-item.bottom-error',
          { attrs: dataIcon(licon.CautionTriangle), class: { 'is-error': !!ctrl.pgnError } },
          renderPgnError(ctrl.pgnError),
        ),
      ]),
    ]),
  ]);
}

export function renderControls(ctrl: AnalyseCtrl) {
  const canJumpPrev = ctrl.path !== '',
    canJumpNext = !!ctrl.node.children[0],
    menuIsOpen = ctrl.actionMenu();
  return hl(
    'div.analyse__controls.analyse-controls',
    {
      hook: onInsert(
        bindMobileMousedown(e => {
          const action = dataAct(e);
          if (action === 'prev' || action === 'next')
            repeater(() => {
              control[action](ctrl);
              ctrl.redraw();
            }, e);
          else if (action === 'first') control.first(ctrl);
          else if (action === 'last') control.last(ctrl);
          else if (action === 'explorer') ctrl.toggleExplorer();
          else if (action === 'practice') ctrl.togglePractice();
          else if (action === 'menu') ctrl.actionMenu.toggle();
          else if (action === 'analysis' && ctrl.studyPractice)
            window.open(ctrl.studyPractice.analysisUrl(), '_blank');
        }, ctrl.redraw),
      ),
    },
    [
      hl(
        'div.features',
        ctrl.studyPractice
          ? [
              hl('button.fbt', {
                attrs: { title: i18n.site.analysis, 'data-act': 'analysis', 'data-icon': licon.Microscope },
              }),
            ]
          : [
              hl('button.fbt', {
                attrs: {
                  title: i18n.site.openingExplorerAndTablebase,
                  'data-act': 'explorer',
                  'data-icon': licon.Book,
                },
                class: {
                  hidden: menuIsOpen || !ctrl.explorer.allowed() || !!ctrl.retro,
                  active: ctrl.explorer.enabled(),
                },
              }),
              ctrl.ceval.possible &&
                ctrl.ceval.allowed() &&
                !ctrl.isGamebook() &&
                !ctrl.isEmbed &&
                hl('button.fbt', {
                  attrs: {
                    title: i18n.site.practiceWithComputer,
                    'data-act': 'practice',
                    'data-icon': licon.Bullseye,
                  },
                  class: { hidden: menuIsOpen || !!ctrl.retro, active: !!ctrl.practice },
                }),
            ],
      ),
      hl('div.jumps', [
        jumpButton(licon.JumpFirst, 'first', canJumpPrev),
        jumpButton(licon.JumpPrev, 'prev', canJumpPrev),
        jumpButton(licon.JumpNext, 'next', canJumpNext),
        jumpButton(licon.JumpLast, 'last', canJumpNext),
      ]),
      ctrl.studyPractice
        ? hl('div.noop')
        : hl('button.fbt', {
            class: { active: menuIsOpen },
            attrs: { title: i18n.site.menu, 'data-act': 'menu', 'data-icon': licon.Hamburger },
          }),
    ],
  );
}

export function renderResult(ctrl: AnalyseCtrl): VNode[] {
  const termination = () => ctrl.study && findTag(ctrl.study.data.chapter.tags, 'termination');
  const render = (result: string, status: string) => [
    hl('div.result', result),
    hl('div.status', [termination() && `${termination()} • `, status]),
  ];
  if (ctrl.data.game.status.id >= 30) {
    const winner = ctrl.data.game.winner;
    const result = winner === 'white' ? '1-0' : winner === 'black' ? '0-1' : '½-½';
    return render(result, statusView(ctrl.data));
  } else if (ctrl.study?.multiBoard.showResults()) {
    const result = findTag(ctrl.study.data.chapter.tags, 'result')?.replace('1/2', '½');
    if (!result || result === '*') return [];
    if (result === '1-0') return render(result, i18n.site.whiteIsVictorious);
    if (result === '0-1') return render(result, i18n.site.blackIsVictorious);
    if (result === '0-0') return render(result, i18n.study.doubleDefeat);
    if (result === '½-0') return render(result, i18n.study.blackDefeatWhiteCanNotWin);
    if (result === '0-½') return render(result, i18n.study.whiteDefeatBlackCanNotWin);
    return render('½-½', i18n.site.draw);
  }
  return [];
}

const renderMoveList = (ctrl: AnalyseCtrl, deps?: typeof studyDeps, concealOf?: ConcealOf): VNode =>
  hl('div.analyse__moves.areplay', [
    hl(`div.areplay__v${ctrl.treeVersion}`, [renderTreeView(ctrl, concealOf), renderResult(ctrl)]),
    !ctrl.practice && !deps?.gbEdit.running(ctrl) && renderNextChapter(ctrl),
  ]);

export const renderMaterialDiffs = (ctrl: AnalyseCtrl): [VNode, VNode] =>
  materialView.renderMaterialDiffs(
    !!ctrl.data.pref.showCaptured,
    ctrl.bottomColor(),
    ctrl.node.fen,
    !!(ctrl.data.player.checks || ctrl.data.opponent.checks), // showChecks
    ctrl.nodeList,
    ctrl.node.ply,
  );

export const addChapterId = (study: StudyCtrl | undefined, cssClass: string) =>
  cssClass + (study && study.data.chapter ? '.' + study.data.chapter.id : '');

function makeConcealOf(ctrl: AnalyseCtrl): ConcealOf | undefined {
  if (defined(ctrl.study?.relay)) {
    if (!ctrl.study.multiBoard.showResults()) {
      return (isMainline: boolean) => (path: Tree.Path, node: Tree.Node) => {
        if (isMainline && ctrl.node.ply >= node.ply) return null;
        if (treePath.contains(ctrl.path, path)) return null;
        return 'hide';
      };
    }
    return undefined;
  }

  const conceal =
    ctrl.study && ctrl.study.data.chapter.conceal !== undefined
      ? {
          owner: ctrl.study.isChapterOwner(),
          ply: ctrl.study.data.chapter.conceal,
        }
      : null;
  if (conceal)
    return (isMainline: boolean) => (path: Tree.Path, node: Tree.Node) => {
      if (!conceal || (isMainline && conceal.ply >= node.ply)) return null;
      if (treePath.contains(ctrl.path, path)) return null;
      return conceal.owner ? 'conceal' : 'hide';
    };
  return undefined;
}

let prevForceInnerCoords: boolean;
function forceInnerCoords(ctrl: AnalyseCtrl, v: boolean) {
  if (ctrl.data.pref.coords === Prefs.Coords.Outside) {
    if (prevForceInnerCoords !== v) {
      prevForceInnerCoords = v;
      $('body').toggleClass('coords-in', v).toggleClass('coords-out', !v);
    }
  }
}

const jumpButton = (icon: string, effect: string, enabled: boolean): VNode =>
  hl('button.fbt', { class: { disabled: !enabled }, attrs: { 'data-act': effect, 'data-icon': icon } });

const dataAct = (e: Event): string | null => {
  const target = e.target as HTMLElement;
  return target.getAttribute('data-act') || (target.parentNode as HTMLElement).getAttribute('data-act');
};

function renderPlayerStrips(ctrl: AnalyseCtrl): [VNode, VNode] | undefined {
  const renderPlayerStrip = (cls: string, materialDiff: VNode, clock?: VNode): VNode =>
    hl('div.analyse__player_strip.' + cls, [materialDiff, clock]);

  const clocks = renderClocks(ctrl, ctrl.path),
    whitePov = ctrl.bottomIsWhite(),
    materialDiffs = renderMaterialDiffs(ctrl);

  return [
    renderPlayerStrip('top', materialDiffs[0], clocks?.[whitePov ? 1 : 0]),
    renderPlayerStrip('bottom', materialDiffs[1], clocks?.[whitePov ? 0 : 1]),
  ];
}
