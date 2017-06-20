import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import * as chessground from './ground';
import { synthetic } from './util';
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
  var result;
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
    var tags = [];
    tags.push(m('div.result', result));
    var winner = game.getPlayer(ctrl.data, ctrl.data.game.winner);
    tags.push(m('div.status', [
      gameView.viewStatus(ctrl),
      winner ? ', ' + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
    ]));
    return tags;
  }
}

function makeConcealOf(ctrl) {
  var conceal = (ctrl.study && ctrl.study.data.chapter.conceal !== null) ? {
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
  return m('div.areplay', [
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
  m.redraw();
  return false;
}

function inputs(ctrl) {
  if (ctrl.ongoing || !ctrl.data.userAnalysis) return null;
  if (ctrl.vm.redirecting) return m.trust(lichess.spinnerHtml);
  var pgnText = pgnExport.renderFullTxt(ctrl);
  return m('div.copyables', [
    m('label.name', 'FEN'),
    m('input.copyable.autoselect[spellCheck=false]', {
      value: ctrl.vm.node.fen,
      config: bindOnce('change', function(e) {
        if (e.target.value !== ctrl.vm.node.fen) ctrl.changeFen(e.target.value);
      })
    }),
    m('div.pgn', [
      m('label.name', 'PGN'),
      m('textarea.copyable.autoselect[spellCheck=false]', {
        value: pgnText
      }),
      m('div.action', [
        m('button', {
          class: 'button text',
          'data-icon': 'G',
          config: bindOnce('click', function(e) {
            var pgn = $('.copyables .pgn textarea').val();
            if (pgn !== pgnText) ctrl.changePgn(pgn);
          })
        }, 'Import PGN')
      ])
    ])
  ]);
}

function visualBoard(ctrl) {
  return m('div.lichess_board_wrap', [
    ctrl.vm.keyboardHelp ? keyboardView(ctrl) : null,
    ctrl.study ? studyView.overboard(ctrl.study) : null,
    m('div', {
      class: 'lichess_board ' + ctrl.data.game.variant.key + ((ctrl.study && ctrl.data.pref.blindfold) ? ' blindfold' : ''),
      config: function(el, isUpdate) {
        if (!isUpdate) el.addEventListener('wheel', function(e) {
          return wheel(ctrl, e);
        });
      }
    }, [
      chessground.render(ctrl),
      renderPromotion(ctrl)
    ]),
    cevalView.renderGauge(ctrl)
  ]);
}

function blindBoard(ctrl) {
  return m('div.lichess_board_blind', [
    m('div.textual', {
      config: function(el, isUpdate) {
        if (isUpdate) return;
        var url = ctrl.data.player.spectator ?
          router.game(ctrl.data, ctrl.data.player.color) :
          router.player(ctrl.data);
        url += '/text';
        $(el).load(url);
      }
    }),
      chessground.render(ctrl)
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

function icon(c) {
  return {
    tag: 'i',
    attrs: {
      'data-icon': c
    }
  };
}

function dataAct(e) {
  return e.target.getAttribute('data-act') ||
    e.target.parentNode.getAttribute('data-act');
}


function navClick(ctrl, action) {
  var repeat = function() {
    control[action](ctrl);
    m.redraw();
    delay = Math.max(100, delay - delay / 15);
    timeout = setTimeout(repeat, delay);
  };
  var delay = 350;
  var timeout = setTimeout(repeat, 500);
  control[action](ctrl);
  document.addEventListener('mouseup', function() {
    clearTimeout(timeout);
  }, {once: true});
}

function buttons(ctrl) {
  var canJumpPrev = ctrl.vm.path !== '';
  var canJumpNext = !!ctrl.vm.node.children[0];
  var menuIsOpen = ctrl.actionMenu.open;
  return m('div.game_control', {
    config: bindOnce('mousedown', function(e) {
      var action = dataAct(e);
      if (action === 'prev' || action === 'next') navClick(ctrl, action);
      else if (action === 'first') control.first(ctrl);
      else if (action === 'last') control.last(ctrl);
      else if (action === 'explorer') ctrl.toggleExplorer();
      else if (action === 'practice') ctrl.togglePractice();
      else if (action === 'menu') ctrl.actionMenu.toggle();
    })
  }, [
    ctrl.embed ? null : m('div.features', ctrl.studyPractice ? [
      m('a', {
        class: 'hint--bottom',
        'data-hint': 'Analysis board',
        target: '_blank',
        href: ctrl.studyPractice.analysisUrl()
      }, icon('A'))
    ] : [
      m('button', {
        'data-hint': ctrl.trans('openingExplorer'),
        'data-act': 'explorer',
        class: 'hint--bottom' + (
          menuIsOpen || !ctrl.explorer.allowed() || ctrl.retro ? ' hidden' : (
            ctrl.explorer.enabled() ? ' active' : ''))
      }, icon(']')),
      ctrl.ceval.possible ? m('button', {
        'data-hint': 'Practice with computer',
        'data-act': 'practice',
        class: 'hint--bottom' + (
          menuIsOpen || ctrl.retro ? ' hidden' : (
            ctrl.practice ? ' active' : ''))
      }, icon('')) : null
  ]),
    m('div.jumps', [
      jumpButton('W', 'first', canJumpPrev),
      jumpButton('Y', 'prev', canJumpPrev),
      jumpButton('X', 'next', canJumpNext),
      jumpButton('V', 'last', canJumpNext)
    ]),
    ctrl.studyPractice ? m('div.noop') : m('button', {
      class: 'hint--bottom' + (menuIsOpen ? ' active' : ''),
      'data-hint': 'Menu',
      'data-act': 'menu'
    }, icon('['))
    ]);
}

function renderOpeningBox(ctrl) {
  var opening = ctrl.tree.getOpening(ctrl.vm.nodeList);
  if (!opening && !ctrl.vm.path) opening = ctrl.data.game.opening;
  if (opening) return m('div', {
    class: 'opening_box',
    title: opening.eco + ' ' + opening.name
  }, [
    m('strong', opening.eco),
    ' ' + opening.name
  ]);
}

function renderChapterName(ctrl) {
  if (ctrl.embed && ctrl.study) return m('div', {
    class: 'chapter_name'
  }, ctrl.study.currentChapter().name);
}

var firstRender = true;

export default function(ctrl: AnalyseController): VNode[] {
  var concealOf = makeConcealOf(ctrl);
  var showCevalPvs = !(ctrl.retro && ctrl.retro.isSolving()) && !ctrl.practice;
  var menuIsOpen = ctrl.actionMenu.open;
  return [
    m('div', {
      config: function(el, isUpdate) {
        if (firstRender) firstRender = false;
        else if (!isUpdate) lichess.pubsub.emit('reset_zoom')();
      },
      class: classSet({
        'gauge_displayed': ctrl.showEvalGauge(),
        'no_computer': !ctrl.vm.showComputer()
      })
    }, [
      m('div.lichess_game', {
        config: function(el, isUpdate) {
          if (isUpdate) return;
          lichess.pubsub.emit('content_loaded')();
        }
      }, [
        ctrl.data.blind ? blindBoard(ctrl) : visualBoard(ctrl),
        m('div.lichess_ground', [
          menuIsOpen ? null : renderClocks(ctrl),
          menuIsOpen ? null : crazyView.pocket(ctrl, ctrl.topColor(), 'top'),
          menuIsOpen ? actionMenu(ctrl) : [
            cevalView.renderCeval(ctrl),
            showCevalPvs ? cevalView.renderPvs(ctrl) : null,
            renderAnalyse(ctrl, concealOf),
            forkView(ctrl, concealOf),
            retroView(ctrl) || practiceView(ctrl) || explorerView(ctrl)
          ],
          menuIsOpen ? null : crazyView.pocket(ctrl, ctrl.bottomColor(), 'bottom'),
          buttons(ctrl)
        ])
      ])
    ]),
    ctrl.embed ? null : m('div', {
      class: 'underboard' + (ctrl.vm.showComputer() ? '' : ' no_computer')
    }, [
      m('div.center', ctrl.study ? studyView.underboard(ctrl) : inputs(ctrl)),
      m('div.right', acplView(ctrl))
    ]),
    ctrl.embed || synthetic(ctrl.data) ? null : m('div.analeft', [
      ctrl.forecast ? forecastView(ctrl) : null,
      game.playable(ctrl.data) ? m('div.back_to_game',
        m('a', {
          class: 'button text',
          href: ctrl.data.player.id ? router.player(ctrl.data) : router.game(ctrl.data),
          'data-icon': 'i'
        }, ctrl.trans('backToGame'))
      ) : null
    ])
  ];
};
