var draughtsground = require('./draughtsground');
var dragNewPiece = require('draughtsground/drag').dragNewPiece;
var eventPosition = require('draughtsground/util').eventPosition;
var resizeHandle = require('common/resize').default;
var editor = require('./editor');
var m = require('mithril');

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
    ctrl.trans.noarg('studyMenu'))
  ]);
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
  };
  var variant2option = function(key, name) {
    return {
      tag: 'option',
      attrs: {
        value: key,
        selected: ctrl.data.variant == key
      },
      children: [name]
    };
  };
  var looksLegit = ctrl.positionLooksLegit();
  var puzzleVariant = (ctrl.data.variant === 'standard' || ctrl.data.variant === 'frisian');
  return m('div.board-editor__tools', [
    ctrl.embed ? null : m('div', [
      ctrl.data.positions ? m('select.positions', {
        onchange: function(e) {
          ctrl.loadNewFen(e.target.value);
        }
      }, [
        optgroup(ctrl.trans.noarg('setTheBoard'), [
          currentPosition ? null : m('option', {
            value: fen,
            selected: true
          }, '- ' + ctrl.trans.noarg('boardEditor') + ' -'),
          ctrl.extraPositions.map(position2option)
        ])/*,
        optgroup(ctrl.trans.noarg('popularOpenings'),
          ctrl.data.positions.map(position2option)
        )*/
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
          }, ctrl.trans.noarg(key));
        }))
      ),
      ctrl.embed ? null : m('div.variant', [
        m('strong', ctrl.trans.noarg('variant')),
        m('div', [
          m('select', {
            onchange: function(e) {
              ctrl.changeVariant(e.target.value);
            }
          }, [
            variant2option('standard', 'Standard'),
            variant2option('frisian', 'Frisian'),
            variant2option('frysk', 'Frysk!'),
            variant2option('antidraughts', 'Antidraughts'),
            variant2option('breakthrough', 'Breakthrough')
          ])
        ])
      ])
    ]),
    ctrl.embed ? m('div.actions', [
      m('a.button.button-empty', {
        onclick: ctrl.startPosition
      }, ctrl.trans.noarg('startPosition')),
      m('a.button.button-empty', {
        onclick: ctrl.clearBoard
      }, ctrl.trans.noarg('clearBoard'))
    ]) : [
      m('div.actions', [
        m('a.button.button-empty.text[data-icon=B]', {
          onclick: function() {
            ctrl.draughtsground.toggleOrientation();
          }
        }, ctrl.trans.noarg('flipBoard')),
        looksLegit ? m('a.button.button-empty.text[data-icon="A"]', {
          href: editor.makeUrl('/analysis/' + (ctrl.data.variant !== 'standard' ? ctrl.data.variant + '/' : ''), fen),
          rel: 'nofollow'
        }, ctrl.trans.noarg('analysis')) : m('span.button.button-empty.disabled.text[data-icon="A"]', {
          rel: 'nofollow'
        }, ctrl.trans.noarg('analysis')),
        ctrl.data.puzzleEditor ? ((looksLegit && puzzleVariant) ? m('a.button.button-empty.text[data-icon="-"]', {
          href: editor.makeUrl('/analysis/puzzle/' + (ctrl.data.variant !== 'standard' ? ctrl.data.variant + '/' : ''), fen),
          rel: 'nofollow'
        }, 'Puzzle editor')  : m('span.button.button-empty.disabled.text[data-icon="-"]', {
          rel: 'nofollow'
        }, 'Puzzle editor')) : null,
        m('a.button.button-empty', {
          class: (looksLegit && ctrl.data.variant === 'standard') ? '' : 'disabled',
          onclick: function() {
            if (ctrl.positionLooksLegit() && ctrl.data.variant === 'standard') $.modal($('.continue-with'));
          }
        },
          m('span.text[data-icon=U]', ctrl.trans.noarg('continueFromHere'))),
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
        value: editor.makeUrl(ctrl.data.baseUrl + (ctrl.data.variant !== 'standard' ? ctrl.data.variant + '/' : ''), fen)
      })
    ])
  ]);
}

// can be 'pointer', 'trash', or [color, role]
function selectedToClass(s) {
  return (s === 'pointer' || s === 'trash' || s === 'empty') ? s : s.join(' ');
}

var lastTouchMovePos;

function sparePieces(ctrl, color, orientation, position) {

  var selectedClass = selectedToClass(ctrl.selected());

  var opposite = color === 'white' ? 'black' : 'white';
  var pieces = [[color, 'king'], [color, 'man'], 'empty', 'empty', [opposite, 'man'], [opposite, 'king']];

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
            !ctrl.draughtsground ||
            !ctrl.draughtsground.state.draggable.current ||
            !ctrl.draughtsground.state.draggable.current.newPiece
          ) &&
          (!Array.isArray(s) || s[0] !== '')
        ) ?
        ' selected-square' : ''
      );
    if (s === 'empty') {
      containerClass += ' empty';
    } else if (s === 'pointer') {
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
    if (s === 'empty') {
      return;
    } if (['pointer', 'trash'].includes(s)) {
      ctrl.selected(s);
    } else {
      ctrl.selected('pointer');

      dragNewPiece(ctrl.draughtsground.state, {
        color: s[0],
        role: s[1]
      }, e, true);

      document.addEventListener(upEvent, function(e) {
        var eventPos = eventPosition(e) || lastTouchMovePos;

        if (eventPos && ctrl.draughtsground.getKeyAtDomPos(eventPos)) {
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
  var url = lidraughts.assetUrl('cursors/' + name + '.cur');

  return 'url(' + url + '), default !important';
}

module.exports = function(ctrl) {
  var fen = ctrl.computeFen();
  var color = ctrl.bottomColor();

  return m('div.board-editor', {
    style: 'cursor: ' + makeCursor(ctrl.selected())
  }, [
    sparePieces(ctrl, color, 'black', 'top'),
    m('div.main-board', [
      draughtsground(ctrl),
      m('div.board-resize', {
        config: function(el, isUpdate) {
          if (!isUpdate) resizeHandle(el);
        }
      })
    ]),
    controls(ctrl, fen),
    inputs(ctrl, fen)
  ]);
};
