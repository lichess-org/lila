var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var status = require('../status');

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
      m('td', i + 1),
      renderTd(pair[0], 2 * i + 1, curPly),
      renderTd(pair[1], 2 * i + 2, curPly)
    ]);
  });
  if (result) {
    result = m('td.result', result);
    if (moves.length % 2 === 0) trs.push(m('tr', [m('td'), result]));
    else trs[trs.length - 1].children[2] = result;
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
    }, m('span[data-icon=B]')), [
      ['first', 'W', 1],
      ['prev', 'Y', curPly - 1],
      ['next', 'X', curPly + 1],
      ['last', 'V', nbMoves]
    ].map(function(b) {
      var enabled = curPly != b[2] && b[2] >= 1 && b[2] <= nbMoves;
      return m('a', {
        class: 'button ' + b[0] + ' ' + classSet({
          disabled: !enabled,
          glowing: ctrl.vm.late && b[0] === 'last'
        }),
        'data-icon': b[1],
        onclick: enabled ? partial(ctrl.jump, b[2]) : null
      });
    })
  ]);
}

module.exports = function(ctrl) {
  if (ctrl.root.data.game.variant.key == 'chess960')
    return m('div.notyet', 'The in-game replay will be available for chess960 very soon');
  var curPly = ctrl.active ? ctrl.ply : ctrl.root.data.game.moves.length;
  var h = curPly + ctrl.root.data.game.moves.join('') + ctrl.root.vm.flip;
  if (ctrl.vm.hash === h) return {
    subtree: 'retain'
  };
  ctrl.vm.hash = h;
  return m('div.replay', [
    ctrl.enabledByPref() ? m('div.moves', {
      config: function(boxEl, isUpdate) {
        var plyEl = boxEl.querySelector('.active');
        if (plyEl) boxEl.scrollTop = plyEl.offsetTop - boxEl.offsetHeight / 2 + plyEl.offsetHeight / 2;
      }
    }, renderTable(ctrl, curPly)) : null,
    renderButtons(ctrl, curPly)
  ]);
}
