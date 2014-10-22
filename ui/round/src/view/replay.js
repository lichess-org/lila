var partial = require('chessground').util.partial;

function renderTd(move, klass, ply, curPly) {
  return move ? {
    tag: 'td',
    attrs: {
      class: klass + (ply === curPly ? ' active' : ''),
      'data-ply': ply
    },
    children: move
  } : null;
}

function renderTable(ctrl, curPly) {
  var moves = ctrl.data.game.moves;
  var pairs = [];
  for (var i = 0; i < moves.length; i += 2) pairs.push([moves[i], moves[i + 1]]);
  return m('table',
    m('tbody', {
        onclick: function(e) {
          var ply = e.target.getAttribute('data-ply');
          if (ply) ctrl.replay.jump(parseInt(ply));
        }
      },
      pairs.map(function(pair, i) {
        return m('tr',
          m('td', i),
          renderTd(pair[0], 'move', 2 * i + 1, curPly),
          renderTd(pair[1], 'move', 2 * i + 2, curPly));
      })));
}

function renderButtons(ctrl, curPly) {
  return m('div.buttons', [
    ['first', 'W', 1],
    ['prev', 'Y', curPly - 1],
    ['next', 'X', curPly + 1],
    ['last', 'V', ctrl.data.game.moves.length]
  ].map(function(b) {
    var enabled = curPly != b[2] && b[2] >= 1 && b[2] <= ctrl.data.game.moves.length;
    return m('a.button.' + b[0] + (enabled ? '' : '.disabled'), {
      'data-icon': b[1],
      onclick: enabled ? partial(ctrl.replay.jump, b[2]) : null
    });
  }));
}

module.exports = function(ctrl) {
  if (ctrl.data.game.variant.key == 'chess960')
    return m('div.notyet', 'The in-game replay will be available for chess960 very soon');
  var curPly = ctrl.replay.active ? ctrl.replay.ply : ctrl.data.game.moves.length;
  return m('div.replay', [
    m('div.moves', {
      config: function(boxEl, isUpdate, context) {
        var hash = curPly + ctrl.data.game.moves.join('');
        if (hash === context.hash) return;
        context.hash = hash;
        var plyEl = boxEl.querySelector('.active');
        boxEl.scrollTop = plyEl.offsetTop - boxEl.offsetHeight / 2 + plyEl.offsetHeight / 2;
      }
    }, renderTable(ctrl, curPly)),
    renderButtons(ctrl, curPly)
  ]);
}
