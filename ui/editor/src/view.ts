import { dragNewPiece } from 'chessground/drag';
import { eventPosition, opposite } from 'chessground/util';
import EditorCtrl from './ctrl';
import chessground from './chessground';
import * as editor from './editor';
import { Position } from './interfaces';

import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

function castleCheckBox(ctrl: EditorCtrl, id, label, reversed: boolean): VNode {
  const input = h('input', {
    attrs: {
      type: 'checkbox',
      checked: ctrl.data.castles[id](),
    },
    on: {
      change(e) {
        ctrl.setCastle(id, (e.target as HTMLInputElement).checked);
      }
    }
  });
  return h('label', reversed ? [input, label] : [label, input]);
}

function optgroup(name: string, opts: VNode[]): VNode {
  return h('optgroup', {
    attrs: {
      label: name
    }
  }, opts);
}

function studyButton(ctrl: EditorCtrl, fen: string): VNode {
  return h('form', {
    attrs: {
      method: 'post',
      action: '/study/as'
    }
  }, [
    h('input', {
      attrs: {
        type: 'hidden',
        name: 'orientation',
        value: ctrl.bottomColor()
      }
    }),
    h('input', {
      attrs: {
        type: 'hidden',
        name: 'variant',
        value: ctrl.data.variant
      }
    }),
    h('input', {
      attrs: {
        type: 'hidden',
        name: 'fen',
        value: fen
      }
    }),
    h('button', {
      attrs: {
        type: 'submit',
        'data-icon': '4',
        disabled: !ctrl.positionLooksLegit(),
        class: `button button-empty text ${ctrl.positionLooksLegit() ? '' : 'disabled'}`
      }
    }, 'Study')
  ]);
}

function variant2option(key: string, name: string, ctrl: EditorCtrl): VNode {
  return h('option', {
    attrs: {
      value: key,
      selected: key == ctrl.data.variant
    },
  }, `${ctrl.trans.noarg('variant')} | ${name}`);
}

function controls(ctrl: EditorCtrl, fen: string): VNode {
  const positionIndex = ctrl.positionIndex[fen.split(' ')[0]];
  const currentPosition = ctrl.data.positions && positionIndex !== -1 ? ctrl.data.positions[positionIndex] : null;
  const position2option = function(pos: Position): VNode {
    return h('option', {
      attrs: {
        value: pos.fen,
        selected: currentPosition && currentPosition.fen === pos.fen
      }
    }, pos.eco ? pos.eco + ' ' + pos.name : pos.name);
  };
  const selectedVariant = ctrl.data.variant;
  const looksLegit = ctrl.positionLooksLegit();
  return h('div.board-editor__tools', [
    ctrl.embed ? null : h('div', [
      ctrl.data.positions ? h('select.positions', {
        on: {
          change(e) {
            ctrl.loadNewFen((e.target as HTMLSelectElement).value);
          }
        }
      }, [
        optgroup(ctrl.trans.noarg('setTheBoard'), [
          ...(currentPosition ? [] : [h('option', {
            value: fen,
            selected: true
          }, `- ${ctrl.trans.noarg('boardEditor')}  -`)]),
          ...ctrl.extraPositions.map(position2option)
        ]),
        optgroup(ctrl.trans.noarg('popularOpenings'),
          ctrl.data.positions.map(position2option)
        )
      ]) : null
    ]),
    h('div.metadata', [
      h('div.color',
        h('select', {
          on: {
            change(e) {
              ctrl.setColor((e.target as HTMLSelectElement).value as 'w' | 'b');
            }
          }
        }, ['whitePlays', 'blackPlays'].map(function(key) {
          return h('option', {
            attrs: {
              value: key[0],
              selected: ctrl.data.color() === key[0]
            }
          }, ctrl.trans(key));
        }))
      ),
      h('div.castling', [
        h('strong', ctrl.trans.noarg('castling')),
        h('div', [
          castleCheckBox(ctrl, 'K', ctrl.trans.noarg('whiteCastlingKingside'), ctrl.options.inlineCastling),
          castleCheckBox(ctrl, 'Q', 'O-O-O', true)
        ]),
        h('div', [
          castleCheckBox(ctrl, 'k', ctrl.trans.noarg('blackCastlingKingside'), ctrl.options.inlineCastling),
          castleCheckBox(ctrl, 'q', 'O-O-O', true)
        ])
      ])
    ]),
    ...(ctrl.embed ? [h('div.actions', [
      h('a.button.button-empty', {
        on: {
          click: ctrl.startPosition
        }
      }, ctrl.trans.noarg('startPosition')),
      h('a.button.button-empty', {
        on: {
          click: ctrl.clearBoard
        }
      }, ctrl.trans.noarg('clearBoard'))
    ])] : [
      h('div', [
        h('select', {
          attrs: {
            id: 'variants'
          },
          on: {
            change(e) {
              ctrl.changeVariant((e.target as HTMLSelectElement).value);
            }
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
      h('div.actions', [
        h('a.button.button-empty.text', {
          attrs: {
            'data-icon': 'B',
          },
          on: {
            click() {
              ctrl.chessground.toggleOrientation();
            }
          }
        }, ctrl.trans.noarg('flipBoard')),
        looksLegit ? h('a.button.button-empty.text', {
          attrs: {
            'data-icon': 'A',
            href: editor.makeUrl('/analysis/' + selectedVariant + '/', fen),
            rel: 'nofollow',
          }
        }, ctrl.trans.noarg('analysis')) : h('span.button.button-empty.disabled.text', {
          attrs: {
            'data-icon': 'A',
            rel: 'nofollow'
          }
        }, ctrl.trans.noarg('analysis')),
        h('a', {
          attrs: {
            class: `button button-empty ${(looksLegit && selectedVariant === 'standard') ? '' : 'disabled'}`,
          },
          on: {
            click: () => {
              if (ctrl.positionLooksLegit() && selectedVariant === 'standard') $.modal($('.continue-with'));
            }
          }
        }, [h('span.text', { attrs: { 'data-icon' : 'U' } }, ctrl.trans.noarg('continueFromHere'))]),
        studyButton(ctrl, fen)
      ]),
      h('div.continue-with.none', [
        h('a.button', {
          attrs: {
            href: '/?fen=' + fen + '#ai',
            rel: 'nofollow'
          }
        }, ctrl.trans.noarg('playWithTheMachine')),
        h('a.button', {
          attrs: {
            href: '/?fen=' + fen + '#friend',
            rel: 'nofollow'
          }
        }, ctrl.trans.noarg('playWithAFriend'))
      ])
    ])
  ]);
}

function inputs(ctrl: EditorCtrl, fen: string): VNode {
  if (ctrl.embed) return;
  return h('div.copyables', [
    h('p', [
      h('strong', 'FEN'),
      h('input.copyable.autoselect', {
        attrs: {
          spellcheck: false,
          value: fen,
        },
        on: {
          change(e) {
            const value = (e.target as HTMLInputElement).value;
            if (value !== fen) ctrl.changeFen(value);
          }
        }
      })
    ]),
    h('p', [
      h('strong.name', 'URL'),
      h('input.copyable.autoselect', {
        attrs: {
          readonly: true,
          spellcheck: false,
          value: editor.makeUrl(ctrl.data.baseUrl, fen)
        }
      })
    ])
  ]);
}

// can be 'pointer', 'trash', or [color, role]
function selectedToClass(s): string {
  return (s === 'pointer' || s === 'trash') ? s : s.join(' ');
}

var lastTouchMovePos;

function sparePieces(ctrl: EditorCtrl, color: Color, orientation: Color, position: 'top' | 'bottom'): VNode {
  const selectedClass = selectedToClass(ctrl.selected());

  const pieces = ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].map(function(role) {
    return [color, role];
  });

  return h('div', {
    attrs: {
      class: ['spare', 'spare-' + position, 'spare-' + color].join(' ')
    }
  }, ['pointer', ...pieces, 'trash'].map(function(s) {

    var className = selectedToClass(s);

    const attrs = {
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

    return h('div', {
      attrs: {
        class: containerClass,
      },
      on: {
        mousedown: onSelectSparePiece(ctrl, s, 'mouseup'),
        touchstart: onSelectSparePiece(ctrl, s, 'touchend'),
        touchmove: (e) => {
          lastTouchMovePos = eventPosition(e as any);
        }
      }
    }, [h('div', [h('piece', { attrs })])]);
  }));
}

function onSelectSparePiece(ctrl: EditorCtrl, s, upEvent) {
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
        ctrl.redraw();
      }, {once: true});
    }
  };
}

function makeCursor(selected): string {
  if (selected === 'pointer') return 'pointer';

  const name = selected === 'trash' ? 'trash' : selected.join('-');
  const url = window.lichess.assetUrl('cursors/' + name + '.cur');

  return `url('${url}'), default !important`;
}

export default function(ctrl: EditorCtrl) {
  const fen = ctrl.computeFen();
  const color = ctrl.bottomColor();

  return h('div.board-editor', {
    attrs: {
      style: 'cursor: ' + makeCursor(ctrl.selected())
    }
  }, [
    sparePieces(ctrl, opposite(color), color, 'top'),
    h('div.main-board', [chessground(ctrl)]),
    sparePieces(ctrl, color, color, 'bottom'),
    controls(ctrl, fen),
    inputs(ctrl, fen)
  ]);
}
