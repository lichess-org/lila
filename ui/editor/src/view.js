var chessground = require('./chessground');
var dragNewPiece = require('chessground/drag').dragNewPiece;
var eventPosition = require('chessground/util').eventPosition;
var resizeHandle = require('common/resize').default;
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

function studyButton(ctrl, fen) {
  return m('form', {
    method: 'post',
    action: '/study/as'
  }, [
    m('input[type=hidden][name=orientation]', {
      value: ctrl.bottomColor()
    }),
    m('input[type=hidden][name=variant]', {
      value: ctrl.data.variant
    }),
    m('input[type=hidden][name=fen]', {
      value: fen
    }),
    m('button.button.button-empty.text', {
      type: 'submit',
      'data-icon': '4',
      disabled: !ctrl.positionLooksLegit(),
      class: ctrl.positionLooksLegit() ? '' : 'disabled'
    },
      'Study')
  ]);
}

function variant2option(key, name, ctrl) {
  return {
    tag: 'option',
    attrs: {
      value: key,
      selected: key == ctrl.data.variant
    },
    children: [ctrl.trans('variant') + ' | ' + name]
  };
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
      children: [pos.eco ? pos.eco + ' ' + pos.name : pos.name]
    };
  };
  var selectedVariant = ctrl.data.variant;
  var looksLegit = ctrl.positionLooksLegit();
  return m('div.board-editor__tools', [
    ctrl.embed ? null : m('div', [
      ctrl.data.positions ? m('select.positions', {
        onchange: function(e) {
          ctrl.loadNewFen(e.target.value);
        }
      }, [
        optgroup(ctrl.trans('setTheBoard'), [
          currentPosition ? null : m('option', {
            value: fen,
            selected: true
          }, '- ' + ctrl.trans('boardEditor') + ' -'),
          ctrl.extraPositions.map(position2option)
        ]),
        optgroup(ctrl.trans('popularOpenings'),
          ctrl.data.positions.map(position2option)
        )
      ]) : null
    ]),
    m('div.metadata', [
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
          castleCheckBox(ctrl, 'Q', 'O-O-O', true)
        ]),
        m('div', [
          castleCheckBox(ctrl, 'k', ctrl.trans('blackCastlingKingside'), ctrl.options.inlineCastling),
          castleCheckBox(ctrl, 'q', 'O-O-O', true)
        ])
      ])
    ]),
    ctrl.embed ? m('div.actions', [
      m('a.button.button-empty', {
        onclick: ctrl.startPosition
      }, 'Initial position'),
      m('a.button.button-empty', {
        onclick: ctrl.clearBoard
      }, 'Empty board')
    ]) : [
      m('div', [
        m('select#variants', {
          onchange: function(e) {
            ctrl.changeVariant(e.target.value);
          }
        }, [
          ['standard', 'Standard'],
          ['antichess', 'Antichess'],
          ['atomic', 'Atomic'],
          ['crazyhouse', 'Crazyhouse'],
          ['horde', 'Horde'],
          ['kingOfTheHill', 'King of the Hill'],
          ['racingKings', 'Racing Kings'],
          ['threeCheck', 'Three-check']
        ].map(function(x) { return variant2option(x[0], x[1], ctrl) })
        )
      ]),
      m('div.actions', [
        m('a.button.button-empty.text[data-icon=B]', {
          onclick: function() {
            ctrl.chessground.toggleOrientation();
          }
        }, ctrl.trans('flipBoard')),
        looksLegit ? m('a.button.button-empty.text[data-icon="A"]', {
          href: editor.makeUrl('/analysis/' + selectedVariant + '/', fen),
          rel: 'nofollow'
        }, ctrl.trans('analysis')) : m('span.button.button-empty.disabled.text[data-icon="A"]', {
          rel: 'nofollow'
        }, ctrl.trans('analysis')),
        m('a.button.button-empty', {
          class: (looksLegit && selectedVariant === 'standard') ? '' : 'disabled',
          onclick: function() {
            if (ctrl.positionLooksLegit() && selectedVariant === 'standard') $.modal($('.continue-with'));
          }
        },
          m('span.text[data-icon=U]', ctrl.trans('continueFromHere'))),
        studyButton(ctrl, fen)
      ]),
      m('div.continue-with.none', [
        m('a.button', {
          href: '/?fen=' + fen + '#ai',
          rel: 'nofollow'
        }, ctrl.trans.noarg('playWithTheMachine')),
        m('a.button', {
          href: '/?fen=' + fen + '#friend',
          rel: 'nofollow'
        }, ctrl.trans.noarg('playWithAFriend'))
      ])
    ]
  ]);
}

function inputs(ctrl, fen) {
  if (ctrl.embed) return;
  return m('div.copyables', [
    m('p', [
      m('strong', 'FEN'),
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

var lastTouchMovePos;

function sparePieces(ctrl, color, orientation, position) {

  var selectedClass = selectedToClass(ctrl.selected());

  var pieces = ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].map(function(role) {
    return [color, role];
  });

  return m('div', {
    class: ['spare', 'spare-' + position, 'spare-' + color].join(' ')
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

    if (s === 'pointer') {
      containerClass += ' pointer';
    } else if (s === 'trash') {
      containerClass += ' trash';
    } else {
      attrs['data-color'] = s[0];
      attrs['data-role'] = s[1];
    }

    return m('div', {
      class: containerClass,
      onmousedown: onSelectSparePiece(ctrl, s, 'mouseup'),
      ontouchstart: onSelectSparePiece(ctrl, s, 'touchend'),
      ontouchmove: function(e) {
        lastTouchMovePos = eventPosition(e)
      }
    }, m('div', m('piece', attrs)));
  }));
}

function onSelectSparePiece(ctrl, s, upEvent) {
  return function(e) {
    e.preventDefault();
    if (['pointer', 'trash'].includes(s)) {
      ctrl.selected(s);
    } else {
      ctrl.selected('pointer');

      dragNewPiece(ctrl.chessground.state, {
        color: s[0],
        role: s[1]
      }, e, true);

      document.addEventListener(upEvent, function(e) {
        var eventPos = eventPosition(e) || lastTouchMovePos;

        if (eventPos && ctrl.chessground.getKeyAtDomPos(eventPos)) {
          ctrl.selected('pointer');
        } else {
          ctrl.selected(s);
        }
        m.redraw();
      }, {once: true});
    }
  };
}

function makeCursor(selected) {

  if (selected === 'pointer') return 'pointer';

  var name = selected === 'trash' ? 'trash' : selected.join('-');
  var url = lichess.assetUrl('cursors/' + name + '.cur');

  return 'url(' + url + '), default !important';
}

module.exports = function(ctrl) {
  var fen = ctrl.computeFen();
  var color = ctrl.bottomColor();
  var opposite = color === 'white' ? 'black' : 'white';

  return m('div.board-editor', {
    style: 'cursor: ' + makeCursor(ctrl.selected())
  }, [
    sparePieces(ctrl, opposite, color, 'top'),
    m('div.main-board', [
      chessground(ctrl),
      m('div.board-resize', {
        config: function(el, isUpdate) {
          if (!isUpdate) resizeHandle(el);
        }
      })
    ]),
    sparePieces(ctrl, color, color, 'bottom'),
    controls(ctrl, fen),
    inputs(ctrl, fen)
  ]);
};
