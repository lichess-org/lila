import { view as cevalView } from 'ceval';
import { read as readFen } from 'chessground/fen';
import { parseFen } from 'chessops/fen';
import { defined } from 'common';
import {
  bind,
  bindNonPassive,
  bindMobileMousedown,
  MaybeVNode,
  MaybeVNodes,
  onInsert,
  dataIcon,
} from 'common/snabbdom';
import { getPlayer, playable } from 'game';
import * as router from 'game/router';
import * as materialView from 'game/view/material';
import statusView from 'game/view/status';
import { h, VNode } from 'snabbdom';
import { ops as treeOps, path as treePath } from 'tree';
import { render as acplView } from './acpl';
import { view as actionMenu } from './actionMenu';
import renderClocks from './clocks';
import * as control from './control';
import crazyView from './crazy/crazyView';
import AnalyseCtrl from './ctrl';
import explorerView from './explorer/explorerView';
import forecastView from './forecast/forecastView';
import { view as forkView } from './fork';
import * as gridHacks from './gridHacks';
import * as chessground from './ground';
import { ConcealOf } from './interfaces';
import { view as keyboardView } from './keyboard';
import * as pgnExport from './pgnExport';
import practiceView from './practice/practiceView';
import retroView from './retrospect/retroView';
import serverSideUnderboard from './serverSideUnderboard';
import * as gbEdit from './study/gamebook/gamebookEdit';
import * as gbPlay from './study/gamebook/gamebookPlayView';
import { StudyCtrl } from './study/interfaces';
import renderPlayerBars from './study/playerBars';
import * as studyPracticeView from './study/practice/studyPracticeView';
import relayManager from './study/relay/relayManagerView';
import relayTour from './study/relay/relayTourView';
import { findTag } from './study/studyChapters';
import * as studyView from './study/studyView';
import { render as renderTreeView } from './treeView/treeView';
import { spinnerVdom as spinner } from 'common/spinner';
import stepwiseScroll from 'common/wheel';

function renderResult(ctrl: AnalyseCtrl): VNode[] {
  const render = (result: string, status: MaybeVNodes) => [h('div.result', result), h('div.status', status)];
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
    const winner = getPlayer(ctrl.data, ctrl.data.game.winner!);
    return render(result, [
      statusView(ctrl),
      winner ? ' • ' + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null,
    ]);
  } else if (ctrl.study) {
    const result = findTag(ctrl.study.data.chapter.tags, 'result');
    if (!result || result === '*') return [];
    if (result === '1-0') return render(result, [ctrl.trans.noarg('whiteIsVictorious')]);
    if (result === '0-1') return render(result, [ctrl.trans.noarg('blackIsVictorious')]);
    return render('½-½', [ctrl.trans.noarg('draw')]);
  }
  return [];
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

export const renderNextChapter = (ctrl: AnalyseCtrl) =>
  !ctrl.embed && ctrl.study?.hasNextChapter()
    ? h(
        'button.next.text',
        {
          attrs: {
            'data-icon': '',
            type: 'button',
          },
          hook: bind('click', ctrl.study.goToNextChapter),
          class: {
            highlighted: !!ctrl.outcome() || ctrl.node == treeOps.last(ctrl.mainline),
          },
        },
        ctrl.trans.noarg('nextChapter')
      )
    : null;

const renderAnalyse = (ctrl: AnalyseCtrl, concealOf?: ConcealOf) =>
  h('div.analyse__moves.areplay', [
    h('div', [
      ctrl.embed && ctrl.study ? h('div.chapter-name', ctrl.study.currentChapter().name) : null,
      renderTreeView(ctrl, concealOf),
      ...renderResult(ctrl),
    ]),
    !ctrl.practice && !gbEdit.running(ctrl) ? renderNextChapter(ctrl) : null,
  ]);

function inputs(ctrl: AnalyseCtrl): VNode | undefined {
  if (ctrl.ongoing || !ctrl.data.userAnalysis) return;
  if (ctrl.redirecting) return spinner();
  return h('div.copyables', [
    h('div.pair', [
      h('label.name', 'FEN'),
      h('input.copyable.autoselect.analyse__underboard__fen', {
        attrs: { spellCheck: false },
        hook: {
          insert: vnode => {
            const el = vnode.elm as HTMLInputElement;
            el.value = defined(ctrl.fenInput) ? ctrl.fenInput : ctrl.node.fen;
            el.addEventListener('change', _ => {
              if (el.value !== ctrl.node.fen && el.reportValidity()) ctrl.changeFen(el.value.trim());
            });
            el.addEventListener('input', _ => {
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
        h('textarea.copyable.autoselect', {
          attrs: { spellCheck: false },
          hook: {
            ...onInsert(el => {
              (el as HTMLTextAreaElement).value = defined(ctrl.pgnInput)
                ? ctrl.pgnInput
                : pgnExport.renderFullTxt(ctrl);
              el.addEventListener('input', e => (ctrl.pgnInput = (e.target as HTMLTextAreaElement).value));
            }),
            postpatch: (_, vnode) => {
              (vnode.elm as HTMLTextAreaElement).value = defined(ctrl.pgnInput)
                ? ctrl.pgnInput
                : pgnExport.renderFullTxt(ctrl);
            },
          },
        }),
        h(
          'button.button.button-thin.action.text',
          {
            attrs: dataIcon(''),
            hook: bind(
              'click',
              _ => {
                const pgn = $('.copyables .pgn textarea').val() as string;
                if (pgn !== pgnExport.renderFullTxt(ctrl)) ctrl.changePgn(pgn);
              },
              ctrl.redraw
            ),
          },
          ctrl.trans.noarg('importPgn')
        ),
      ]),
    ]),
  ]);
}

const jumpButton = (icon: string, effect: string, enabled: boolean): VNode =>
  h('button.fbt', {
    class: { disabled: !enabled },
    attrs: { 'data-act': effect, 'data-icon': icon },
  });

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

function controls(ctrl: AnalyseCtrl) {
  const canJumpPrev = ctrl.path !== '',
    canJumpNext = !!ctrl.node.children[0],
    menuIsOpen = ctrl.actionMenu.open,
    noarg = ctrl.trans.noarg;
  return h(
    'div.analyse__controls.analyse-controls',
    {
      hook: onInsert(el => {
        bindMobileMousedown(
          el,
          e => {
            const action = dataAct(e);
            if (action === 'prev' || action === 'next') repeater(ctrl, action, e);
            else if (action === 'first') control.first(ctrl);
            else if (action === 'last') control.last(ctrl);
            else if (action === 'explorer') ctrl.toggleExplorer();
            else if (action === 'practice') ctrl.togglePractice();
            else if (action === 'menu') ctrl.actionMenu.toggle();
            else if (action === 'analysis' && ctrl.studyPractice)
              window.open(ctrl.studyPractice.analysisUrl(), '_blank', 'noopener');
          },
          ctrl.redraw
        );
      }),
    },
    [
      ctrl.embed
        ? null
        : h(
            'div.features',
            ctrl.studyPractice
              ? [
                  h('button.fbt', {
                    attrs: {
                      title: noarg('analysis'),
                      'data-act': 'analysis',
                      'data-icon': '',
                    },
                  }),
                ]
              : [
                  h('button.fbt', {
                    attrs: {
                      title: noarg('openingExplorerAndTablebase'),
                      'data-act': 'explorer',
                      'data-icon': '',
                    },
                    class: {
                      hidden: menuIsOpen || !ctrl.explorer.allowed() || !!ctrl.retro,
                      active: ctrl.explorer.enabled(),
                    },
                  }),
                  ctrl.ceval.possible && ctrl.ceval.allowed() && !ctrl.isGamebook()
                    ? h('button.fbt', {
                        attrs: {
                          title: noarg('practiceWithComputer'),
                          'data-act': 'practice',
                          'data-icon': '',
                        },
                        class: {
                          hidden: menuIsOpen || !!ctrl.retro,
                          active: !!ctrl.practice,
                        },
                      })
                    : null,
                ]
          ),
      h('div.jumps', [
        jumpButton('', 'first', canJumpPrev),
        jumpButton('', 'prev', canJumpPrev),
        jumpButton('', 'next', canJumpNext),
        jumpButton('', 'last', canJumpNext),
      ]),
      ctrl.studyPractice
        ? h('div.noop')
        : h('button.fbt', {
            class: { active: menuIsOpen },
            attrs: {
              title: noarg('menu'),
              'data-act': 'menu',
              'data-icon': '',
            },
          }),
    ]
  );
}

function forceInnerCoords(ctrl: AnalyseCtrl, v: boolean) {
  if (ctrl.data.pref.coords === Prefs.Coords.Outside) {
    $('body').toggleClass('coords-in', v).toggleClass('coords-out', !v);
  }
}

const addChapterId = (study: StudyCtrl | undefined, cssClass: string) =>
  cssClass + (study && study.data.chapter ? '.' + study.data.chapter.id : '');

const analysisDisabled = (ctrl: AnalyseCtrl): MaybeVNode =>
  ctrl.ceval.possible && ctrl.ceval.allowed()
    ? h('div.comp-off__hint', [
        h('span', ctrl.trans.noarg('computerAnalysisDisabled')),
        h(
          'button',
          {
            hook: bind('click', ctrl.toggleComputer, ctrl.redraw),
            attrs: { type: 'button' },
          },
          ctrl.trans.noarg('enable')
        ),
      ])
    : undefined;

export function renderMaterialDiffs(ctrl: AnalyseCtrl): [VNode, VNode] {
  const cgState = ctrl.chessground?.state,
    pieces = cgState ? cgState.pieces : readFen(ctrl.node.fen);

  return materialView.renderMaterialDiffs(
    !!ctrl.data.pref.showCaptured,
    ctrl.bottomColor(),
    pieces,
    !!(ctrl.data.player.checks || ctrl.data.opponent.checks), // showChecks
    ctrl.nodeList,
    ctrl.node.ply
  );
}

const renderPlayerStrip = (cls: string, materialDiff: VNode, clock?: VNode): VNode =>
  h('div.analyse__player_strip.' + cls, [materialDiff, clock]);

function renderPlayerStrips(ctrl: AnalyseCtrl): [VNode, VNode] | undefined {
  if (ctrl.embed) return;

  const clocks = renderClocks(ctrl),
    whitePov = ctrl.bottomIsWhite(),
    materialDiffs = renderMaterialDiffs(ctrl);

  return [
    renderPlayerStrip('top', materialDiffs[0], clocks?.[whitePov ? 1 : 0]),
    renderPlayerStrip('bottom', materialDiffs[1], clocks?.[whitePov ? 0 : 1]),
  ];
}

export default function (ctrl: AnalyseCtrl): VNode {
  if (ctrl.nvui) return ctrl.nvui.render(ctrl);
  const concealOf = makeConcealOf(ctrl),
    study = ctrl.study,
    showCevalPvs = !(ctrl.retro && ctrl.retro.isSolving()) && !ctrl.practice,
    menuIsOpen = ctrl.actionMenu.open,
    gamebookPlay = ctrl.gamebookPlay(),
    gamebookPlayView = gamebookPlay && gbPlay.render(gamebookPlay),
    gamebookEditView = gbEdit.running(ctrl) ? gbEdit.render(ctrl) : undefined,
    playerBars = renderPlayerBars(ctrl),
    playerStrips = !playerBars && renderPlayerStrips(ctrl),
    gaugeOn = ctrl.showEvalGauge(),
    needsInnerCoords = !!gaugeOn || !!playerBars,
    tour = relayTour(ctrl);

  return h(
    'main.analyse.variant-' + ctrl.data.game.variant.key,
    {
      hook: {
        insert: vn => {
          const elm = vn.elm as HTMLElement;
          forceInnerCoords(ctrl, needsInnerCoords);
          if (!!playerBars != $('body').hasClass('header-margin')) {
            requestAnimationFrame(() => {
              $('body').toggleClass('header-margin', !!playerBars);
              ctrl.redraw();
            });
          }
          if (ctrl.opts.chat) {
            const chatEl = document.createElement('section');
            chatEl.classList.add('mchat');
            elm.appendChild(chatEl);
            const chatOpts = ctrl.opts.chat;
            chatOpts.instance?.then(c => c.destroy());
            chatOpts.parseMoves = true;
            chatOpts.instance = lichess.makeChat(chatOpts);
          }
          gridHacks.start(elm);
        },
        update(_, _2) {
          forceInnerCoords(ctrl, needsInnerCoords);
        },
        postpatch(old, vnode) {
          if (old.data!.gaugeOn !== gaugeOn) document.body.dispatchEvent(new Event('chessground.resize'));
          vnode.data!.gaugeOn = gaugeOn;
        },
      },
      class: {
        'comp-off': !ctrl.showComputer(),
        'gauge-on': gaugeOn,
        'has-players': !!playerBars,
        'gamebook-play': !!gamebookPlayView,
        'has-relay-tour': !!tour,
        'analyse-hunter': ctrl.opts.hunter,
        'analyse--wiki': !!ctrl.wiki && !ctrl.study,
      },
    },
    [
      ctrl.keyboardHelp ? keyboardView(ctrl) : null,
      study ? studyView.overboard(study) : null,
      tour ||
        h(
          addChapterId(study, 'div.analyse__board.main-board'),
          {
            hook:
              'ontouchstart' in window || lichess.storage.get('scrollMoves') == '0'
                ? undefined
                : bindNonPassive(
                    'wheel',
                    stepwiseScroll((e: WheelEvent, scroll: boolean) => {
                      if (ctrl.gamebookPlay()) return;
                      const target = e.target as HTMLElement;
                      if (target.tagName !== 'PIECE' && target.tagName !== 'SQUARE' && target.tagName !== 'CG-BOARD')
                        return;
                      e.preventDefault();
                      if (e.deltaY > 0 && scroll) control.next(ctrl);
                      else if (e.deltaY < 0 && scroll) control.prev(ctrl);
                      ctrl.redraw();
                    })
                  ),
          },
          [
            ...(playerStrips || []),
            playerBars ? playerBars[ctrl.bottomIsWhite() ? 1 : 0] : null,
            chessground.render(ctrl),
            playerBars ? playerBars[ctrl.bottomIsWhite() ? 0 : 1] : null,
            ctrl.promotion.view(ctrl.data.game.variant.key === 'antichess'),
          ]
        ),
      gaugeOn && !tour ? cevalView.renderGauge(ctrl) : null,
      menuIsOpen || tour ? null : crazyView(ctrl, ctrl.topColor(), 'top'),
      gamebookPlayView ||
        (tour
          ? null
          : h(addChapterId(study, 'div.analyse__tools'), [
              ...(menuIsOpen
                ? [actionMenu(ctrl)]
                : [
                    ctrl.showComputer() ? cevalView.renderCeval(ctrl) : analysisDisabled(ctrl),
                    showCevalPvs ? cevalView.renderPvs(ctrl) : null,
                    renderAnalyse(ctrl, concealOf),
                    gamebookEditView || forkView(ctrl, concealOf),
                    retroView(ctrl) || practiceView(ctrl) || explorerView(ctrl),
                  ]),
            ])),
      menuIsOpen || tour ? null : crazyView(ctrl, ctrl.bottomColor(), 'bottom'),
      gamebookPlayView || tour ? null : controls(ctrl),
      ctrl.embed || tour
        ? null
        : h(
            'div.analyse__underboard',
            {
              hook:
                ctrl.synthetic || playable(ctrl.data) ? undefined : onInsert(elm => serverSideUnderboard(elm, ctrl)),
            },
            study ? studyView.underboard(ctrl) : [inputs(ctrl)]
          ),
      tour ? null : acplView(ctrl),
      ctrl.embed
        ? null
        : ctrl.studyPractice
        ? studyPracticeView.side(study!)
        : h(
            'aside.analyse__side',
            {
              hook: onInsert(elm => {
                ctrl.opts.$side && ctrl.opts.$side.length && $(elm).replaceWith(ctrl.opts.$side);
                $(elm).append($('.context-streamers').clone().removeClass('none'));
              }),
            },
            ctrl.studyPractice
              ? [studyPracticeView.side(study!)]
              : study
              ? [studyView.side(study)]
              : [
                  ctrl.forecast ? forecastView(ctrl, ctrl.forecast) : null,
                  !ctrl.synthetic && playable(ctrl.data)
                    ? h(
                        'div.back-to-game',
                        h(
                          'a.button.button-empty.text',
                          {
                            attrs: {
                              href: router.game(ctrl.data, ctrl.data.player.color),
                              'data-icon': '',
                            },
                          },
                          ctrl.trans.noarg('backToGame')
                        )
                      )
                    : null,
                ]
          ),
      study && study.relay && relayManager(study.relay),
      ctrl.embed
        ? null
        : h('div.chat__members.none', {
            hook: onInsert(lichess.watchers),
          }),
    ]
  );
}
