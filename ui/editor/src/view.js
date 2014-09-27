var partial = require('lodash-node/modern/functions/partial');
var chessground = require('chessground');
var editor = require('./editor');
var drag = require('./drag');

function promptNewFen(ctrl) {
  var fen = prompt('Paste FEN position').trim();
  if (fen) ctrl.loadNewFen(fen);
}

function castleCheckBox(ctrl, id, label, reversed) {
  var input = m('input[type=checkbox]', {
    checked: ctrl.data.castles[id](),
    onchange: function() {
      ctrl.data.castles[id](this.checked);
    }
  });
  return m('label', reversed ? [input, label] : [label, input]);
}

function controls(ctrl, fen) {
  return m('div#editor-side', [
    m('div', [
      m('a.button', {
        onclick: ctrl.startPosition
      }, ctrl.trans('startPosition')),
      m('a.button', {
        onclick: ctrl.clearBoard
      }, ctrl.trans('clearBoard'))
    ]),
    m('div', [
      m('a.button[data-icon=B]', {
        onclick: ctrl.chessground.toggleOrientation
      }, ctrl.trans('flipBoard')),
      m('a.button', {
        onclick: promptNewFen.bind(ctrl)
      }, ctrl.trans('loadPosition'))
    ]),
    m('div', [
      m('select.color', {
        value: ctrl.data.color(),
        onchange: m.withAttr('value', ctrl.data.color)
      }, [
        m('option[value=w]', ctrl.trans('whitePlays')),
        m('option[value=b]', ctrl.trans('blackPlays'))
      ])
    ]),
    m('div.castling', [
      m('strong', 'Castling'),
      m('div', [
        castleCheckBox(ctrl, 'K', 'White O-O', false),
        castleCheckBox(ctrl, 'Q', 'White O-O-O', true)
      ]),
      m('div', [
        castleCheckBox(ctrl, 'k', 'Black O-O', false),
        castleCheckBox(ctrl, 'q', 'Black O-O-O', true)
      ])
    ]),
    m('div', [
      m('a.button', {
        href: '/?fen=' + fen + '#ai'
      }, ctrl.trans('playWithTheMachine')),
      m('a.button', {
        href: '/?fen=' + fen + '#friend'
      }, ctrl.trans('playWithAFriend'))
    ])
  ]);
}

function inputs(ctrl, fen) {
  return m('div.copyables', [
    m('p', [
      m('strong.name', 'FEN'),
      m('input.copyable[readonly][spellCheck=false]', {
        value: fen
      })
    ]),
    m('p', [
      m('strong.name', 'URL'),
      m('input.copyable[readonly][spellCheck=false]', {
        value: editor.makeUrl(ctrl.data, fen)
      })
    ])
  ]);
}

function sparePieces(ctrl, color) {
  return m('div.spare', ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].map(function(role) {
    return m('div', {
      class: ['cg-piece', color, role].join(' '),
      'data-color': color,
      'data-role': role
    })
  }));
}

function extraPiece(ctrl) {
  var cur = ctrl.data.extra;
  if (!cur.role) return;
  return m('div', {
    class: ['extra-piece', 'cg-piece', cur.color, cur.role].join(' '),
    style: {
      left: cur.left + cur.dec[0] + 'px',
      top: cur.top + cur.dec[1] + 'px'
    }
  });
}

module.exports = function(ctrl) {
  var fen = ctrl.computeFen();
  var color = ctrl.chessground.data.orientation;
  var opposite = color === 'white' ? 'black' : 'white';
  return m('div.editor', {
    config: function(el, isUpdate, context) {
      if (isUpdate) return;
      var onstart = partial(drag.start, ctrl);
      var onmove = partial(drag.move, ctrl);
      var onend = partial(drag.end, ctrl);
      document.addEventListener('mousedown', onstart);
      document.addEventListener('mousemove', onmove);
      document.addEventListener('mouseup', onend);
      context.onunload = function() {
        document.removeEventListener('mousedown', onstart);
        document.removeEventListener('mousemove', onmove);
        document.removeEventListener('mouseup', onend);
      };
    }
  }, [
    m('div.board-and-spare', [
      sparePieces(ctrl, opposite),
      chessground.view(ctrl.chessground),
      sparePieces(ctrl, color),
      extraPiece(ctrl)
    ]),
    controls(ctrl, fen),
    inputs(ctrl, fen)
  ]);
};
