var chessground = require('./chessground');
var dragNewPiece = require('chessground/drag').dragNewPiece;
var editor = require('./editor');
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
        onclick: function() {
          ctrl.chessground.toggleOrientation();
        }
      }, ctrl.trans('flipBoard')),
      ctrl.positionLooksLegit() ? m('a.button.text[data-icon="A"]', {
        href: editor.makeUrl('/analysis/', fen),
        rel: 'nofollow'
      }, ctrl.trans('analysis')) : m('span.button.disabled.text[data-icon="A"]', {
        rel: 'nofollow'
      }, ctrl.trans('analysis')),
      ctrl.positionLooksLegit() ? m('a.button.text[data-icon=U]', {
          onclick: function() {
            $.ajax({
              url: '/setup/validate-fen?fen=' + fen,
              method: 'get',
              success: function(data) {
                $.ajax({
                  url: '/setup/validate-fen?fen=' + fen + '&strict=1',
                  method: 'get',
                  success: function(data) {
                    $.modal($('.continue_with#all'));
                  },
                  error: function(error) {
                    $.modal($('.continue_with#friend'));
                  }
                });
              },
              error: function(error) {
                alert(ctrl.trans('positionUnplayable'));
              }
            });
          }
        }, ctrl.trans('continueFromHere')) : m('span.button.disabled.text[data-icon="U"]', {
        }, ctrl.trans('continueFromHere'))
    ]),
    ctrl.embed ? null : m('div.continue_with#all', [
      m('a.button', {
        href: '/?fen=' + fen + '#ai',
        rel: 'nofollow'
      }, ctrl.trans('playWithTheMachine')),
      m('br'),
      m('a.button', {
        href: '/?fen=' + fen + '#friend',
        rel: 'nofollow'
      }, ctrl.trans('playWithAFriend'))
    ]),
    ctrl.embed ? null : m('div.continue_with#friend', [
      m('a.button', {
        href: '/?fen=' + fen + '#friend',
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

// can be 'pointer', 'trash', or [color, role]
function selectedToClass(s) {
  return (s === 'pointer' || s === 'trash') ? s : s.join(' ');
}

function sparePieces(ctrl, color, orientation, position) {

  var selectedClass = selectedToClass(ctrl.vm.selected());

  var pieces = ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].map(function(role) {
    return [color, role];
  });

  return m('div', {
    class: ['spare', position, 'orientation-' + orientation, color].join(' ')
  }, ['pointer'].concat(pieces).concat('trash').map(function(s) {

    var className = selectedToClass(s);

    var attrs = {
      class: className
    };

    var containerClass = 'no-square' +
      (
        (
          selectedClass === className &&
            (
              !ctrl.chessground ||
                !ctrl.chessground.state.draggable.current ||
                !ctrl.chessground.state.draggable.current.newPiece
            )
        ) ?
          ' selected-square' : ''
      );

    if (s === 'trash') {
      attrs['data-icon'] = 'q';
      containerClass += ' trash';
    } else if (s !== 'pointer') {
      attrs['data-color'] = s[0];
      attrs['data-role'] = s[1];
    }

    return m('div', {
      class: containerClass,
      onmousedown: function(e) {
        if (['pointer', 'trash'].indexOf(s) !== -1) {
          ctrl.vm.selected(s);
        } else {
          ctrl.vm.selected('pointer');

          dragNewPiece(ctrl.chessground.state, {
            color: s[0],
            role: s[1]
          }, e, true);

          document.addEventListener('mouseup', function() {
            ctrl.vm.selected(s);
            m.redraw();
          }, {once: true});
        }
      }
    }, m('piece', attrs));
  }));
}

function makeCursor(selected) {

  if (selected === 'pointer') return 'pointer';

  var name = selected === 'trash' ? 'trash' : selected.join('-');
  var url = lichess.assetUrl('/assets/cursors/' + name + '.cur');

  return 'url(' + url + '), default !important';
}

var eventNames = ['mousedown', 'touchstart'];

module.exports = function(ctrl) {
  var fen = ctrl.computeFen();
  var color = ctrl.bottomColor();
  var opposite = color === 'white' ? 'black' : 'white';

  return m('div.editor', {
    style: 'cursor: ' + makeCursor(ctrl.vm.selected())
  }, [
    sparePieces(ctrl, opposite, color, 'top'),
    chessground(ctrl),
    sparePieces(ctrl, color, color, 'bottom'),
    controls(ctrl, fen),
    inputs(ctrl, fen)
  ]);
};
