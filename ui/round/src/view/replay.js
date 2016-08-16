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

var emptyMove = m('move.empty', '...');
var nullMove = m('move.empty', '');

function renderMove(step, curPly, orEmpty) {
  return step ? {
    tag: 'move',
    attrs: step.ply !== curPly ? {} : {
      class: 'active'
    },
    children: [step.san[0] === 'P' ? step.san.slice(1) : step.san]
  } : (orEmpty ? emptyMove : nullMove)
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
    return [
      m('p.result', result),
      m('p.status', [
        renderStatus(ctrl),
        winner ? ', ' + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
      ])
    ];
  }
}

function renderMoves(ctrl) {
  var steps = ctrl.data.steps;
  var firstPly = round.firstPly(ctrl.data);
  var lastPly = round.lastPly(ctrl.data);
  if (typeof lastPly === 'undefined') return;

  var pairs = [];
  if (firstPly % 2 === 0)
    for (var i = 1, len = steps.length; i < len; i += 2) pairs.push([steps[i], steps[i + 1]]);
  else {
    pairs.push([null, steps[1]]);
    for (var i = 2, len = steps.length; i < len; i += 2) pairs.push([steps[i], steps[i + 1]]);
  }

  var rows = [];
  for (var i = 0, len = pairs.length; i < len; i++) rows.push({
    tag: 'turn',
    children: [{
        tag: 'index',
        children: [i + 1]
      },
      renderMove(pairs[i][0], ctrl.vm.ply, true),
      renderMove(pairs[i][1], ctrl.vm.ply, false)
    ]
  });
  rows.push(renderResult(ctrl));

  return rows;
}

var analyseButtonIcon = m('span[data-icon="A"]');

function analyseButton(ctrl) {
  var showInfo = ctrl.forecastInfo();
  var attrs = {
    class: classSet({
      'button analysis': true,
      'hint--top': !showInfo,
      'hint--bottom': showInfo,
      'glowed': showInfo,
      'text': ctrl.data.forecastCount
    }),
    'data-hint': ctrl.trans('analysis'),
    href: router.game(ctrl.data, ctrl.data.player.color) + '/analysis#' + ctrl.vm.ply,
  };
  if (showInfo) attrs.config = function(el) {
    setTimeout(function() {
      $(el).powerTip({
        manual: true,
        fadeInTime: 300,
        fadeOutTime: 300,
        placement: 'n'
      }).data('powertipjq', $(el).siblings('.forecast-info').clone().show()).powerTip('show');
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

function renderButtons(ctrl) {
  var d = ctrl.data;
  var firstPly = round.firstPly(d);
  var lastPly = round.lastPly(d);
  var flipAttrs = {
    class: 'button flip hint--top' + (ctrl.vm.flip ? ' active' : ''),
    'data-hint': ctrl.trans('flipBoard'),
    'data-act': 'flip'
  };
  return m('div.buttons', {
    config: util.bindOnce('mousedown', function(e) {
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
    })
  }, [
    m('a', flipAttrs, flipIcon), [
      ['W', firstPly],
      ['Y', ctrl.vm.ply - 1],
      ['X', ctrl.vm.ply + 1],
      ['V', lastPly]
    ].map(function(b, i) {
      var enabled = ctrl.vm.ply !== b[1] && b[1] >= firstPly && b[1] <= lastPly;
      return m('a', {
        class: 'button ' + classSet({
          disabled: (ctrl.broken || !enabled),
          glowed: i === 3 && ctrl.isLate() && !ctrl.vm.initializing
        }),
        'data-icon': b[0],
        'data-ply': enabled ? b[1] : '-'
      });
    }), game.userAnalysable(d) ? analyseButton(ctrl) : null
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

module.exports = function(ctrl) {
  var d = ctrl.data;
  var h = ctrl.vm.ply + ctrl.stepsHash(d.steps) + d.game.status.id + d.game.winner + ctrl.vm.flip;
  if (ctrl.vm.replayHash === h) return {
    subtree: 'retain'
  };
  ctrl.vm.replayHash = h;
  var message = (d.game.variant.key === 'racingKings' && d.game.turns === 0) ? racingKingsInit : null;
  return m('div.replay', [
    renderButtons(ctrl),
    racingKingsInit(ctrl.data) || (ctrl.replayEnabledByPref() ? m('div.moves', {
      config: function(el, isUpdate, ctx) {
        if (isUpdate) return;
        util.bindOnce('mousedown', function(e) {
          var turn = parseInt($(e.target).siblings('index').text());
          var ply = 2 * turn - 2 + $(e.target).index();
          if (ply) ctrl.userJump(ply);
        })(el, isUpdate, ctx);
        var scrollNow = partial(autoScroll, el, ctrl);
        ctrl.vm.autoScroll = {
          now: scrollNow,
          throttle: util.throttle(300, false, scrollNow)
        };
        scrollNow();
      }
    }, renderMoves(ctrl)) : renderResult(ctrl))
  ]);
}
