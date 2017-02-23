var chessground = require('chessground');
var partial = chessground.util.partial;
var editor = require('./editor');
var drag = require('./drag');
var m = require('mithril');

function castleCheckBox(ctrl, id, label, reversed) {
  var input = m('input[type=checkbox]', {
    checked: ctrl.data.castles[id](),
    onchange: function(e) {
      ctrl.setCastle(id, e.target.checked);
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
  var currentPosition = ctrl.data.positions && positionIndex !== -1 ? ctrl.data.positions[positionIndex] : null;
  var encodedFen = fen.replace(/\s/g, '_');
  var position2option = function(pos) {
    return {
      tag: 'option',
      attrs: {
        value: pos.fen,
        selected: currentPosition && currentPosition.fen === pos.fen
      },
      children: [pos.eco ? pos.eco + " " + pos.name : pos.name]
    };
  }
  return m('div.editor-side', [
    ctrl.embed ? null : m('div', [
      ctrl.data.positions ? m('select.positions', {
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
      ]) : null
    ]),
    m('div.metadata.content_box', [
      m('div.color',
        m('select', {
          onchange: m.withAttr('value', ctrl.setColor)
        }, ['whitePlays', 'blackPlays'].map(function(key) {
          return m('option', {
            value: key[0],
            selected: ctrl.data.color() === key[0]
          }, ctrl.trans(key));
        }))
      ),
      m('div.castling', [
        m('strong', ctrl.trans('castling')),
        m('div', [
          castleCheckBox(ctrl, 'K', ctrl.trans('whiteCastlingKingside'), ctrl.options.inlineCastling),
          castleCheckBox(ctrl, 'Q', ctrl.trans('whiteCastlingQueenside'), true)
        ]),
        m('div', [
          castleCheckBox(ctrl, 'k', ctrl.trans('blackCastlingKingside'), ctrl.options.inlineCastling),
          castleCheckBox(ctrl, 'q', ctrl.trans('blackCastlingQueenside'), true)
        ])
      ])
    ]),
    ctrl.embed ? m('div', [
      m('a.button.frameless', {
        onclick: ctrl.startPosition
      }, 'Initial position'),
      m('a.button.frameless', {
        onclick: ctrl.clearBoard
      }, 'Empty board')
    ]) : m('div', [
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
    ctrl.embed ? null : m('div.continue_with', [
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
  if (ctrl.embed) return;
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

function isRightButton(e) {
  return e.buttons === 2 || e.button === 2;
}

function isRightClick(e) {
  return isRightButton(e) || (e.ctrlKey && isLeftButton(e));
}

function sparePieces(ctrl, color, orientation, position) {
  return m('div', {
    class: ['spare', position, 'orientation-' + orientation, color].join(' ')
  }, ['pointer', 'king', 'queen', 'rook', 'bishop', 'knight', 'pawn', 'trash'].map(function(role) {
    var piece = ((['pointer', 'trash'].indexOf(role) === -1) ? color + ' ' : '') + role;
    var pieceElement = {
      class: piece,
    };
    var containerClass = 'no-square' +
      ((ctrl.vm.selected() === piece && !ctrl.vm.draggingSpare()) ? ' selected-square' : '');

    if (piece === 'trash') {
      pieceElement['data-icon'] = 'q';
      containerClass += ' trash';
    } else {
      pieceElement['data-color'] = color;
      pieceElement['data-role'] = role;
    }

    return m('div', {
        class: containerClass,
        onmousedown: function() {
          if (['pointer', 'trash'].indexOf(piece) !== -1) {
            ctrl.vm.selected(piece);
          } else {
            var listener;
            ctrl.vm.draggingSpare(true);
            ctrl.vm.selected('pointer');

            document.addEventListener('mouseup', listener = function() {
              ctrl.vm.selected(piece);
              ctrl.vm.draggingSpare(false);
              m.redraw();
              document.removeEventListener('mouseup', listener);
            });
          }
        }
      }, m('piece', pieceElement)
    );
  }));
}

var eventNames = ['mousedown', 'touchstart'];

module.exports = function(ctrl) {
  var fen = ctrl.computeFen();
  var color = ctrl.chessground.data.orientation;
  var opposite = color === 'white' ? 'black' : 'white';
  var sparePieceSelected = ctrl.vm.selected();
  var selectedParts = sparePieceSelected.split(' ');
  var cursorName = selectedParts[0] + ((selectedParts.length >= 2) ? '-' + selectedParts[1] : '');
  // http://www.cursors-4u.com
  var cursor = (cursorName === 'pointer') ?
    cursorName : 'url(/assets/cursors/' + cursorName + '.cur), default !important';

  ctrl.chessground.sparePieceSelected = sparePieceSelected;

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
    },
    style: 'cursor: ' + ((cursor) ? cursor : 'pointer'),
    onmousedown: function(data) {
      if (
        ['pointer', 'trash'].indexOf(ctrl.vm.selected()) === -1 &&
          isRightClick(data)
      ) {
        var selectedParts = ctrl.vm.selected().split(' ');

        if (selectedParts.length >= 2) {
          if (selectedParts[0] === 'white') {
            selectedParts[0] = 'black';
          } else if (selectedParts[0] === 'black') {
            selectedParts[0] = 'white';
          }

          ctrl.vm.selected(selectedParts.join(' '));
        }
      }
    }
  }, [
    sparePieces(ctrl, opposite, color, 'top'),
    chessground.view(ctrl.chessground),
    sparePieces(ctrl, color, color, 'bottom'),
    controls(ctrl, fen),
    inputs(ctrl, fen)
  ]);
};
