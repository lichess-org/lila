import { view as cevalView } from 'ceval';
import { parseFen } from 'chessops/fen';
import { defined } from 'common';
import * as licon from 'common/licon';
import { bind, bindNonPassive, MaybeVNode, onInsert, dataIcon } from 'common/snabbdom';
import { bindMobileMousedown, isMobile } from 'common/device';
import { playable } from 'game';
import * as router from 'game/router';
import * as materialView from 'game/view/material';
import statusView from 'game/view/status';
import { h, VNode, VNodeChildren } from 'snabbdom';
import { path as treePath } from 'tree';
import { render as trainingView } from './roundTraining';
import { view as actionMenu } from './actionMenu';
import renderClocks from './clocks';
import * as control from '../control';
import crazyView from '../crazy/crazyView';
import AnalyseCtrl from '../ctrl';
import explorerView from '../explorer/explorerView';
import forecastView from '../forecast/forecastView';
import { view as forkView } from '../fork';
import * as gridHacks from './gridHacks';
import * as chessground from '../ground';
import { ConcealOf } from '../interfaces';
import { view as keyboardView } from '../keyboard';
import * as pgnExport from '../pgnExport';
import retroView from '../retrospect/retroView';
import practiceView from '../practice/practiceView';
import serverSideUnderboard from '../serverSideUnderboard';
import { StudyCtrl } from '../study/interfaces';
import { render as renderTreeView } from '../treeView/treeView';
import { spinnerVdom as spinner } from 'common/spinner';
import { stepwiseScroll } from 'common/scroll';
import type * as studyDeps from '../study/studyDeps';
import { renderNextChapter } from '../study/nextChapter';
import * as Prefs from 'common/prefs';

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

function inputs(ctrl: AnalyseCtrl): VNode | undefined {
  if (ctrl.ongoing || !ctrl.data.userAnalysis) return;
  if (ctrl.redirecting) return spinner();
  return h('div.copyables', [
    h('div.pair', [
      h('label.name', 'FEN'),
      h('input.copyable.autoselect.analyse__underboard__fen', {
        attrs: {
          spellcheck: 'false',
          enterkeyhint: 'done',
        },
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
        h('textarea.copyable', {
          attrs: { spellcheck: 'false' },
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
        isMobile()
          ? null
          : h(
              'button.button.button-thin.action.text',
              {
                attrs: dataIcon(licon.PlayTriangle),
                hook: bind('click', _ => {
                  const pgn = $('.copyables .pgn textarea').val() as string;
                  if (pgn !== pgnExport.renderFullTxt(ctrl)) ctrl.changePgn(pgn, true);
                }),
              },
              ctrl.trans.noarg('importPgn'),
            ),
      ]),
    ]),
  ]);
}

function controls(ctrl: AnalyseCtrl) {
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
                attrs: {
                  title: noarg('analysis'),
                  'data-act': 'analysis',
                  'data-icon': licon.Microscope,
                },
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
              ctrl.ceval.possible && ctrl.ceval.allowed() && !ctrl.isGamebook()
                ? h('button.fbt', {
                    attrs: {
                      title: noarg('practiceWithComputer'),
                      'data-act': 'practice',
                      'data-icon': licon.Bullseye,
                    },
                    class: {
                      hidden: menuIsOpen || !!ctrl.retro,
                      active: !!ctrl.practice,
                    },
                  })
                : null,
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
            attrs: {
              title: noarg('menu'),
              'data-act': 'menu',
              'data-icon': licon.Hamburger,
            },
          }),
    ],
  );
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
          ctrl.trans.noarg('enable'),
        ),
      ])
    : undefined;

const renderPlayerStrip = (cls: string, materialDiff: VNode, clock?: VNode): VNode =>
  h('div.analyse__player_strip.' + cls, [materialDiff, clock]);

export const renderMaterialDiffs = (ctrl: AnalyseCtrl): [VNode, VNode] =>
  materialView.renderMaterialDiffs(
    !!ctrl.data.pref.showCaptured,
    ctrl.bottomColor(),
    ctrl.node.fen,
    !!(ctrl.data.player.checks || ctrl.data.opponent.checks), // showChecks
    ctrl.nodeList,
    ctrl.node.ply,
  );

function renderPlayerStrips(ctrl: AnalyseCtrl): [VNode, VNode] | undefined {
  const clocks = renderClocks(ctrl),
    whitePov = ctrl.bottomIsWhite(),
    materialDiffs = renderMaterialDiffs(ctrl);

  return [
    renderPlayerStrip('top', materialDiffs[0], clocks?.[whitePov ? 1 : 0]),
    renderPlayerStrip('bottom', materialDiffs[1], clocks?.[whitePov ? 0 : 1]),
  ];
}

export default function (deps?: typeof studyDeps) {
  function renderResult(ctrl: AnalyseCtrl): VNode[] {
    const render = (result: string, status: VNodeChildren) => [
      h('div.result', result),
      h('div.status', status),
    ];
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

  const renderAnalyse = (ctrl: AnalyseCtrl, concealOf?: ConcealOf) =>
    h('div.analyse__moves.areplay', [
      h(`div.areplay__v${ctrl.treeVersion}`, [renderTreeView(ctrl, concealOf), ...renderResult(ctrl)]),
      !ctrl.practice && !deps?.gbEdit.running(ctrl) ? renderNextChapter(ctrl) : null,
    ]);

  return function (ctrl: AnalyseCtrl): VNode {
    if (ctrl.nvui) return ctrl.nvui.render();
    const concealOf = makeConcealOf(ctrl),
      study = ctrl.study,
      showCevalPvs = !(ctrl.retro && ctrl.retro.isSolving()) && !ctrl.practice,
      menuIsOpen = ctrl.actionMenu(),
      gamebookPlay = ctrl.gamebookPlay(),
      gamebookPlayView = gamebookPlay && deps?.gbPlay.render(gamebookPlay),
      gamebookEditView = deps?.gbEdit.running(ctrl) ? deps?.gbEdit.render(ctrl) : undefined,
      playerBars = deps?.renderPlayerBars(ctrl),
      playerStrips = !playerBars && renderPlayerStrips(ctrl),
      gaugeOn = ctrl.showEvalGauge(),
      needsInnerCoords = ctrl.data.pref.showCaptured || !!gaugeOn || !!playerBars,
      tour = deps?.relayTour(ctrl);

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
        study ? deps?.studyView.overboard(study) : null,
        tour ||
          h(
            addChapterId(study, 'div.analyse__board.main-board'),
            {
              hook:
                'ontouchstart' in window || !lichess.storage.boolean('scrollMoves').getOrDefault(true)
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
              playerBars ? playerBars[ctrl.bottomIsWhite() ? 1 : 0] : null,
              chessground.render(ctrl),
              playerBars ? playerBars[ctrl.bottomIsWhite() ? 0 : 1] : null,
              ctrl.promotion.view(ctrl.data.game.variant.key === 'antichess'),
            ],
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
                      gamebookEditView,
                      forkView(ctrl, concealOf),
                      retroView(ctrl) || practiceView(ctrl) || explorerView(ctrl),
                    ]),
              ])),
        menuIsOpen || tour ? null : crazyView(ctrl, ctrl.bottomColor(), 'bottom'),
        gamebookPlayView || tour ? null : controls(ctrl),
        tour
          ? null
          : h(
              'div.analyse__underboard',
              {
                hook:
                  ctrl.synthetic || playable(ctrl.data)
                    ? undefined
                    : onInsert(elm => serverSideUnderboard(elm, ctrl)),
              },
              study ? deps?.studyView.underboard(ctrl) : [inputs(ctrl)],
            ),
        tour ? null : trainingView(ctrl),
        ctrl.studyPractice
          ? deps?.studyPracticeView.side(study!)
          : h(
              'aside.analyse__side',
              {
                hook: onInsert(elm => {
                  ctrl.opts.$side && ctrl.opts.$side.length && $(elm).replaceWith(ctrl.opts.$side);
                  $(elm).append($('.context-streamers').clone().removeClass('none'));
                }),
              },
              ctrl.studyPractice
                ? [deps?.studyPracticeView.side(study!)]
                : study
                ? [deps?.studyView.side(study)]
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
                                'data-icon': licon.Back,
                              },
                            },
                            ctrl.trans.noarg('backToGame'),
                          ),
                        )
                      : null,
                  ],
            ),
        study && study.relay && deps?.relayManager(study.relay),
        h('div.chat__members.none', {
          hook: onInsert(lichess.watchers),
        }),
      ],
    );
  };
}
