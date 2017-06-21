import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { AnalyseController, MaybeVNodes } from './interfaces';
import * as chessground from './ground';
import { synthetic, bind, dataIcon, iconTag, spinner } from './util';
import { game, router, view as gameView } from 'game';
import { path as treePath } from 'tree';
import treeView = require('./treeView');
import control = require('./control');
import { view as actionMenu } from './actionMenu';
import { view as renderPromotion } from './promotion';
import renderClocks = require('./clocks');
import pgnExport = require('./pgnExport');
import forecastView = require('./forecast/forecastView');
import { view as cevalView } from 'ceval';
import crazyView from './crazy/crazyView';
import { view as keyboardView} from './keyboard';
import explorerView from './explorer/explorerView';
import retroView = require('./retrospect/retroView');
import practiceView = require('./practice/practiceView');
import studyView = require('./study/studyView');
import { view as forkView } from './fork'
import { render as acplView } from './acpl'

function renderResult(ctrl) {
  let result;
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
  if (result) {
    const tags: VNode[] = [];
    tags.push(h('div.result', result));
    const winner = game.getPlayer(ctrl.data, ctrl.data.game.winner);
    tags.push(h('div.status', [
      gameView.status(ctrl),
      winner ? ', ' + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
    ]));
    return tags;
  }
}

function makeConcealOf(ctrl) {
  const conceal = (ctrl.study && ctrl.study.data.chapter.conceal !== null) ? {
    owner: ctrl.study.isChapterOwner(),
    ply: ctrl.study.data.chapter.conceal
  } : null;
  if (conceal) return function(isMainline) {
    return function(path, node) {
      if (!conceal || (isMainline && conceal.ply >= node.ply)) return null;
      if (treePath.contains(ctrl.vm.path, path)) return null;
      return conceal.owner ? 'conceal' : 'hide';
    };
  };
}

function renderAnalyse(ctrl, concealOf) {
  return h('div.areplay', [
    renderChapterName(ctrl),
    renderOpeningBox(ctrl),
    treeView.render(ctrl, concealOf),
    renderResult(ctrl)
  ]);
}

function wheel(ctrl, e) {
  if (e.target.tagName !== 'PIECE' && e.target.tagName !== 'SQUARE' && !e.target.classList.contains('cg-board')) return;
  e.preventDefault();
  if (e.deltaY > 0) control.next(ctrl);
  else if (e.deltaY < 0) control.prev(ctrl);
  ctrl.redraw();
  return false;
}

function inputs(ctrl) {
  if (ctrl.ongoing || !ctrl.data.userAnalysis) return null;
  if (ctrl.vm.redirecting) return spinner();
  const pgnText = pgnExport.renderFullTxt(ctrl);
  return h('div.copyables', [
    h('label.name', 'FEN'),
    h('input.copyable.autoselect', {
      attrs: {
        spellCheck: false,
        value: ctrl.vm.node.fen
      },
      hook: bind('change', e => {
        const value = (e.target as HTMLInputElement).value;
        if (value !== ctrl.vm.node.fen) ctrl.changeFen(value);
      })
    }),
    h('div.pgn', [
      h('label.name', 'PGN'),
      h('textarea.copyable.autoselect', {
        attrs: {
          spellCheck: false,
          value: pgnText
        }
      }),
      h('div.action', [
        h('button.button.text', {
          attrs: dataIcon('G'),
          hook: bind('click', _ => {
            const pgn = $('.copyables .pgn textarea').val();
            if (pgn !== pgnText) ctrl.changePgn(pgn);
          })
        }, 'Import PGN')
      ])
    ])
  ]);
}

function visualBoard(ctrl) {
  return h('div.lichess_board_wrap', [
    ctrl.vm.keyboardHelp ? keyboardView(ctrl) : null,
    ctrl.study ? studyView.overboard(ctrl.study) : null,
    h('div.lichess_board.' + ctrl.data.game.variant.key, {
      hook: bind('wheel', e => wheel(ctrl, e))
    }, [
      chessground.render(ctrl),
      renderPromotion(ctrl)
    ]),
    cevalView.renderGauge(ctrl)
  ]);
}

function jumpButton(icon, effect, enabled) {
  return {
    tag: 'button',
    attrs: {
      'data-act': effect,
      'data-icon': icon,
      class: enabled ? '' : 'disabled'
    }
  };
}

function dataAct(e) {
  return e.target.getAttribute('data-act') ||
  e.target.parentNode.getAttribute('data-act');
}


function navClick(ctrl: AnalyseController, action) {
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

function buttons(ctrl) {
  const canJumpPrev = ctrl.vm.path !== '';
  const canJumpNext = !!ctrl.vm.node.children[0];
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
    })
  }, [
    ctrl.embed ? null : h('div.features', ctrl.studyPractice ? [
      h('a.hint--bottom', {
        attrs: {
          'data-hint': 'Analysis board',
          target: '_blank',
          href: ctrl.studyPractice.analysisUrl()
        }
      }, iconTag('A'))
    ] : [
      h('button.hint--bottom', {
        attrs: {
          'data-hint': ctrl.trans('openingExplorer'),
          'data-act': 'explorer'
        },
        class: {
          hidden: menuIsOpen || !ctrl.explorer.allowed() || ctrl.retro,
          active: ctrl.explorer.enabled()
        }
      }, iconTag(']')),
      ctrl.ceval.possible ? h('button.hint--bottom', {
        attrs: {
          'data-hint': 'Practice with computer',
          'data-act': 'practice'
        },
        class: {
          hidden: menuIsOpen || ctrl.retro,
          active: ctrl.practice
        }
      }, iconTag('')) : null
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
        'data-hint': 'Menu',
        'data-act': 'menu'
      }
    }, iconTag('['))
    ]);
}

function renderOpeningBox(ctrl) {
  let opening = ctrl.tree.getOpening(ctrl.vm.nodeList);
  if (!opening && !ctrl.vm.path) opening = ctrl.data.game.opening;
  if (opening) return h('div.opening_box', {
    attrs: { title: opening.eco + ' ' + opening.name }
  }, [
    h('strong', opening.eco),
    h('span', ' ' + opening.name)
  ]);
}

function renderChapterName(ctrl) {
  if (ctrl.embed && ctrl.study) return h('div.chapter_name', ctrl.study.currentChapter().name);
}

let firstRender = true;

export default function(ctrl: AnalyseController): MaybeVNodes {
  const concealOf = makeConcealOf(ctrl);
  const showCevalPvs = !(ctrl.retro && ctrl.retro.isSolving()) && !ctrl.practice;
  const menuIsOpen = ctrl.actionMenu.open;
  return [
    h('div', {
      hook: {
        insert: _ => {
        if (firstRender) firstRender = false;
        else window.lichess.pubsub.emit('reset_zoom')();
        }
      },
      class: {
        'gauge_displayed': ctrl.showEvalGauge(),
        'no_computer': !ctrl.vm.showComputer()
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
          menuIsOpen ? null : crazyView(ctrl, ctrl.topColor(), 'top'),
          menuIsOpen ? actionMenu(ctrl) : [
            cevalView.renderCeval(ctrl),
            showCevalPvs ? cevalView.renderPvs(ctrl) : null,
            renderAnalyse(ctrl, concealOf),
            forkView(ctrl, concealOf),
            retroView(ctrl) || practiceView(ctrl) || explorerView(ctrl)
          ],
          menuIsOpen ? null : crazyView(ctrl, ctrl.bottomColor(), 'bottom'),
          buttons(ctrl)
        ])
      ])
    ]),
    ctrl.embed ? null : h('div.underboard', {
      class: { no_computer: !ctrl.vm.showComputer() }
    }, [
      h('div.center', ctrl.study ? studyView.underboard(ctrl) : inputs(ctrl)),
      h('div.right', acplView(ctrl))
    ]),
    ctrl.embed || synthetic(ctrl.data) ? null : h('div.analeft', [
      ctrl.forecast ? forecastView(ctrl) : null,
      game.playable(ctrl.data) ? h('div.back_to_game',
        m('a', {
          class: 'button text',
          href: ctrl.data.player.id ? router.player(ctrl.data) : router.game(ctrl.data),
          'data-icon': 'i'
        }, ctrl.trans('backToGame'))
      ) : null
    ])
  ];
};
