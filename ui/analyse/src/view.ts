import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import * as draughtsground from './ground';
import { synthetic, bind, onInsert, dataIcon, spinner } from './util';
import { getPlayer, playable } from 'game';
import * as router from 'game/router';
import statusView from 'game/view/status';
import { path as treePath } from 'tree';
import { render as renderTreeView } from './treeView/treeView';
import * as control from './control';
import { view as actionMenu } from './actionMenu';
//import renderClocks from './clocks';
import * as pdnExport from './pdnExport';
import forecastView from './forecast/forecastView';
import { view as cevalView } from 'ceval';
import crazyView from './crazy/crazyView';
import { view as keyboardView } from './keyboard';
import explorerView from './explorer/explorerView';
import retroView from './retrospect/retroView';
import practiceView from './practice/practiceView';
import * as gbEdit from './study/gamebook/gamebookEdit';
import * as gbPlay from './study/gamebook/gamebookPlayView';
import { StudyCtrl } from './study/interfaces';
import * as studyView from './study/studyView';
import { view as forkView } from './fork'
import { render as acplView } from './acpl'
import AnalyseCtrl from './ctrl';
import { ConcealOf } from './interfaces';
import relayManager from './study/relay/relayManagerView';
import relayIntro from './study/relay/relayIntroView';
import renderPlayerBars from './study/playerBars';
import underboard from './underboard';

const li = window.lidraughts;

function renderResult(ctrl: AnalyseCtrl): VNode[] {
  let result: string | undefined;
  if (ctrl.data.game.status.id >= 30) switch (ctrl.data.game.winner) {
    case 'white':
      result = ctrl.data.pref.draughtsResult ? '2-0' : '1-0';
      break;
    case 'black':
      result = ctrl.data.pref.draughtsResult ? '0-2' : '0-1';
      break;
    default:
      result = ctrl.data.pref.draughtsResult ? '1-1' : '½-½';
  }
  const tags: VNode[] = [];
  if (result) {
    tags.push(h('div.result', result));
    const winner = getPlayer(ctrl.data, ctrl.data.game.winner!),
      statusText = statusView(ctrl);
    tags.push(h('div.status', [
      statusText,
      winner ? (statusText.length !== 0 ? ', ' : '') + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
    ]));
  }
  return tags;
}

function makeConcealOf(ctrl: AnalyseCtrl): ConcealOf | undefined {
  const conceal = (ctrl.study && ctrl.study.data.chapter.conceal !== undefined) ? {
    owner: ctrl.study.isChapterOwner(),
    ply: ctrl.study.data.chapter.conceal
  } : null;
  if (conceal) return function (isMainline: boolean) {
    return function (path: Tree.Path, node: Tree.Node) {
      if (!conceal || (isMainline && conceal.ply >= node.ply)) return null;
      if (treePath.contains(ctrl.path, path)) return null;
      return conceal.owner ? 'conceal' : 'hide';
    };
  };
}

function renderAnalyse(ctrl: AnalyseCtrl, concealOf?: ConcealOf) {
  return h('div.analyse__moves.areplay', [
    renderChapterName(ctrl),
    renderOpeningBox(ctrl),
    renderTreeView(ctrl, concealOf),
  ].concat(renderResult(ctrl)));
}

function wheel(ctrl: AnalyseCtrl, e: WheelEvent) {
  const target = e.target as HTMLElement;
  if (target.tagName !== 'PIECE' && target.tagName !== 'SQUARE' && !target.classList.contains('cg-board')) return;
  e.preventDefault();
  if (e.deltaY > 0) control.next(ctrl);
  else if (e.deltaY < 0) control.prev(ctrl);
  ctrl.redraw();
  return false;
}

function inputs(ctrl: AnalyseCtrl): VNode | undefined {
  if (ctrl.ongoing || !ctrl.data.userAnalysis) return;
  if (ctrl.redirecting) return spinner();
  return h('div.copyables', [
    h('label.name', 'FEN'),
    h('input.copyable.autoselect', {
      attrs: {
        spellCheck: false,
        value: ctrl.node.fen
      },
      hook: bind('change', e => {
        const value = (e.target as HTMLInputElement).value;
        if (value !== ctrl.node.fen) ctrl.changeFen(value);
      })
    }),
    h('div.pdn', [
      h('label.name', 'PDN'),
      h('textarea.copyable.autoselect', {
        attrs: { spellCheck: false },
        hook: {
          postpatch: (_, vnode) => {
            (vnode.elm as HTMLInputElement).value = pdnExport.renderFullTxt(ctrl);
          }
        }
      }),
      h('div.action', [
        h('button.button.text', {
          attrs: dataIcon('G'),
          hook: bind('click', _ => {
            const pdn = $('.copyables .pdn textarea').val();
            if (pdn !== pdnExport.renderFullTxt(ctrl)) ctrl.changePdn(pdn);
          }, ctrl.redraw)
        }, ctrl.trans.noarg('importPdn'))
      ])
    ])
  ]);
}

function jumpButton(icon: string, effect: string, enabled: boolean): VNode {
  return h('button', {
    class: { disabled: !enabled },
    attrs: { 'data-act': effect, 'data-icon': icon }
  });
}

function dataAct(e: Event): string | null {
  const target = e.target as HTMLElement;
  return target.getAttribute('data-act') ||
    (target.parentNode as HTMLElement).getAttribute('data-act');
}


function navClick(ctrl: AnalyseCtrl, action: 'prev' | 'next') {
  const repeat = function () {
    control[action](ctrl);
    ctrl.redraw();
    delay = Math.max(100, delay - delay / 15);
    timeout = setTimeout(repeat, delay);
  };
  let delay = 350;
  let timeout = setTimeout(repeat, 500);
  control[action](ctrl);
  document.addEventListener('mouseup', function () {
    clearTimeout(timeout);
  }, { once: true } as any);
}

function controls(ctrl: AnalyseCtrl) {
  const canJumpPrev = ctrl.path !== '',
    canJumpNext = !!ctrl.node.children[0],
    menuIsOpen = ctrl.actionMenu.open,
    multiBoardMenu = ctrl.study && ctrl.study.relay && ctrl.study.members.canContribute() && ctrl.study.multiBoardMenu,
    noarg = ctrl.trans.noarg;
  return h('div.analyse__controls.analyse-controls', {
    hook: bind('mousedown', e => {
      const action = dataAct(e);
      if (action === 'prev' || action === 'next') navClick(ctrl, action);
      else if (action === 'first') control.first(ctrl);
      else if (action === 'last') control.last(ctrl);
      else if (action === 'explorer') ctrl.toggleExplorer();
      else if (action === 'practice') ctrl.togglePractice();
      else if (action === 'menu') {
        ctrl.actionMenu.toggle();
        if (ctrl.study && ctrl.study.multiBoardMenu)
          ctrl.study.multiBoardMenu.open = false;
      } else if (action === 'multiboard-menu') {
        ctrl.actionMenu.open = false;
        if (ctrl.study && ctrl.study.multiBoardMenu)
          ctrl.study.multiBoardMenu.toggle();
      }
    }, ctrl.redraw)
  }, [
      ctrl.embed ? null : h('div.features', ctrl.studyPractice ? [
        h('a', {
          attrs: {
            title: noarg('analysis'),
            target: '_blank',
            href: ctrl.studyPractice.analysisUrl(),
            'data-icon': 'A'
          }
        })
      ] : [
          /*h('button', {
            attrs: {
              title: noarg('openingExplorerAndTablebase'),
              'data-act': 'explorer',
              'data-icon': ']'
            },
            class: {
              hidden: menuIsOpen || !ctrl.explorer.allowed() || !!ctrl.retro,
              active: ctrl.explorer.enabled()
            }
          }), null*/
          /*ctrl.ceval.possible && ctrl.ceval.allowed() && !ctrl.isGamebook() ? h('button', {
            attrs: {
              title: noarg('practiceWithComputer'),
              'data-act': 'practice',
              'data-icon': ''
            },
            class: {
              hidden: menuIsOpen || !!ctrl.retro,
              active: !!ctrl.practice
            }
          }) : null*/
        ]),
      h('div.jumps', [
        jumpButton('W', 'first', canJumpPrev),
        jumpButton('Y', 'prev', canJumpPrev),
        jumpButton('X', 'next', canJumpNext),
        jumpButton('V', 'last', canJumpNext)
      ]),
      h('div.buttons', [
        multiBoardMenu ? h('button', {
          class: { active: multiBoardMenu.open },
          attrs: {
            title: 'Multiboard',
            'data-act': 'multiboard-menu',
            'data-icon': ''
          }
        }) : null,
        ctrl.studyPractice ? h('div.noop') : h('button', {
          class: { active: menuIsOpen },
          attrs: {
            title: noarg('menu'),
            'data-act': 'menu',
            'data-icon': '['
          }
        })
      ])
    ]);
}

function renderOpeningBox(ctrl: AnalyseCtrl) {
  let opening = ctrl.tree.getOpening(ctrl.nodeList);
  if (!opening && !ctrl.path) opening = ctrl.data.game.opening;
  if (opening) return h('div.opening_box', {
    attrs: { title: opening.eco + ' ' + opening.name }
  }, [
      h('strong', opening.eco),
      ' ' + opening.name
    ]);
}

function renderChapterName(ctrl: AnalyseCtrl) {
  if (ctrl.embed && ctrl.study) return h('div.chapter_name', ctrl.study.currentChapter().name);
}

const innerCoordsCss = 'stylesheets/board.coords.inner.css';

function forceInnerCoords(ctrl: AnalyseCtrl, v: boolean) {
  const pref = ctrl.data.pref.coords;
  if (!pref) return;
  if (v) li.loadCss(innerCoordsCss);
  else if (pref === 2) li.unloadCss(innerCoordsCss);
}

let firstRender = true;

function addChapterId(study: StudyCtrl | undefined, cssClass: string) {
  return cssClass + ((study && study.data.chapter) ? '.' + study.data.chapter.id + study.vm.loading : '.nostudy');
}

export default function(ctrl: AnalyseCtrl): VNode {
  if (ctrl.nvui) return ctrl.nvui.render(ctrl);
  const concealOf = makeConcealOf(ctrl),
    study = ctrl.study,
    showCevalPvs = !(ctrl.retro && ctrl.retro.isSolving()) && !ctrl.practice,
    menuIsOpen = ctrl.actionMenu.open,
    multiBoardMenu = ctrl.study && ctrl.study.multiBoardMenu,
    multiBoardMenuIsOpen = multiBoardMenu && multiBoardMenu.open && ctrl.study && ctrl.study.members.canContribute(),
    gamebookPlay = ctrl.gamebookPlay(),
    gamebookPlayView = gamebookPlay && gbPlay.render(gamebookPlay),
    gamebookEditView = gbEdit.running(ctrl) ? gbEdit.render(ctrl) : undefined,
    relayEdit = study && study.relay && relayManager(study.relay),
    playerBars = renderPlayerBars(ctrl),
    gaugeOn = ctrl.showEvalGauge(),
    needsInnerCoords = !!gaugeOn || !!playerBars,
    intro = relayIntro(ctrl);
  return h(addChapterId(study, 'main.analyse'), {
    hook: {
      insert: _ => {
        if (firstRender) {
          firstRender = false;
          if (ctrl.data.pref.coords === 1) li.loadedCss[innerCoordsCss] = true;
        }
        else li.pubsub.emit('reset_zoom')();
        forceInnerCoords(ctrl, needsInnerCoords);
      },
      update(_, _2) {
        forceInnerCoords(ctrl, needsInnerCoords);
      },
      postpatch(old, vnode) {
        if (old.data!.gaugeOn !== gaugeOn) {
          window.lidraughts.dispatchEvent(document.body, 'draughtsground.resize');
        }
        vnode.data!.gaugeOn = gaugeOn;
      }
    },
    class: {
      'gauge-on': gaugeOn,
      'gb_edit': !!gamebookEditView,
      'gb_play': !!gamebookPlayView,
      'relay_edit': !!relayEdit,
      'player_bars': !!playerBars,
    }
  }, [
    (gaugeOn && !intro) ? cevalView.renderGauge(ctrl) : null,
    ctrl.keyboardHelp ? keyboardView(ctrl) : null,
    ctrl.study ? studyView.overboard(ctrl.study) : null,
    playerBars ? playerBars[ctrl.bottomIsWhite() ? 1 : 0] : null,
    intro || h(addChapterId(study, 'div.analyse__board.main-board.' + ctrl.data.game.variant.key + '.' + ctrl.bottomColor()), {
      hook: ctrl.gamebookPlay() ? undefined : bind('wheel', e => wheel(ctrl, e as WheelEvent))
    }, [
      draughtsground.render(ctrl)
    ]),
    playerBars ? playerBars[ctrl.bottomIsWhite() ? 0 : 1] : null,
    h('div.analyse__tools', gamebookPlayView || [
      // (menuIsOpen || multiBoardMenuIsOpen || playerBars) ? null : renderClocks(ctrl),
      (menuIsOpen || multiBoardMenuIsOpen || intro) ? null : crazyView(ctrl, ctrl.topColor(), 'top'),
      ...(menuIsOpen ? [actionMenu(ctrl)] : (
        (multiBoardMenu && multiBoardMenuIsOpen) ? [multiBoardMenu.view(ctrl.study)] : [
          cevalView.renderCeval(ctrl),
          showCevalPvs ? cevalView.renderPvs(ctrl) : null,
          renderAnalyse(ctrl, concealOf),
          gamebookEditView ? null : forkView(ctrl, concealOf),
          retroView(ctrl) || practiceView(ctrl) || explorerView(ctrl)
        ])),
      (menuIsOpen || multiBoardMenuIsOpen || intro) ? null : crazyView(ctrl, ctrl.bottomColor(), 'bottom'),
      gamebookEditView || relayEdit
    ]),
    controls(ctrl),
    (ctrl.embed || intro) ? null : h('div.analyse__underboard', {
      class: { 'comp-off': !ctrl.showComputer() },
      hook: onInsert(elm => underboard(elm, ctrl))
    }, ctrl.study ? studyView.underboard(ctrl) : [inputs(ctrl)]),
    intro ? null : h('div.analyse__acpl', [acplView(ctrl)]),
    ctrl.embed || synthetic(ctrl.data) ? null : h('aside.analyse__side', {
      hook: onInsert(elm => {
        ctrl.opts.$side.length && $(elm).replaceWith(ctrl.opts.$side);
      })
    }, [
      ctrl.forecast ? forecastView(ctrl, ctrl.forecast) : null,
      playable(ctrl.data) ? h('div.back_to_game',
        h('a.button.text', {
          attrs: {
            href: ctrl.data.player.id ? router.player(ctrl.data) : router.game(ctrl.data),
            'data-icon': 'i'
          }
        }, ctrl.trans.noarg('backToGame'))
      ) : null
    ]),
    ctrl.opts.chat && h('section.mchat', {
      hook: onInsert(elm => {
        ctrl.opts.chat.parseMoves = true;
        window.lidraughts.makeChat(elm, ctrl.opts.chat);
      })
    }),
    h('div.analyse__underchat', {
      hook: onInsert(elm => {
        $(elm).replaceWith($('.analyse__underchat.none').removeClass('none'));
      })
    })
  ]);
}
