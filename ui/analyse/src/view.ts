import { view as cevalView } from 'ceval';
import { makeChat } from 'chat';
import { defined } from 'common/common';
import { bindMobileMousedown, hasTouchEvents } from 'common/mobile';
import { type MaybeVNode, bind, bindNonPassive, dataIcon, onInsert } from 'common/snabbdom';
import spinner from 'common/spinner';
import stepwiseScroll from 'common/wheel';
import { playable } from 'game';
import * as router from 'game/router';
import statusView from 'game/view/status';
import { i18n, i18nFormatCapitalized, i18nPluralSame } from 'i18n';
import { colorName } from 'shogi/color-name';
import { isHandicap } from 'shogiops/handicaps';
import { parseSfen } from 'shogiops/sfen';
import { type VNode, h } from 'snabbdom';
import { path as treePath } from 'tree';
import { render as acplView } from './acpl';
import { view as actionMenu } from './action-menu';
import renderClocks from './clocks';
import * as control from './control';
import type AnalyseCtrl from './ctrl';
import forecastView from './forecast/forecast-view';
import { view as forkView } from './fork';
import * as gridHacks from './grid-hacks';
import * as shogiground from './ground';
import type { ConcealOf } from './interfaces';
import { view as keyboardView } from './keyboard';
import * as notationExport from './notation-export';
import practiceView from './practice/practice-view';
import retroView from './retrospect/retro-view';
import serverSideUnderboard from './server-side-underboard';
import { studyAdvancedButton, studyModal } from './study-modal';
import * as gbEdit from './study/gamebook/gamebook-edit';
import * as gbPlay from './study/gamebook/gamebook-play-view';
import type { StudyCtrl } from './study/interfaces';
import renderPlayerBars from './study/player-bars';
import * as studyPracticeView from './study/practice/study-practice-view';
import * as studyView from './study/study-view';
import { render as renderTreeView } from './tree-view/tree-view';

function renderResult(ctrl: AnalyseCtrl): MaybeVNode {
  const handicap = isHandicap({
    rules: ctrl.data.game.variant.key,
    sfen: ctrl.data.game.initialSfen,
  });
  const render = (status: string, winner?: Color) =>
    h('div.status', [
      status,
      winner ? `, ${i18nFormatCapitalized('xIsVictorious', colorName(winner, handicap))}` : null,
    ]);
  if (ctrl.data.game.status.id >= 30) {
    const status = statusView(ctrl.data.game.status, ctrl.data.game.winner, handicap);
    return render(status, ctrl.data.game.winner);
  } else if (ctrl.study?.data.chapter.setup.endStatus) {
    const status = statusView(
      ctrl.study.data.chapter.setup.endStatus.status,
      ctrl.study.data.chapter.setup.endStatus.winner,
      handicap,
    );
    return render(status, ctrl.study.data.chapter.setup.endStatus.winner);
  } else return null;
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

function renderAnalyse(ctrl: AnalyseCtrl, concealOf?: ConcealOf) {
  return h('div.analyse__moves.areplay', [
    ctrl.embed && ctrl.study ? h('div.chapter-name', ctrl.study.currentChapter().name) : null,
    renderTreeView(ctrl, concealOf),
    renderResult(ctrl),
  ]);
}

function inputs(ctrl: AnalyseCtrl): VNode | undefined {
  if (ctrl.ongoing || !ctrl.data.userAnalysis) return;
  if (ctrl.redirecting) return spinner();
  return h('div.copyables', [
    h('div.pair', [
      h('label.name', 'SFEN'),
      h('input.copyable.autoselect.analyse__underboard__sfen', {
        attrs: { spellCheck: false },
        hook: {
          insert: vnode => {
            const el = vnode.elm as HTMLInputElement;
            el.value = defined(ctrl.sfenInput) ? ctrl.sfenInput : ctrl.node.sfen;
            el.addEventListener('change', _ => {
              if (el.value !== ctrl.node.sfen && el.reportValidity())
                ctrl.changeSfen(el.value.trim());
            });
            el.addEventListener('input', _ => {
              ctrl.sfenInput = el.value;
              el.addEventListener('input', _ => {
                ctrl.sfenInput = el.value;
                const position = parseSfen(ctrl.data.game.variant.key, el.value.trim());
                el.setCustomValidity(position.isOk ? '' : 'Invalid SFEN');
              });
            });
          },
          postpatch: (_, vnode) => {
            const el = vnode.elm as HTMLInputElement;
            if (!defined(ctrl.sfenInput)) {
              el.value = ctrl.node.sfen;
              el.setCustomValidity('');
            } else if (el.value != ctrl.sfenInput) {
              el.value = ctrl.sfenInput;
            }
          },
        },
      }),
    ]),
    h('div.kif', [
      h('div.pair', [
        h('label.name', 'KIF'),
        h('textarea.copyable.autoselect', {
          attrs: { spellCheck: false },
          hook: {
            ...onInsert(el => {
              (el as HTMLTextAreaElement).value = defined(ctrl.kifInput)
                ? ctrl.kifInput
                : notationExport.renderFullKif(ctrl);
              el.addEventListener(
                'input',
                e => (ctrl.kifInput = (e.target as HTMLTextAreaElement).value),
              );
            }),
            postpatch: (_, vnode) => {
              (vnode.elm as HTMLTextAreaElement).value = defined(ctrl.kifInput)
                ? ctrl.kifInput
                : notationExport.renderFullKif(ctrl);
            },
          },
        }),
        h(
          'button.button.button-thin.action.text',
          {
            attrs: dataIcon('G'),
            hook: bind(
              'click',
              _ => {
                const kif = $('.copyables .kif textarea').val() as string;
                if (kif !== notationExport.renderFullKif(ctrl)) ctrl.changeNotation(kif);
              },
              ctrl.redraw,
            ),
          },
          i18n('importKif'),
        ),
      ]),
    ]),
    ['standard'].includes(ctrl.data.game.variant.key)
      ? h('div.csa', [
          h('div.pair', [
            h('label.name', 'CSA'),
            h('textarea.copyable.autoselect', {
              attrs: { spellCheck: false },
              hook: {
                ...onInsert(el => {
                  (el as HTMLTextAreaElement).value = defined(ctrl.csaInput)
                    ? ctrl.csaInput
                    : notationExport.renderFullCsa(ctrl);
                  el.addEventListener(
                    'input',
                    e => (ctrl.csaInput = (e.target as HTMLTextAreaElement).value),
                  );
                }),
                postpatch: (_, vnode) => {
                  (vnode.elm as HTMLTextAreaElement).value = defined(ctrl.csaInput)
                    ? ctrl.csaInput
                    : notationExport.renderFullCsa(ctrl);
                },
              },
            }),
            h(
              'button.button.button-thin.action.text',
              {
                attrs: dataIcon('G'),
                hook: bind(
                  'click',
                  _ => {
                    const csa = $('.copyables .csa textarea').val() as string;
                    if (csa !== notationExport.renderFullCsa(ctrl)) ctrl.changeNotation(csa);
                  },
                  ctrl.redraw,
                ),
              },
              i18n('importCsa'),
            ),
          ]),
        ])
      : null,
    h('div.url', [
      h('div.pair', [
        h('label.name', 'URL'),
        h('input.copyable.autoselect', {
          attrs: { spellCheck: false },
          hook: {
            ...onInsert(el => {
              (el as HTMLTextAreaElement).value = defined(ctrl.urlInput)
                ? ctrl.urlInput
                : notationExport.renderUrlUsiLine(ctrl);
              el.addEventListener(
                'input',
                e => (ctrl.urlInput = (e.target as HTMLTextAreaElement).value),
              );
            }),
            postpatch: (_, vnode) => {
              (vnode.elm as HTMLTextAreaElement).value = defined(ctrl.urlInput)
                ? ctrl.urlInput
                : notationExport.renderUrlUsiLine(ctrl);
            },
          },
        }),
      ]),
      h(
        'div.form-help.text',
        {
          attrs: { 'data-icon': '' },
        },
        [
          i18n('shareMainlineUrl'),
          ctrl.mainline.length > 300
            ? h('span.error', `MAX: ${i18nPluralSame('nbMoves', 300)}`)
            : null,
        ],
      ),
    ]),
  ]);
}

function jumpButton(icon: string, effect: string, enabled: boolean): VNode {
  return h('button.fbt', {
    class: { disabled: !enabled },
    attrs: { 'data-act': effect, 'data-icon': icon },
  });
}

function dataAct(e: Event): string | null {
  const target = e.target as HTMLElement;
  return (
    target.getAttribute('data-act') || (target.parentNode as HTMLElement).getAttribute('data-act')
  );
}

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
  document.addEventListener(eventName, () => clearTimeout(timeout), {
    once: true,
  });
}

function controls(ctrl: AnalyseCtrl) {
  const canJumpPrev = ctrl.path !== '',
    canJumpNext = !!ctrl.node.children[0],
    menuIsOpen = ctrl.actionMenu.open;
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
            else if (action === 'practice') ctrl.togglePractice();
            else if (action === 'menu') ctrl.actionMenu.toggle();
          },
          ctrl.redraw,
        );
      }),
    },
    [
      ctrl.embed || ctrl.forecast
        ? null
        : h(
            `div.features${!ctrl.synthetic ? '.from-game' : ''}`,
            ctrl.studyPractice
              ? [
                  h('a.fbt', {
                    attrs: {
                      title: i18n('analysis'),
                      target: '_blank',
                      href: ctrl.studyPractice.analysisUrl(),
                      'data-icon': 'A',
                    },
                  }),
                ]
              : [
                  !ctrl.synthetic ? studyAdvancedButton(ctrl, menuIsOpen) : null,
                  h('button.fbt', {
                    attrs: {
                      title: i18n('practiceWithComputer'),
                      'data-act': 'practice',
                      'data-icon': '',
                      hidden: !!ctrl.retro,
                      disabled:
                        menuIsOpen ||
                        !(ctrl.ceval.possible && ctrl.ceval.allowed() && !ctrl.isGamebook()),
                    },
                    class: {
                      active: !!ctrl.practice,
                    },
                  }),
                ],
          ),
      h('div.jumps', [
        !ctrl.embed ? jumpButton('W', 'first', canJumpPrev) : undefined,
        jumpButton('Y', 'prev', canJumpPrev),
        jumpButton('X', 'next', canJumpNext),
        !ctrl.embed ? jumpButton('V', 'last', canJumpNext) : undefined,
      ]),
      ctrl.studyPractice
        ? h('div.noop')
        : h('button.fbt', {
            class: { active: menuIsOpen },
            attrs: {
              title: i18n('menu'),
              'data-act': 'menu',
              'data-icon': '[',
            },
          }),
    ],
  );
}

function forceInnerCoords(ctrl: AnalyseCtrl, v: boolean) {
  if (ctrl.data.pref.coords == 2) {
    $('body').toggleClass('coords-in', v).toggleClass('coords-out', !v);
  }
}

function addChapterId(study: StudyCtrl | undefined, cssClass: string) {
  return cssClass + (study?.data.chapter ? `.${study.data.chapter.id}` : '');
}

export default function (ctrl: AnalyseCtrl): VNode {
  if (ctrl.nvui) return ctrl.nvui.render(ctrl);
  const concealOf = makeConcealOf(ctrl),
    study = ctrl.study,
    showCevalPvs = !ctrl.retro?.isSolving() && !ctrl.practice,
    menuIsOpen = ctrl.actionMenu.open,
    gamebookPlay = ctrl.gamebookPlay(),
    gamebookPlayView = gamebookPlay && gbPlay.render(gamebookPlay),
    gamebookEditView = gbEdit.running(ctrl) ? gbEdit.render(ctrl) : undefined,
    playerBars = renderPlayerBars(ctrl),
    clocks = !playerBars && renderClocks(ctrl, true),
    gaugeOn = ctrl.showEvalGauge(),
    needsInnerCoords = !!playerBars;
  return h(
    `main.sb-insert.analyse.main-v-${ctrl.data.game.variant.key}`, // sb-insert - to force snabbdom to call insert
    {
      hook: {
        insert: vn => {
          forceInnerCoords(ctrl, needsInnerCoords);
          if (!!playerBars != $('body').hasClass('header-margin')) {
            requestAnimationFrame(() => {
              $('body').toggleClass('header-margin', !!playerBars);
              ctrl.redraw();
            });
          }
          gridHacks.start(vn.elm as HTMLElement);
        },
        update(_, _2) {
          forceInnerCoords(ctrl, needsInnerCoords);
        },
        postpatch(old, vnode) {
          if (old.data!.gaugeOn !== gaugeOn)
            document.body.dispatchEvent(new Event('shogiground.resize'));
          vnode.data!.gaugeOn = gaugeOn;
        },
      },
      class: {
        'comp-off': !ctrl.showComputer(),
        'gauge-on': gaugeOn,
        'has-players': !!playerBars,
        'post-game': !!ctrl.study?.data.postGameStudy,
        'has-clocks': !!clocks,
      },
    },
    [
      ctrl.keyboardHelp ? keyboardView(ctrl) : null,
      ctrl.studyModal() ? studyModal(ctrl) : null,
      study ? studyView.overboard(study) : null,

      h(
        addChapterId(study, `div.analyse__board.main-board.v-${ctrl.data.game.variant.key}`),
        {
          hook:
            hasTouchEvents ||
            ctrl.gamebookPlay() ||
            window.lishogi.storage.get('scrollMoves') == '0'
              ? undefined
              : bindNonPassive(
                  'wheel',
                  stepwiseScroll((e: WheelEvent, scroll: boolean) => {
                    if (ctrl.gamebookPlay()) return;
                    const target = e.target as HTMLElement;
                    if (target.tagName !== 'SG-PIECES') return;
                    e.preventDefault();
                    if (e.deltaY > 0 && scroll) control.next(ctrl);
                    else if (e.deltaY < 0 && scroll) control.prev(ctrl);
                    ctrl.redraw();
                  }),
                ),
        },
        [
          ...(clocks || []),
          playerBars ? playerBars[ctrl.bottomIsSente() ? 1 : 0] : null,
          shogiground.renderBoard(ctrl),
          playerBars ? playerBars[ctrl.bottomIsSente() ? 0 : 1] : null,
        ],
      ),
      gaugeOn ? cevalView.renderGauge(ctrl) : null,
      gamebookPlayView ||
        h(addChapterId(study, 'div.analyse__tools'), [
          ...(menuIsOpen
            ? [actionMenu(ctrl)]
            : [
                cevalView.renderCeval(ctrl),
                showCevalPvs ? cevalView.renderPvs(ctrl) : null,
                renderAnalyse(ctrl, concealOf),
                gamebookEditView || forkView(ctrl, concealOf),
                retroView(ctrl) || practiceView(ctrl),
              ]),
        ]),
      gamebookPlayView ? null : controls(ctrl),
      ctrl.embed
        ? null
        : h(
            'div.analyse__underboard',
            {
              hook:
                ctrl.synthetic || playable(ctrl.data)
                  ? undefined
                  : onInsert(elm => serverSideUnderboard(elm, ctrl)),
            },
            study ? studyView.underboard(ctrl) : [inputs(ctrl)],
          ),
      acplView(ctrl),
      ctrl.embed
        ? null
        : ctrl.studyPractice
          ? studyPracticeView.side(study!)
          : h(
              'aside.analyse__side',
              {
                hook: onInsert(elm => {
                  ctrl.opts.$side?.length && $(elm).replaceWith(ctrl.opts.$side);
                  $(elm).append($('.streamers').clone().removeClass('none'));
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
                                  'data-icon': 'i',
                                },
                              },
                              i18n('backToGame'),
                            ),
                          )
                        : null,
                    ],
            ),
      ctrl.opts.chat &&
        h('section.mchat', {
          hook: onInsert(_ => {
            if (ctrl.opts.chat.instance) ctrl.opts.chat.instance.destroy();
            ctrl.opts.chat.parseMoves = true;
            ctrl.opts.chat.instance = makeChat(ctrl.opts.chat);
          }),
        }),
      ctrl.embed
        ? null
        : h(
            'div.chat__members.none',
            {
              hook: onInsert(el => $(el).watchers()),
            },
            [h('span.number', '\xa0'), ' ', i18n('spectators'), ' ', h('span.list')],
          ),
    ],
  );
}
