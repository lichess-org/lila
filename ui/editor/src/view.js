var chessground = require('chessground');
var partial = chessground.util.partial;
var editor = require('./editor');
var drag = require('./drag');
var m = require('mithril');

function castleCheckBox(ctrl, id, label, reversed) {
  var input = m('input[type=checkbox]', {
    checked: ctrl.data.castles[id](),
    onchange: function() {
      ctrl.data.castles[id](this.checked);
    }
  });
  return m('label', reversed ? [input, label] : [label, input]);
}

function optgroup(name, opts) {
  return m('optgroup', {
    label: name
  }, opts);
}

function controls(ctrl, fen) {
  var positionIndex = ctrl.positionIndex[fen.split(' ')[0]];
  var currentPosition = positionIndex !== -1 ? ctrl.data.positions[positionIndex] : null;
  var encodedFen = fen.replace(/\s/g, '_');
  var position2option = function(pos) {
    return {
      tag: 'option',
      attrs: {
        value: pos.fen,
        selected: currentPosition && currentPosition.fen === pos.fen
      },
      children: [pos.name]
    };
  }
  return m('div#editor-side', [
    m('div', [
      m('select.positions', {
        onchange: function(e) {
          ctrl.loadNewFen(e.target.value);
        }
      }, [
        optgroup('Set the board', [
          currentPosition ? null : m('option', {
            value: fen,
            selected: true
          }, '- Position -'),
          ctrl.extraPositions.map(position2option)
        ]),
        optgroup('Popular openings',
          ctrl.data.positions.map(position2option)
        )
      ])
    ]),
    m('div.metadata.content_box', [
      m('div.color', [
        m('select', {
          value: ctrl.data.color(),
          onchange: m.withAttr('value', ctrl.data.color)
        }, [
          m('option[value=w]', ctrl.trans('whitePlays')),
          m('option[value=b]', ctrl.trans('blackPlays'))
        ])
      ]),
      m('div.castling', [
        m('strong', ctrl.trans('castling')),
        m('div', [
          castleCheckBox(ctrl, 'K', ctrl.trans('whiteCastlingKingside'), false),
          castleCheckBox(ctrl, 'Q', ctrl.trans('whiteCastlingQueenside'), true)
        ]),
        m('div', [
          castleCheckBox(ctrl, 'k', ctrl.trans('blackCastlingKingside'), false),
          castleCheckBox(ctrl, 'q', ctrl.trans('blackCastlingQueenside'), true)
        ])
      ])
    ]),
    m('div', [
      m('a.button.text[data-icon=B]', {
        onclick: ctrl.chessground.toggleOrientation
      }, ctrl.trans('flipBoard')),
      ctrl.positionLooksLegit() ? m('a.button.text[data-icon="A"]', {
        href: editor.makeUrl('/analysis/', fen),
        rel: 'nofollow'
      }, ctrl.trans('analysis')) : m('span.button.disabled.text[data-icon="A"]', {
        rel: 'nofollow'
      }, ctrl.trans('analysis')),
      m('a.button', {
          onclick: function() {
            $.modal($('.continue_with'));
          }
        },
        m('span.text[data-icon=U]', ctrl.trans('continueFromHere')))
    ]),
    m('div.continue_with', [
      m('a.button', {
        href: '/?fen=' + encodedFen + '#ai',
        rel: 'nofollow'
      }, ctrl.trans('playWithTheMachine')),
      m('br'),
      m('a.button', {
        href: '/?fen=' + encodedFen + '#friend',
        rel: 'nofollow'
      }, ctrl.trans('playWithAFriend'))
    ])
  ]);
}

function inputs(ctrl, fen) {
  if (ctrl.vm.redirecting) return m.trust(lichess.spinnerHtml);
  return m('div.copyables', [
    m('p', [
      m('strong.name', 'FEN'),
      m('input.copyable.autoselect[spellCheck=false]', {
        value: fen,
        onchange: function(e) {
          if (e.target.value !== fen) ctrl.changeFen(e.target.value);
        }
      })
    ]),
    m('p', [
      m('strong.name', 'URL'),
      m('input.copyable.autoselect[readonly][spellCheck=false]', {
        value: editor.makeUrl(ctrl.data.baseUrl, fen)
      })
    ])
  ]);
}

function sparePieces(ctrl, color, orientation, position) {
  return m('div', {
    class: ['spare', position, 'orientation-' + orientation, color].join(' ')
  }, ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].map(function(role) {
    return m('div.no-square', m('piece', {
      class: color + ' ' + role,
      'data-color': color,
      'data-role': role
    }))
  }));
}

var eventNames = ['mousedown', 'touchstart'];

module.exports = function(ctrl) {
  var fen = ctrl.computeFen();
  var color = ctrl.chessground.data.orientation;
  var opposite = color === 'white' ? 'black' : 'white';
  return m('div.editor', {
    config: function(el, isUpdate, context) {
      if (isUpdate) return;
      var onstart = partial(drag, ctrl);
      eventNames.forEach(function(name) {
        document.addEventListener(name, onstart);
      });
      context.onunload = function() {
        eventNames.forEach(function(name) {
          document.removeEventListener(name, onstart);
        });
      };
    }
  }, [
    sparePieces(ctrl, opposite, color, 'top'),
    chessground.view(ctrl.chessground),
    sparePieces(ctrl, color, color, 'bottom'),
    controls(ctrl, fen),
    inputs(ctrl, fen)
  ]);
};
