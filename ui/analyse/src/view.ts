import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import * as chessground from './ground';
import { synthetic, bind, dataIcon, iconTag, spinner } from './util';
import { game, router, view as gameView } from 'game';
import { path as treePath } from 'tree';
import treeView from './treeView';
import * as control from './control';
import { view as actionMenu } from './actionMenu';
import { view as renderPromotion } from './promotion';
import renderClocks from './clocks';
import * as pgnExport from './pgnExport';
import forecastView from './forecast/forecastView';
import { view as cevalView } from 'ceval';
import crazyView from './crazy/crazyView';
import { view as keyboardView} from './keyboard';
import explorerView from './explorer/explorerView';
import retroView from './retrospect/retroView';
import practiceView from './practice/practiceView';
import * as studyView from './study/studyView';
import { view as forkView } from './fork'
import { render as acplView } from './acpl'
import AnalyseController from './ctrl';
import { ConcealOf } from './interfaces';

function renderResult(ctrl: AnalyseController): VNode[] {
  let result: string | undefined;
  if (ctrl.data.game.status.id >= 30) switch (ctrl.data.game.winner) {
    case 'white':
      result = '1-0';
      break;
    case 'black':
      result = '0-1';
      break;
    default:
      result = '½-½';
  }
  const tags: VNode[] = [];
  if (result) {
    tags.push(h('div.result', result));
    const winner = game.getPlayer(ctrl.data, ctrl.data.game.winner!);
    tags.push(h('div.status', [
      gameView.status(ctrl),
      winner ? ', ' + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
    ]));
  }
  return tags;
}

function makeConcealOf(ctrl: AnalyseController): ConcealOf | undefined {
  const conceal = (ctrl.study && ctrl.study.data.chapter.conceal !== null) ? {
    owner: ctrl.study.isChapterOwner(),
    ply: ctrl.study.data.chapter.conceal
  } : null;
  if (conceal) return function(isMainline: boolean) {
    return function(path: Tree.Path, node: Tree.Node) {
      if (!conceal || (isMainline && conceal.ply >= node.ply)) return null;
      if (treePath.contains(ctrl.path, path)) return null;
      return conceal.owner ? 'conceal' : 'hide';
    };
  };
}

function renderAnalyse(ctrl: AnalyseController, concealOf?: ConcealOf) {
  return h('div.areplay', [
    renderChapterName(ctrl),
    renderOpeningBox(ctrl),
    treeView(ctrl, concealOf),
  ].concat(renderResult(ctrl)));
}

function wheel(ctrl: AnalyseController, e: WheelEvent) {
  const target = e.target as HTMLElement;
  if (target.tagName !== 'PIECE' && target.tagName !== 'SQUARE' && !target.classList.contains('cg-board')) return;
  e.preventDefault();
  if (e.deltaY > 0) control.next(ctrl);
  else if (e.deltaY < 0) control.prev(ctrl);
  ctrl.redraw();
  return false;
}

function inputs(ctrl: AnalyseController): VNode | undefined {
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
    h('div.pgn', [
      h('label.name', 'PGN'),
      h('textarea.copyable.autoselect', {
        attrs: { spellCheck: false },
        hook: {
          postpatch: (_, vnode) => {
            (vnode.elm as HTMLInputElement).value = pgnExport.renderFullTxt(ctrl);
          }
        }
      }),
      h('div.action', [
        h('button.button.text', {
          attrs: dataIcon('G'),
          hook: bind('click', _ => {
            const pgn = $('.copyables .pgn textarea').val();
            if (pgn !== pgnExport.renderFullTxt(ctrl)) ctrl.changePgn(pgn);
          }, ctrl.redraw)
        }, 'Import PGN')
      ])
    ])
  ]);
}

function visualBoard(ctrl: AnalyseController) {
  return h('div.lichess_board_wrap', [
    ctrl.keyboardHelp ? keyboardView(ctrl) : null,
    ctrl.study ? studyView.overboard(ctrl.study) : null,
    h('div.lichess_board.' + ctrl.data.game.variant.key, {
      hook: bind('wheel', e => wheel(ctrl, e as WheelEvent))
    }, [
      chessground.render(ctrl),
      renderPromotion(ctrl)
    ]),
    cevalView.renderGauge(ctrl)
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


function navClick(ctrl: AnalyseController, action: 'prev' | 'next') {
  const repeat = function() {
    control[action](ctrl);
    ctrl.redraw();
    delay = Math.max(100, delay - delay / 15);
    timeout = setTimeout(repeat, delay);
  };
  let delay = 350;
  let timeout = setTimeout(repeat, 500);
  control[action](ctrl);
  document.addEventListener('mouseup', function() {
    clearTimeout(timeout);
  }, {once: true} as any);
}

function buttons(ctrl: AnalyseController) {
  const canJumpPrev = ctrl.path !== '';
  const canJumpNext = !!ctrl.node.children[0];
  const menuIsOpen = ctrl.actionMenu.open;
  return h('div.game_control', {
    hook: bind('mousedown', e => {
      const action = dataAct(e);
      if (action === 'prev' || action === 'next') navClick(ctrl, action);
      else if (action === 'first') control.first(ctrl);
      else if (action === 'last') control.last(ctrl);
      else if (action === 'explorer') ctrl.toggleExplorer();
      else if (action === 'practice') ctrl.togglePractice();
      else if (action === 'menu') ctrl.actionMenu.toggle();
    }, ctrl.redraw)
  }, [
    ctrl.embed ? null : h('div.features', ctrl.studyPractice ? [
      h('a.hint--bottom', {
        attrs: {
          'data-hint': ctrl.trans.noarg('analysisBoard'),
          target: '_blank',
          href: ctrl.studyPractice.analysisUrl()
        }
      }, [iconTag('A')])
    ] : [
      h('button.hint--bottom', {
        attrs: {
          'data-hint': ctrl.trans.noarg('openingExplorerAndTablebase'),
          'data-act': 'explorer'
        },
        class: {
          hidden: menuIsOpen || !ctrl.explorer.allowed() || !!ctrl.retro,
          active: ctrl.explorer.enabled()
        }
      }, [iconTag(']')]),
      ctrl.ceval.possible && ctrl.ceval.allowed() ? h('button.hint--bottom', {
        attrs: {
          'data-hint': 'Practice with computer',
          'data-act': 'practice'
        },
        class: {
          hidden: menuIsOpen || !!ctrl.retro,
          active: !!ctrl.practice
        }
      }, [iconTag('')]) : null
  ]),
    h('div.jumps', [
      jumpButton('W', 'first', canJumpPrev),
      jumpButton('Y', 'prev', canJumpPrev),
      jumpButton('X', 'next', canJumpNext),
      jumpButton('V', 'last', canJumpNext)
    ]),
    ctrl.studyPractice ? h('div.noop') : h('button.hint--bottom', {
      class: { active: menuIsOpen },
      attrs: {
        'data-hint': ctrl.trans.noarg('menu'),
        'data-act': 'menu'
      }
    }, [iconTag('[')])
    ]);
}

function renderOpeningBox(ctrl: AnalyseController) {
  let opening = ctrl.tree.getOpening(ctrl.nodeList);
  if (!opening && !ctrl.path) opening = ctrl.data.game.opening;
  if (opening) return h('div.opening_box', {
    attrs: { title: opening.eco + ' ' + opening.name }
  }, [
    h('strong', opening.eco),
    ' ' + opening.name
  ]);
}

function renderChapterName(ctrl: AnalyseController) {
  if (ctrl.embed && ctrl.study) return h('div.chapter_name', ctrl.study.currentChapter().name);
}

let firstRender = true;

export default function(ctrl: AnalyseController): VNode {
  const concealOf = makeConcealOf(ctrl);
  const showCevalPvs = !(ctrl.retro && ctrl.retro.isSolving()) && !ctrl.practice;
  const menuIsOpen = ctrl.actionMenu.open;
  const chapterId = ctrl.study ? ctrl.study.currentChapter().id : 'nostudy';
  return h('div.analyse.cg-512', [
    h('div.' + chapterId, {
      hook: {
        insert: _ => {
          if (firstRender) firstRender = false;
          else window.lichess.pubsub.emit('reset_zoom')();
        }
      },
      class: {
        'gauge_displayed': ctrl.showEvalGauge(),
        'no_computer': !ctrl.showComputer()
      }
    }, [
      h('div.lichess_game', {
        hook: {
          insert: _ => window.lichess.pubsub.emit('content_loaded')()
        }
      }, [
        visualBoard(ctrl),
        h('div.lichess_ground', [
          menuIsOpen ? null : renderClocks(ctrl),
          menuIsOpen ? null : crazyView(ctrl, ctrl.topColor(), 'top')
        ].concat(
          menuIsOpen ? [actionMenu(ctrl)] : [
            cevalView.renderCeval(ctrl),
            showCevalPvs ? cevalView.renderPvs(ctrl) : null,
            renderAnalyse(ctrl, concealOf),
            forkView(ctrl, concealOf),
            retroView(ctrl) || practiceView(ctrl) || explorerView(ctrl)
          ]).concat([
            menuIsOpen ? null : crazyView(ctrl, ctrl.bottomColor(), 'bottom'),
            buttons(ctrl)
          ]))
      ])
    ]),
    ctrl.embed ? null : h('div.underboard', {
      class: { no_computer: !ctrl.showComputer() }
    }, [
      h('div.center', ctrl.study ? studyView.underboard(ctrl) : [inputs(ctrl)]),
      h('div.right', [acplView(ctrl)])
    ]),
    ctrl.embed || synthetic(ctrl.data) ? null : h('div.analeft', [
      ctrl.forecast ? forecastView(ctrl) : null,
      game.playable(ctrl.data) ? h('div.back_to_game',
        h('a.button.text', {
          attrs: {
            href: ctrl.data.player.id ? router.player(ctrl.data) : router.game(ctrl.data),
            'data-icon': 'i'
          }
        }, ctrl.trans('backToGame'))
      ) : null
    ])
  ]);
};
