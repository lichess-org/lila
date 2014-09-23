var chessground = require('chessground');
var data = require('./data');

// function handle(handler) {
//   return function(event) {
//     event.stopPropagation();
//     event.preventDefault();
//     return handler(event);
//   }
// }

function promptNewFen(ctrl) {
  var fen = prompt('Paste FEN position').trim();
  if (fen) this.loadNewFen(fen);
}

function castleCheckBox(ctrl, id, label, reversed) {
  var input = m('input[type=checkbox]', {
    checked: ctrl.editor.castles[id],
    onchange: function() {
      ctrl.setCastle(id, this.checked);
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
        onclick: ctrl.toggleOrientation
      }, ctrl.trans('flipBoard')),
      m('a.button', {
        onclick: promptNewFen.bind(ctrl)
      }, ctrl.trans('loadPosition'))
    ]),
    m('div', [
      m('select.color', {
        value: ctrl.editor.color,
        onchange: m.withAttr('value', ctrl.setColor)
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
        value: data.makeUrl(fen)
      })
    ])
  ]);
}

module.exports = function(ctrl) {
  var fen = ctrl.computeFen();
  return m('div.editor', [
    chessground.view(ctrl.chessground),
    controls(ctrl, fen),
    inputs(ctrl, fen)
  ]);
};
