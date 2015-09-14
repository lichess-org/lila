var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var game = require('game').game;
var status = require('game').status;
var renderStatus = require('./status');
var m = require('mithril');

var emptyTd = m('td.move', '...');

function renderTd(step, curPly, orEmpty) {
  return step ? {
    tag: 'td',
    attrs: {
      class: 'move' + (step.ply === curPly ? ' active' : ''),
      'data-ply': step.ply
    },
    children: [step.san]
  } : (orEmpty ? emptyTd : null)
}

function renderResult(ctrl, asTable) {
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
    return asTable ? [
      m('tr', m('td.result[colspan=3]', result)),
      m('tr.status', m('td[colspan=3]', [
        renderStatus(ctrl),
        winner ? ', ' + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
      ]))
    ] : [
      m('p.result', result),
      m('p.status', [
        renderStatus(ctrl),
        winner ? ', ' + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
      ])
    ];
  }
}

function renderTable(ctrl) {
  var steps = ctrl.data.steps;
  var firstPly = ctrl.firstPly();
  var lastPly = ctrl.lastPly();
  if (typeof lastPly === 'undefined') return;

  var pairs = [];
  if (firstPly % 2 === 0)
    for (var i = 1, len = steps.length; i < len; i += 2) pairs.push([steps[i], steps[i + 1]]);
  else {
    pairs.push([null, steps[1]]);
    for (var i = 2, len = steps.length; i < len; i += 2) pairs.push([steps[i], steps[i + 1]]);
  }

  var trs = [];
  for (var i = 0, len = pairs.length; i < len; i++)
    trs.push(m('tr', [
      m('td.index', i + 1),
      renderTd(pairs[i][0], ctrl.vm.ply, true),
      renderTd(pairs[i][1], ctrl.vm.ply, false)
    ]));
  if (ctrl.conditionable()) trs.push(renderConditional(ctrl));
  else trs.push(renderResult(ctrl, true));

  return m('table',
    m('tbody', {
        onclick: function(e) {
          var ply = e.target.getAttribute('data-ply');
          if (ply) ctrl.jump(parseInt(ply));
        }
      },
      trs));
}

function renderConditional(ctrl) {
  return m('tr.forecast', m('td.result[colspan=3]',
    m('a.button', {
      href: '/' + ctrl.data.game.id + ctrl.data.player.id + '/forecast'
    }, 'Conditional premoves')
  ));
}

function renderButtons(ctrl) {
  var firstPly = ctrl.firstPly();
  var lastPly = ctrl.lastPly();
  var flipAttrs = {
    class: 'button flip hint--top' + (ctrl.vm.flip ? ' active' : ''),
    'data-hint': ctrl.trans('flipBoard'),
  };
  if (ctrl.data.tv) flipAttrs.href = '/tv/' + ctrl.data.tv.channel + (ctrl.data.tv.flip ? '' : '?flip=1');
  else if (ctrl.data.player.spectator) flipAttrs.href = ctrl.router.Round.watcher(ctrl.data.game.id, ctrl.data.opponent.color).url;
  else flipAttrs.onclick = ctrl.flip;
  return m('div.buttons', [
    m('a', flipAttrs, m('span[data-icon=B]')), [
      ['first', 'W', ctrl.firstPly()],
      ['prev', 'Y', ctrl.vm.ply - 1],
      ['next', 'X', ctrl.vm.ply + 1],
      ['last', 'V', lastPly]
    ].map(function(b) {
      var enabled = ctrl.vm.ply !== b[2] && b[2] >= firstPly && b[2] <= lastPly;
      return m('a', {
        class: 'button ' + b[0] + ' ' + classSet({
          disabled: (ctrl.broken || !enabled),
          glowed: b[0] === 'last' && ctrl.isLate()
        }),
        'data-icon': b[1],
        onclick: enabled ? partial(ctrl.jump, b[2]) : null
      });
    }), (game.userAnalysable(ctrl.data) ? m('a', {
      class: 'button hint--top analysis',
      'data-hint': ctrl.trans('analysis'),
      href: ctrl.router.UserAnalysis.game(ctrl.data.game.id, ctrl.data.player.color).url + '#' + ctrl.vm.ply,
    }, m('span[data-icon="A"]')) : null)
  ]);
}

function autoScroll(movelist) {
  var plyEl = movelist.querySelector('.active') || movelist.querySelector('tr:first-child');
  if (plyEl) movelist.scrollTop = plyEl.offsetTop - movelist.offsetHeight / 2 + plyEl.offsetHeight / 2;
}

module.exports = function(ctrl) {
  var h = ctrl.vm.ply + ctrl.stepsHash(ctrl.data.steps) + ctrl.data.game.status.id + ctrl.data.game.winner + ctrl.vm.flip;
  if (ctrl.vm.replayHash === h) return {
    subtree: 'retain'
  };
  ctrl.vm.replayHash = h;
  return m('div.replay', [
    renderButtons(ctrl),
    ctrl.replayEnabledByPref() ? m('div.moves', {
      config: function(el, isUpdate) {
        autoScroll(el);
        if (!isUpdate) setTimeout(partial(autoScroll, el), 100);
      },
    }, renderTable(ctrl)) : renderResult(ctrl, false)
  ]);
}
