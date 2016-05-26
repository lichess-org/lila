var m = require('mithril');
var chessground = require('chessground');
var raf = chessground.util.requestAnimationFrame;
var util = require('./util');
var game = require('game').game;
var renderStatus = require('game').view.status;
var router = require('game').router;
var treeView = require('./tree/treeView');
var control = require('./control');
var actionMenu = require('./actionMenu').view;
var renderPromotion = require('./promotion').view;
var pgnExport = require('./pgnExport');
var forecastView = require('./forecast/forecastView');
var cevalView = require('./ceval/cevalView');
var crazyView = require('./crazy/crazyView');
var explorerView = require('./explorer/explorerView');
var studyView = require('./study/studyView');
var contextMenu = require('./contextMenu');

var autoScroll = util.throttle(300, false, function(el) {
  raf(function() {
    var plyEl = el.querySelector('.active') || el.querySelector('turn:first-child');
    if (plyEl) el.scrollTop = plyEl.offsetTop - el.offsetHeight / 2 + plyEl.offsetHeight / 2;
  });
});

function renderAnalyse(ctrl) {
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
  var conceal;
  if (ctrl.study && ctrl.study.data.chapter.conceal !== null) conceal = {
    owner: ctrl.study.isChapterOwner(),
    ply: ctrl.study.data.chapter.conceal
  };
  var tags = treeView.renderMainline(ctrl, ctrl.vm.mainline, conceal);
  if (result) {
    tags.push(m('div.result', result));
    var winner = game.getPlayer(ctrl.data, ctrl.data.game.winner);
    tags.push(m('div.status', [
      renderStatus(ctrl),
      winner ? ', ' + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
    ]));
  }
  return m('div.replay', {
      onmousedown: function(e) {
        if (e.button !== undefined && e.button !== 0) return; // only touch or left click
        var path = treeView.eventPath(e, ctrl);
        if (path) ctrl.userJump(path);
      },
      oncontextmenu: function(e) {
        var path = treeView.eventPath(e, ctrl);
        contextMenu.open(e, {
          path: path,
          root: ctrl
        });
        return false;
      },
      onclick: function(e) {
        return false;
      },
      config: function(el, isUpdate) {
        if (ctrl.vm.autoScrollRequested || !isUpdate) {
          autoScroll(el);
          ctrl.vm.autoScrollRequested = false;
        }
      }
    },
    tags);
}

function wheel(ctrl, e) {
  if (e.target.tagName !== 'PIECE' && e.target.tagName !== 'SQUARE') return;
  if (e.deltaY > 0) control.next(ctrl);
  else if (e.deltaY < 0) control.prev(ctrl);
  m.redraw();
  e.preventDefault();
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
      onchange: function(e) {
        if (e.target.value !== ctrl.vm.node.fen) ctrl.changeFen(e.target.value);
      }
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
          onclick: function(e) {
            var pgn = $('.copyables .pgn textarea').val();
            if (pgn !== pgnText) ctrl.changePgn(pgn);
          }
        }, 'Import PGN')
      ])
    ])
  ]);
}

function visualBoard(ctrl) {
  return m('div.lichess_board_wrap', [
    m('div.lichess_board.' + ctrl.data.game.variant.key, {
        config: function(el, isUpdate) {
          if (!isUpdate) el.addEventListener('wheel', function(e) {
            return wheel(ctrl, e);
          });
        }
      },
      chessground.view(ctrl.chessground),
      renderPromotion(ctrl),
      ctrl.study ? studyView.overboard(ctrl.study) : null),
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
    chessground.view(ctrl.chessground)
  ]);
}

var cachedButtons = (function() {
  var make = function(icon, effect) {
    return m('button', {
      class: 'button',
      'data-act': effect,
      'data-icon': icon
    });
  };
  return m('div', [
    m('div.jumps', [
      make('Y', 'prev'),
      make('W', 'first')
    ]),
    m('div.jumps', [
      make('X', 'next'),
      make('V', 'last')
    ])
  ])
})();

function icon(c) {
  return {
    tag: 'i',
    attrs: {
      'data-icon': c
    }
  };
}

function buttons(ctrl) {
  return m('div.game_control',
    m('div.buttons', {
      onmousedown: function(e) {
        var action = e.target.getAttribute('data-act') || e.target.parentNode.getAttribute('data-act');
        if (action === 'explorer') ctrl.explorer.toggle();
        else if (action === 'menu') ctrl.actionMenu.toggle();
        else if (control[action]) control[action](ctrl);
      }
    }, [
      cachedButtons,
      m('div', [
        (ctrl.actionMenu.open || !ctrl.explorer.allowed()) ? null : m('button', {
          id: 'open_explorer',
          'data-hint': ctrl.trans('openingExplorer'),
          'data-act': 'explorer',
          class: 'button hint--bottom' + (ctrl.explorer.enabled() ? ' active' : '')
        }, icon(']')),
        m('button', {
          class: 'button menu hint--bottom' + (ctrl.actionMenu.open ? ' active' : ''),
          'data-hint': 'Menu',
          'data-act': 'menu'
        }, icon('['))
      ])
    ])
  );
}

function renderOpeningBox(ctrl) {
  var opening = ctrl.tree.getOpening(ctrl.vm.nodeList);
  if (opening) return m('div', {
    class: 'opening_box',
    title: opening.eco + ' ' + opening.name
  }, [
    m('strong', opening.eco),
    ' ' + opening.name
  ]);
}

module.exports = function(ctrl) {
  return [
    m('div', {
      class: ctrl.showEvalGauge() ? 'gauge_displayed' : ''
    }, [
      m('div.lichess_game', {
        config: function(el, isUpdate, context) {
          if (isUpdate) return;
          $('body').trigger('lichess.content_loaded');
        }
      }, [
        ctrl.data.blind ? blindBoard(ctrl) : visualBoard(ctrl),
        m('div.lichess_ground', [
          ctrl.actionMenu.open ? null : crazyView.pocket(ctrl, ctrl.data.opponent.color, 'top'),
          ctrl.actionMenu.open ? actionMenu(ctrl) : [
            cevalView.renderCeval(ctrl),
            renderOpeningBox(ctrl),
            renderAnalyse(ctrl),
            explorerView.renderExplorer(ctrl)
          ],
          ctrl.actionMenu.open ? null : crazyView.pocket(ctrl, ctrl.data.player.color, 'bottom'),
          buttons(ctrl)
        ])
      ])
    ]),
    m('div.underboard', [
      m('div.center', ctrl.study ? studyView.underboard(ctrl) : inputs(ctrl)),
      m('div.right')
    ]),
    util.synthetic(ctrl.data) ? null : m('div.analeft', [
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
