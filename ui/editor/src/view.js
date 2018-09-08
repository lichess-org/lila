var draughtsground = require('./draughtsground');
var dragNewPiece = require('draughtsground/drag').dragNewPiece;
var eventPosition = require('draughtsground/util').eventPosition;
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
    m('button.button.text', {
      type: 'submit',
      'data-icon': '4',
      disabled: !ctrl.positionLooksLegit()
    },
    'Study')
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
  return m('div.editor-side', [
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
        ])/*,
        optgroup(ctrl.trans('popularOpenings'),
          ctrl.data.positions.map(position2option)
        )*/
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
      m('div.variant', [
        m('strong', ctrl.trans('variant')),
        m('div', [
          m('select', {
            onchange: function(e) {
              ctrl.changeVariant(e.target.value);
            }
          }, [
            variant2option('standard', 'Standard'),
            variant2option('frisian', 'Frisian'),
            variant2option('antidraughts', 'Antidraughts')
          ])
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
    ]) : [
      m('div', [
        m('a.button.text[data-icon=B]', {
          onclick: function() {
            ctrl.draughtsground.toggleOrientation();
          }
        }, ctrl.trans('flipBoard')),
        looksLegit ? m('a.button.text[data-icon="A"]', {
          href: editor.makeUrl('/analysis/' + (ctrl.data.variant !== 'standard' ? ctrl.data.variant + '/' : ''), fen),
          rel: 'nofollow'
        }, ctrl.trans('analysis')) : m('span.button.disabled.text[data-icon="A"]', {
          rel: 'nofollow'
        }, ctrl.trans('analysis')),
        m('a.button', {
          class: (looksLegit && ctrl.data.variant === 'standard') ? '' : 'disabled',
          onclick: function() {
            if (ctrl.positionLooksLegit() && ctrl.data.variant === 'standard') $.modal($('.continue_with'));
          }
        },
        m('span.text[data-icon=U]', ctrl.trans('continueFromHere'))),
        studyButton(ctrl, fen)
      ]),
      m('div.continue_with', [
        /*m('a.button', {
          href: '/?fen=' + fen + '#ai',
          rel: 'nofollow'
        }, ctrl.trans('playWithTheMachine')),
        m('br'),*/
        m('a.button', {
          href: '/?fen=' + fen + '#friend',
          rel: 'nofollow'
        }, ctrl.trans('playWithAFriend'))
      ])
    ]
  ]);
}

function inputs(ctrl, fen) {
  if (ctrl.embed) return;
  if (ctrl.vm.redirecting) return m.trust(lidraughts.spinnerHtml);
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
        value: editor.makeUrl(ctrl.data.baseUrl + (ctrl.data.variant !== 'standard' ? ctrl.data.variant + '/' : ''), fen)
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

  var selectedClass = selectedToClass(ctrl.vm.selected());

  var opposite = color === 'white' ? 'black' : 'white';
  var pieces = [[color, 'king'], [color, 'man'], ['', ''], ['', ''], [opposite, 'man'], [opposite, 'king']];

  return m('div', {
    class: ['spare', position, orientation].join(' ')
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

      if (s === 'trash') {
        attrs['data-icon'] = 'q';
        containerClass += ' trash';
      } else if (s !== 'pointer') {
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
      }, m('piece', attrs));
  }));
}

function onSelectSparePiece(ctrl, s, upEvent) {
  return function(e) {
    if (['pointer', 'trash'].indexOf(s) !== -1) {
      ctrl.vm.selected(s);
    } else {
      ctrl.vm.selected('pointer');

      if (e.type === 'touchstart') {
        e.preventDefault();
      }

      dragNewPiece(ctrl.draughtsground.state, {
        color: s[0],
        role: s[1]
      }, e, true);

      document.addEventListener(upEvent, function(e) {
        var eventPos = eventPosition(e) || lastTouchMovePos;

        if (eventPos && ctrl.draughtsground.getKeyAtDomPos(eventPos)) {
          ctrl.vm.selected('pointer');
        } else {
          ctrl.vm.selected(s);
        }
        m.redraw();
      }, {once: true});
    }
  };
}

function makeCursor(selected) {

  if (selected === 'pointer') return 'pointer';

  var name = selected === 'trash' ? 'trash' : selected.join('-');
  var url = lidraughts.assetUrl('/assets/cursors/' + name + '.cur');

  return 'url(' + url + '), default !important';
}

var eventNames = ['mousedown', 'touchstart'];

module.exports = function(ctrl) {
  var fen = ctrl.computeFen();
  var color = ctrl.bottomColor();

  return m('div.editor', {
    style: 'cursor: ' + makeCursor(ctrl.vm.selected())
  }, [
    sparePieces(ctrl, color, 'black', 'top'),
    draughtsground(ctrl),
    controls(ctrl, fen),
    inputs(ctrl, fen)
  ]);
};
