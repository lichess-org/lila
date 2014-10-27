var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var round = require('../round');
var status = require('../status');
var renderStatus = require('../view/status');

function renderTd(move, ply, curPly) {
  return move ? {
    tag: 'td',
    attrs: {
      class: 'move' + (ply === curPly ? ' active' : ''),
      'data-ply': ply
    },
    children: move
  } : null;
}

function renderTable(ctrl, curPly) {
  var moves = ctrl.root.data.game.moves;
  var pairs = [];
  for (var i = 0; i < moves.length; i += 2) pairs.push([moves[i], moves[i + 1]]);
  var result;
  if (status.finished(ctrl.root.data)) switch (ctrl.root.data.game.winner) {
    case 'white':
      result = '1-0';
      break;
    case 'black':
      result = '0-1';
      break;
    default:
      result = '½-½';
  }
  var trs = pairs.map(function(pair, i) {
    return m('tr', [
      m('td.index', i + 1),
      renderTd(pair[0], 2 * i + 1, curPly),
      renderTd(pair[1], 2 * i + 2, curPly)
    ]);
  });
  if (result) {
    trs.push(m('tr', m('td.result[colspan=3]', result)));
    var winner = round.getPlayer(ctrl.root.data, ctrl.root.data.game.winner);
    trs.push(m('tr.status', m('td[colspan=3]', [
      renderStatus(ctrl.root),
      winner ? ', ' + ctrl.root.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
    ])));
  }
  return m('table',
    m('tbody', {
        onclick: function(e) {
          var ply = e.target.getAttribute('data-ply');
          if (ply) ctrl.jump(parseInt(ply));
        }
      },
      trs));
}

function renderButtons(ctrl, curPly) {
  var nbMoves = ctrl.root.data.game.moves.length;
  return m('div.buttons', [
    m('a', {
      class: 'button flip hint--bottom' + (ctrl.root.vm.flip ? ' active' : ''),
      'data-hint': ctrl.root.trans('flipBoard'),
      onclick: ctrl.root.flip
    }, m('span[data-icon=B]')), m('div.hint--bottom', {
      'data-hint': 'Tip: use your keyboard arrow keys!'
    }, [
      ['first', 'W', 1],
      ['prev', 'Y', curPly - 1],
      ['next', 'X', curPly + 1],
      ['last', 'V', nbMoves]
    ].map(function(b) {
      var enabled = curPly != b[2] && b[2] >= 1 && b[2] <= nbMoves;
      return m('a', {
        class: 'button ' + b[0] + ' ' + classSet({
          disabled: (ctrl.broken || !enabled),
          glowing: ctrl.vm.late && b[0] === 'last'
        }),
        'data-icon': b[1],
        onclick: enabled ? partial(ctrl.jump, b[2]) : null
      });
    }))
  ]);
}

function autoScroll(movelist) {
  var plyEl = movelist.querySelector('.active');
  if (plyEl) movelist.scrollTop = plyEl.offsetTop - movelist.offsetHeight / 2 + plyEl.offsetHeight / 2;
}

module.exports = function(ctrl) {
  var curPly = ctrl.active ? ctrl.ply : ctrl.root.data.game.moves.length;
  var h = curPly + ctrl.root.data.game.moves.join('') + ctrl.root.vm.flip;
  if (ctrl.vm.hash === h) return {
    subtree: 'retain'
  };
  ctrl.vm.hash = h;
  return m('div.replay', [
    ctrl.enabledByPref() ? m('div.moves', {
      config: function(el, isUpdate) {
        autoScroll(el);
        if (!isUpdate) setTimeout(partial(autoScroll, el), 100);
      },
    }, renderTable(ctrl, curPly)) : null,
    renderButtons(ctrl, curPly)
  ]);
}
