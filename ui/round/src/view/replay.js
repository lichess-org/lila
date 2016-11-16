var round = require('../round');
var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var raf = require('chessground').util.requestAnimationFrame;
var game = require('game').game;
var util = require('../util');
var status = require('game').status;
var renderStatus = require('game').view.status;
var router = require('game').router;
var m = require('mithril');
var vn = require('mithril/render/vnode');

var emptyMove = vn('move', undefined, {
  class: 'empty'
}, undefined, '...');
var nullMove = vn('move', undefined, {
  class: 'empty'
}, undefined, '');

function renderMove(step, curPly) {
  var san = step.san[0] === 'P' ? step.san.slice(1) : step.san;
  return vn(
    'move',
    undefined,
    step.ply !== curPly ? {} : {
      class: 'active'
    },
    undefined,
    san
  );
}

function renderIndex(i) {
  return vn('index', undefined, undefined, undefined, i);
}

function renderMoves(ctrl) {
  var steps = ctrl.data.steps,
    len = steps.length;
  var firstPly = round.firstPly(ctrl.data);
  var lastPly = round.lastPly(ctrl.data);
  if (typeof lastPly === 'undefined') return [];

  var blackFirst = firstPly % 2 === 1;
  var nodes = [],
    startAt = 1;

  if (blackFirst) {
    nodes.push(renderIndex(1));
    nodes.push(emptyMove);
    nodes.push(steps[1] ? renderMove(steps[1], ctrl.vm.ply) : nullMove);
    startAt = 2;
  }

  for (var i = startAt; i < len; ++i) {
    if ((i % 2 === 0) === blackFirst) nodes.push(renderIndex((i + startAt) / 2));
    nodes.push(renderMove(steps[i], ctrl.vm.ply));
  }

  if (len % 2 === 0) nodes.push(nullMove);

  nodes.push(renderResult(ctrl));

  return nodes;
}

function renderResult(ctrl) {
  var result;
  if (status.finished(ctrl.data)) switch (ctrl.data.game.winner) {
    case 'white':
      result = '1-0';
      break;
    case 'black':
      result = '0-1';
      break;
    default:
      result = '½-½';
  }
  if (result || status.aborted(ctrl.data)) {
    var winner = game.getPlayer(ctrl.data, ctrl.data.game.winner);
    return m('div.end', [
      m('p.result', result),
      m('p.status', {
        oncreate: function() {
          if (ctrl.vm.autoScroll) ctrl.vm.autoScroll.now();
          else setTimeout(function() {
            ctrl.vm.autoScroll.now();
          }, 200);
        }
      }, [
        renderStatus(ctrl),
        winner ? ', ' + ctrl.trans.noarg(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
      ])
    ]);
  }
}

var analyseButtonIcon = m('span[data-icon="A"]');

function analyseButton(ctrl) {
  var showInfo = ctrl.forecastInfo();
  var attrs = {
    class: classSet({
      'fbt analysis': true,
      'hint--top': !showInfo,
      'hint--bottom': showInfo,
      'glowed': showInfo,
      'text': ctrl.data.forecastCount
    }),
    'data-hint': ctrl.trans.noarg('analysis'),
    href: router.game(ctrl.data, ctrl.data.player.color) + '/analysis#' + ctrl.vm.ply,
  };
  if (showInfo) attrs.oncreate = function(vnode) {
    setTimeout(function() {
      $(vnode.dom).powerTip({
        manual: true,
        fadeInTime: 300,
        fadeOutTime: 300,
        placement: 'n'
      }).data('powertipjq', $(vnode.dom).siblings('.forecast-info').clone().show()).powerTip('show');
    }, 1000);
  };
  return [
    m('a', attrs, [
      m('span', {
        'data-icon': 'A',
        class: ctrl.data.forecastCount ? 'text' : ''
      }),
      ctrl.data.forecastCount
    ]),
    showInfo ? m('div.forecast-info.info.none', [
      m('strong.title.text[data-icon=]', 'Speed up your game!'),
      m('span.content', [
        'Use the analysis board to create conditional premoves.',
        m('br'),
        'Now available on your turn!'
      ])
    ]) : null
  ];
}

var flipIcon = m('span[data-icon=B]');

var noopNode = m('div.noop');

function renderButtons(ctrl) {
  var d = ctrl.data;
  var firstPly = round.firstPly(d);
  var lastPly = round.lastPly(d);
  var flipAttrs = {
    class: 'fbt flip hint--top' + (ctrl.vm.flip ? ' active' : ''),
    'data-hint': ctrl.trans.noarg('flipBoard'),
    'data-act': 'flip'
  };
  return m('div.buttons', {
    onmousedown: function(e) {
      var ply = parseInt(e.target.getAttribute('data-ply'));
      if (!isNaN(ply)) ctrl.userJump(ply);
      else {
        var action = e.target.getAttribute('data-act') || e.target.parentNode.getAttribute('data-act');
        if (action === 'flip') {
          if (d.tv) location.href = '/tv/' + d.tv.channel + (d.tv.flip ? '' : '?flip=1');
          else if (d.player.spectator) location.href = router.game(d, d.opponent.color);
          else ctrl.flip();
        }
      }
    }
  }, [
    m('button', flipAttrs, flipIcon), m('nav', [
      ['W', firstPly],
      ['Y', ctrl.vm.ply - 1],
      ['X', ctrl.vm.ply + 1],
      ['V', lastPly]
    ].map(function(b, i) {
      var enabled = ctrl.vm.ply !== b[1] && b[1] >= firstPly && b[1] <= lastPly;
      return vn('button', undefined, {
        class: 'fbt' + (i === 3 && ctrl.isLate() && !ctrl.vm.initializing ? ' glowed' : ''),
        disabled: (ctrl.broken || !enabled),
        'data-icon': b[0],
        'data-ply': enabled ? b[1] : '-'
      });
    })), game.userAnalysable(d) ? analyseButton(ctrl) : noopNode
  ]);
}

function autoScroll(el, ctrl) {
  raf(function() {
    if (ctrl.data.steps.length < 7) return;
    var st;
    if (ctrl.vm.ply >= round.lastPly(ctrl.data) - 1) st = 9999;
    else {
      var plyEl = el.querySelector('.active') || el.querySelector('turn:first-child');
      if (plyEl) st = plyEl.offsetTop - el.offsetHeight / 2 + plyEl.offsetHeight / 2;
    }
    if (st !== undefined) el.scrollTop = st;
  });
}

function racingKingsInit(d) {
  if (d.game.variant.key === 'racingKings' && d.game.turns === 0 && !d.player.spectator)
    return m('div.message', {
      'data-icon': '',
    }, [
      "You have the " + d.player.color + " pieces",
      d.player.color === 'white' ? [',', m('br'), m('strong', "it's your turn!")] : null
    ]);
}

module.exports = {
  oninit: function(vnode) {
    vnode.state.ctrl = vnode.attrs.ctrl;
  },
  onbeforeupdate: function(vnode) {
    var ctrl = vnode.state.ctrl;
    var d = ctrl.data;
    var hash = ctrl.stepsHash(d.steps) + d.game.status.id + d.game.winner + ctrl.vm.ply + ctrl.vm.flip;
    if (vnode.state.hash === hash) return false;
    vnode.state.hash = hash;
  },
  view: function(vnode) {
    var ctrl = vnode.state.ctrl;
    var d = ctrl.data;
    return m('div', {
      key: 'replay',
      class: 'replay',
    }, [
      renderButtons(ctrl),
      racingKingsInit(ctrl.data) || (ctrl.replayEnabledByPref() ? m('div.moves', {
        oncreate: function(vnode) {
          var scrollNow = partial(autoScroll, vnode.dom, ctrl);
          ctrl.vm.autoScroll = {
            now: scrollNow,
            throttle: util.throttle(300, false, scrollNow)
          };
          scrollNow();
          window.addEventListener('load', scrollNow);
        },
        onmousedown: function(e) {
          var ply, prev = e.target.previousElementSibling;
          if (prev.tagName == 'INDEX') ply = parseInt(prev.textContent) * 2 - 1;
          else {
            prev = prev.previousElementSibling;
            if (prev.tagName == 'INDEX') ply = parseInt(prev.textContent) * 2;
          }
          if (ply) ctrl.userJump(ply);
        },
      }, renderMoves(ctrl)) : renderResult(ctrl))
    ]);
  }
};
