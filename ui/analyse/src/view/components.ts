import { view as cevalView } from 'ceval';
import { VNode } from 'snabbdom';
import { parseFen } from 'chessops/fen';
import { defined } from 'common';
import * as licon from 'common/licon';
import {
  bind,
  bindNonPassive,
  onInsert,
  dataIcon,
  VNodeKids,
  looseH as h,
  MaybeVNode,
} from 'common/snabbdom';
import { playable } from 'game';
import { bindMobileMousedown, isMobile } from 'common/device';
import * as materialView from 'game/view/material';
import { path as treePath } from 'tree';
import { view as actionMenu } from './actionMenu';
import retroView from '../retrospect/retroView';
import practiceView from '../practice/practiceView';
import explorerView from '../explorer/explorerView';
import { view as forkView } from '../fork';
import renderClocks from './clocks';
import * as control from '../control';
import * as chessground from '../ground';
import AnalyseCtrl from '../ctrl';
import { ConcealOf } from '../interfaces';
import * as pgnExport from '../pgnExport';
import { spinnerVdom as spinner } from 'common/spinner';
import * as Prefs from 'common/prefs';
import statusView from 'game/view/status';
import { stepwiseScroll } from 'common/scroll';
import { renderNextChapter } from '../study/nextChapter';
import { render as renderTreeView } from '../treeView/treeView';
import * as gridHacks from './gridHacks';
import { dispatchChessgroundResize } from 'common/resize';
import serverSideUnderboard from '../serverSideUnderboard';
import StudyCtrl from '../study/studyCtrl';
import RelayCtrl from '../study/relay/relayCtrl';
import type * as studyDeps from '../study/studyDeps';
import { renderPgnError } from '../pgnImport';

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
  kids: VNodeKids,
): VNode {
  return h(
    'main.analyse.variant-' + ctrl.data.game.variant.key,
    {
      hook: {
        insert: vn => {
          const elm = vn.elm as HTMLElement;
          forceInnerCoords(ctrl, needsInnerCoords);
          if (!!playerBars != document.body.classList.contains('header-margin')) {
            $('body').toggleClass('header-margin', !!playerBars);
          }
          !hasRelayTour && makeChat(ctrl, c => elm.appendChild(c));
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
        'is-relay': ctrl.study?.relay !== undefined,
        'analyse-hunter': ctrl.opts.hunter,
        'analyse--wiki': !!ctrl.wiki && !ctrl.study,
      },
    },
    kids,
  );
}

export function renderTools({ ctrl, deps, concealOf, allowVideo }: ViewContext, embedded?: MaybeVNode) {
  return h(addChapterId(ctrl.study, 'div.analyse__tools'), [
    allowVideo && embedded,
    ...(ctrl.actionMenu()
      ? [actionMenu(ctrl)]
      : [
          ...cevalView.renderCeval(ctrl),
          !ctrl.retro?.isSolving() && !ctrl.practice && cevalView.renderPvs(ctrl),
          renderMoveList(ctrl, deps, concealOf),
          deps?.gbEdit.running(ctrl) ? deps?.gbEdit.render(ctrl) : undefined,
          forkView(ctrl, concealOf),
          retroView(ctrl) || practiceView(ctrl) || explorerView(ctrl),
        ]),
  ]);
}

export function renderBoard({ ctrl, study, playerBars, playerStrips }: ViewContext) {
  return h(
    addChapterId(study, 'div.analyse__board.main-board'),
    {
      hook:
        'ontouchstart' in window || !site.storage.boolean('scrollMoves').getOrDefault(true)
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
                e.preventDefault();
                if (e.deltaY > 0 && scroll) control.next(ctrl);
                else if (e.deltaY < 0 && scroll) control.prev(ctrl);
                ctrl.redraw();
              }),
            ),
    },
    [
      ...(playerStrips || []),
      playerBars?.[ctrl.bottomIsWhite() ? 1 : 0],
      chessground.render(ctrl),
      playerBars?.[ctrl.bottomIsWhite() ? 0 : 1],
      ctrl.promotion.view(ctrl.data.game.variant.key === 'antichess'),
    ],
  );
}

export function renderUnderboard({ ctrl, deps, study }: ViewContext) {
  return h(
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
  return h('div.copyables', [
    h('div.pair', [
      h('label.name', 'FEN'),
      h('input.copyable', {
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
            } else if (el.value != ctrl.fenInput) el.value = ctrl.fenInput;
          },
        },
      }),
    ]),
    h('div.pgn', [
      h('div.pair', [
        h('label.name', 'PGN'),
        h('textarea.copyable', {
          attrs: { spellcheck: 'false' },
          class: { 'is-error': !!ctrl.pgnError },
          hook: {
            ...onInsert((el: HTMLTextAreaElement) => {
              el.value = defined(ctrl.pgnInput) ? ctrl.pgnInput : pgnExport.renderFullTxt(ctrl);
              const changePgnIfDifferent = () =>
                el.value !== pgnExport.renderFullTxt(ctrl) && ctrl.changePgn(el.value, true);

              el.addEventListener('input', () => (ctrl.pgnInput = el.value));

              el.addEventListener('keypress', (e: KeyboardEvent) => {
                if (e.key != 'Enter' || e.shiftKey || e.ctrlKey || e.altKey || e.metaKey || isMobile())
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
          h(
            'button.button.button-thin.bottom-item.bottom-action.text',
            {
              attrs: dataIcon(licon.PlayTriangle),
              hook: bind('click', _ => {
                const pgn = $('.copyables .pgn textarea').val() as string;
                if (pgn !== pgnExport.renderFullTxt(ctrl)) ctrl.changePgn(pgn, true);
              }),
            },
            ctrl.trans.noarg('importPgn'),
          ),
        h(
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
    menuIsOpen = ctrl.actionMenu(),
    noarg = ctrl.trans.noarg;
  return h(
    'div.analyse__controls.analyse-controls',
    {
      hook: onInsert(
        bindMobileMousedown(e => {
          const action = dataAct(e);
          if (action === 'prev' || action === 'next') repeater(ctrl, action, e);
          else if (action === 'first') control.first(ctrl);
          else if (action === 'last') control.last(ctrl);
          else if (action === 'explorer') ctrl.toggleExplorer();
          else if (action === 'practice') ctrl.togglePractice();
          else if (action === 'menu') ctrl.actionMenu.toggle();
          else if (action === 'analysis' && ctrl.studyPractice)
            window.open(ctrl.studyPractice.analysisUrl(), '_blank', 'noopener');
        }, ctrl.redraw),
      ),
    },
    [
      h(
        'div.features',
        ctrl.studyPractice
          ? [
              h('button.fbt', {
                attrs: { title: noarg('analysis'), 'data-act': 'analysis', 'data-icon': licon.Microscope },
              }),
            ]
          : [
              h('button.fbt', {
                attrs: {
                  title: noarg('openingExplorerAndTablebase'),
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
                h('button.fbt', {
                  attrs: {
                    title: noarg('practiceWithComputer'),
                    'data-act': 'practice',
                    'data-icon': licon.Bullseye,
                  },
                  class: { hidden: menuIsOpen || !!ctrl.retro, active: !!ctrl.practice },
                }),
            ],
      ),
      h('div.jumps', [
        jumpButton(licon.JumpFirst, 'first', canJumpPrev),
        jumpButton(licon.JumpPrev, 'prev', canJumpPrev),
        jumpButton(licon.JumpNext, 'next', canJumpNext),
        jumpButton(licon.JumpLast, 'last', canJumpNext),
      ]),
      ctrl.studyPractice
        ? h('div.noop')
        : h('button.fbt', {
            class: { active: menuIsOpen },
            attrs: { title: noarg('menu'), 'data-act': 'menu', 'data-icon': licon.Hamburger },
          }),
    ],
  );
}

function renderMoveList(ctrl: AnalyseCtrl, deps?: typeof studyDeps, concealOf?: ConcealOf) {
  function renderResult(ctrl: AnalyseCtrl, deps?: typeof studyDeps): VNode[] {
    const render = (result: string, status: VNodeKids) => [h('div.result', result), h('div.status', status)];
    if (ctrl.data.game.status.id >= 30) {
      let result;
      switch (ctrl.data.game.winner) {
        case 'white':
          result = '1-0';
          break;
        case 'black':
          result = '0-1';
          break;
        default:
          result = '½-½';
      }
      return render(result, statusView(ctrl));
    } else if (ctrl.study) {
      const result = deps?.findTag(ctrl.study.data.chapter.tags, 'result');
      if (!result || result === '*') return [];
      if (result === '1-0') return render(result, [ctrl.trans.noarg('whiteIsVictorious')]);
      if (result === '0-1') return render(result, [ctrl.trans.noarg('blackIsVictorious')]);
      return render('½-½', [ctrl.trans.noarg('draw')]);
    }
    return [];
  }
  return h('div.analyse__moves.areplay', [
    h(`div.areplay__v${ctrl.treeVersion}`, [renderTreeView(ctrl, concealOf), ...renderResult(ctrl)]),
    !ctrl.practice && !deps?.gbEdit.running(ctrl) && renderNextChapter(ctrl),
  ]);
}

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

export function makeChat(ctrl: AnalyseCtrl, insert: (chat: HTMLElement) => void) {
  if (ctrl.opts.chat) {
    const chatEl = document.createElement('section');
    chatEl.classList.add('mchat');
    insert(chatEl);
    const chatOpts = ctrl.opts.chat;
    chatOpts.instance?.then(c => c.destroy());
    chatOpts.enhance = { plies: true, boards: !!ctrl.study?.relay };
    chatOpts.instance = site.makeChat(chatOpts);
  }
}

function makeConcealOf(ctrl: AnalyseCtrl): ConcealOf | undefined {
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
  h('button.fbt', { class: { disabled: !enabled }, attrs: { 'data-act': effect, 'data-icon': icon } });

const dataAct = (e: Event): string | null => {
  const target = e.target as HTMLElement;
  return target.getAttribute('data-act') || (target.parentNode as HTMLElement).getAttribute('data-act');
};

function repeater(ctrl: AnalyseCtrl, action: 'prev' | 'next', e: Event) {
  const repeat = () => {
    control[action](ctrl);
    ctrl.redraw();
    delay = Math.max(100, delay - delay / 15);
    timeout = setTimeout(repeat, delay);
  };
  let delay = 350;
  let timeout = setTimeout(repeat, 500);
  control[action](ctrl);
  const eventName = e.type == 'touchstart' ? 'touchend' : 'mouseup';
  document.addEventListener(eventName, () => clearTimeout(timeout), { once: true });
}

function renderPlayerStrips(ctrl: AnalyseCtrl): [VNode, VNode] | undefined {
  const renderPlayerStrip = (cls: string, materialDiff: VNode, clock?: VNode): VNode =>
    h('div.analyse__player_strip.' + cls, [materialDiff, clock]);

  const clocks = renderClocks(ctrl),
    whitePov = ctrl.bottomIsWhite(),
    materialDiffs = renderMaterialDiffs(ctrl);

  return [
    renderPlayerStrip('top', materialDiffs[0], clocks?.[whitePov ? 1 : 0]),
    renderPlayerStrip('bottom', materialDiffs[1], clocks?.[whitePov ? 0 : 1]),
  ];
}
