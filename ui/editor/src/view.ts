import { dragNewPiece } from 'shogiground/drag';
import { MouchEvent } from 'shogiground/types';
import { eventPosition, opposite, samePiece } from 'shogiground/util';
import { parseSfen } from 'shogiops/sfen';
import { Piece, Role, Rules } from 'shogiops/types';
import { allRoles, handRoles } from 'shogiops/variant/util';
import { VNode, h } from 'snabbdom';
import EditorCtrl from './ctrl';
import { EditorState, OpeningPosition, Selected } from './interfaces';
import * as ground from './shogiground';

function pieceCounter(ctrl: EditorCtrl): VNode {
  function singlePieceCounter(cur: number, total: number, name: string, suffix: string = ''): VNode {
    return h('span', [`${cur.toString()}/${total.toString()}`, h('strong', ` ${name}`), `${suffix}`]);
  }
  const pieceCount: [Role, number, string][] =
    ctrl.rules === 'minishogi'
      ? [
          ['pawn', 2, '歩(P)'],
          ['silver', 2, '銀(S)'],
          ['gold', 2, '金(G)'],
          ['bishop', 2, '角(B)'],
          ['rook', 2, '飛(R)'],
          ['rook', 2, '玉(K)'],
        ]
      : [
          ['pawn', 18, '歩(P)'],
          ['lance', 4, '香(L)'],
          ['knight', 4, '桂(N)'],
          ['silver', 4, '銀(S)'],
          ['gold', 4, '金(G)'],
          ['bishop', 2, '角(B)'],
          ['rook', 2, '飛(R)'],
          ['king', 2, '玉(K)'],
        ];
  return h('div.piece-counter', {}, [
    h(
      'div.piece-count',
      pieceCount.map((s, i) =>
        singlePieceCounter(ctrl.countPieces(s[0]), s[1], s[2], pieceCount.length > i + 1 ? ', ' : '')
      )
    ),
  ]);
}

function optgroup(name: string, opts: VNode[]): VNode {
  return h('optgroup', { attrs: { label: name } }, opts);
}

function studyButton(ctrl: EditorCtrl, state: EditorState): VNode {
  return h(
    'form',
    {
      attrs: {
        method: 'post',
        action: '/study/as',
      },
    },
    [
      h('input', {
        attrs: {
          type: 'hidden',
          name: 'orientation',
          value: ctrl.bottomColor(),
        },
      }),
      h('input', {
        attrs: { type: 'hidden', name: 'variant', value: ctrl.rules },
      }),
      h('input', {
        attrs: { type: 'hidden', name: 'sfen', value: state.legalSfen || '' },
      }),
      h(
        'button',
        {
          attrs: {
            type: 'submit',
            'data-icon': '4',
            disabled: !state.legalSfen,
          },
          class: {
            button: true,
            'button-empty': true,
            text: true,
            disabled: !state.legalSfen,
          },
        },
        ctrl.trans.noarg('toStudy')
      ),
    ]
  );
}

function variant2option(key: Rules, name: string, ctrl: EditorCtrl): VNode {
  return h(
    'option',
    {
      attrs: {
        value: key,
        selected: key == ctrl.rules,
      },
    },
    `${ctrl.trans.noarg('variant')} | ${name}`
  );
}

const allVariants: Array<[Rules, string]> = [
  ['standard', 'Standard'],
  ['minishogi', 'Minishogi'],
  ['chushogi', 'Chushogi'],
];

function controls(ctrl: EditorCtrl, state: EditorState): VNode {
  const position2option = function (pos: OpeningPosition): VNode {
    return h(
      'option',
      {
        attrs: {
          value: pos.sfen,
          'data-sfen': pos.sfen,
        },
      },
      pos.japanese ? `${pos.japanese} ${pos.english}` : pos.english
    );
  };
  return h('div.board-editor__tools', [
    ...(ctrl.data.embed || !ctrl.data.positions
      ? []
      : [
          h('div', [
            h(
              'select.positions',
              {
                props: {
                  value: state.sfen.split(' ').slice(0, 4).join(' '),
                },
                on: {
                  change(e) {
                    const el = e.target as HTMLSelectElement;
                    let value = el.selectedOptions[0].getAttribute('data-sfen');
                    if (value === 'prompt') value = (prompt('Paste SFEN') || '').trim();
                    if (value === 'start') ctrl.startPosition();
                    else if (!value || !ctrl.setSfen(value)) el.value = '';
                  },
                },
              },
              [
                optgroup(ctrl.trans.noarg('setTheBoard'), [
                  h(
                    'option',
                    {
                      attrs: {
                        selected: true,
                      },
                    },
                    `- ${ctrl.trans.noarg('boardEditor')}  -`
                  ),
                  ...ctrl.extraPositions.map(position2option),
                ]),
                ctrl.rules === 'standard' ? optgroup('Handicaps', ctrl.data.positions.map(position2option)) : null,
              ]
            ),
          ]),
        ]),
    h('div.metadata', [
      h(
        'div.color',
        h(
          'select',
          {
            on: {
              change(e) {
                ctrl.setTurn((e.target as HTMLSelectElement).value as Color);
              },
            },
          },
          ['blackPlays', 'whitePlays'].map(function (key) {
            return h(
              'option',
              {
                attrs: {
                  value: key[0] == 'b' ? 'sente' : 'gote',
                  selected: (ctrl.turn === 'sente' ? 'b' : 'w') === key[0],
                },
              },
              ctrl.trans(key)
            );
          })
        )
      ),
      ctrl.data.embed || ctrl.rules === 'chushogi' ? '' : pieceCounter(ctrl),
    ]),
    ...(ctrl.data.embed
      ? [
          h('div.actions', [
            h(
              'a.button.button-empty',
              {
                on: {
                  click() {
                    ctrl.startPosition();
                  },
                },
              },
              ctrl.trans.noarg('startPosition')
            ),
            h(
              'a.button.button-empty',
              {
                on: {
                  click() {
                    ctrl.clearBoard();
                  },
                },
              },
              ctrl.trans.noarg('clearBoard')
            ),
            h(
              'a.button.button-empty',
              {
                class: {
                  disabled: !ctrl.canFillGoteHand(),
                },
                on: {
                  click() {
                    ctrl.fillGotesHand();
                  },
                },
              },
              ctrl.trans.noarg('fillGotesHand')
            ),
          ]),
        ]
      : [
          h('div', [
            h(
              'select',
              {
                attrs: { id: 'variants' },
                on: {
                  change(e) {
                    ctrl.setRules((e.target as HTMLSelectElement).value as Rules);
                  },
                },
              },
              allVariants.map(x => variant2option(x[0], x[1], ctrl))
            ),
          ]),
          h('div.actions', [
            h(
              'a.button.button-empty.text',
              {
                attrs: { 'data-icon': 'q' },
                on: {
                  click() {
                    ctrl.clearBoard();
                  },
                },
              },
              ctrl.trans.noarg('clearBoard')
            ),
            h(
              'a.button.button-empty.text',
              {
                attrs: { 'data-icon': 'N' },
                class: {
                  disabled: !ctrl.canFillGoteHand(),
                },
                on: {
                  click() {
                    ctrl.fillGotesHand();
                  },
                },
              },
              ctrl.trans.noarg('fillGotesHand')
            ),
            h(
              'a.button.button-empty.text',
              {
                attrs: { 'data-icon': 'B' },
                on: {
                  click() {
                    ctrl.setOrientation(opposite(ctrl.shogiground.state.orientation));
                  },
                },
              },
              `${ctrl.trans.noarg('flipBoard')} - (${ctrl.trans(ctrl.shogiground.state.orientation)} POV)`
            ),
            h(
              'a',
              {
                attrs: {
                  'data-icon': 'A',
                  rel: 'nofollow',
                  ...(state.legalSfen ? { href: ctrl.makeAnalysisUrl(state.legalSfen, ctrl.bottomColor()) } : {}),
                },
                class: {
                  button: true,
                  'button-empty': true,
                  text: true,
                  disabled: !state.legalSfen,
                },
              },
              ctrl.trans.noarg('analysis')
            ),
            h(
              'a',
              {
                class: {
                  button: true,
                  'button-empty': true,
                  disabled: !state.playable,
                },
                on: {
                  click: () => {
                    if (state.playable) $.modal($('.continue-with'));
                  },
                },
              },
              [h('span.text', { attrs: { 'data-icon': 'U' } }, ctrl.trans.noarg('continueFromHere'))]
            ),
            studyButton(ctrl, state),
          ]),
          h('div.continue-with.none', [
            h(
              'a.button',
              {
                attrs: {
                  href:
                    '/?sfen=' +
                    ctrl.encodeSfen(state.legalSfen || '') +
                    `&variant=${ctrl.rules === 'standard' ? '1' : '2'}` +
                    '#ai',
                  rel: 'nofollow',
                },
              },
              ctrl.trans.noarg('playWithTheMachine')
            ),
            h(
              'a.button',
              {
                attrs: {
                  href:
                    '/?sfen=' +
                    ctrl.encodeSfen(state.legalSfen || '') +
                    `&variant=${ctrl.rules === 'standard' ? '1' : '2'}` +
                    '#friend',
                  rel: 'nofollow',
                },
              },
              ctrl.trans.noarg('playWithAFriend')
            ),
          ]),
        ]),
  ]);
}

function inputs(ctrl: EditorCtrl, sfen: string): VNode | undefined {
  if (ctrl.data.embed) return;
  return h('div.copyables', [
    h('p', [
      h('strong', 'SFEN'),
      h('input.copyable', {
        attrs: {
          spellcheck: false,
        },
        props: {
          value: sfen,
        },
        on: {
          change(e) {
            const el = e.target as HTMLInputElement;
            ctrl.setSfen(el.value.trim());
            el.reportValidity();
          },
          input(e) {
            const el = e.target as HTMLInputElement;
            const valid = parseSfen(ctrl.rules, el.value.trim()).isOk;
            el.setCustomValidity(valid ? '' : 'Invalid SFEN');
          },
          blur(e) {
            const el = e.target as HTMLInputElement;
            el.value = ctrl.getSfen();
            el.setCustomValidity('');
          },
        },
      }),
    ]),
    h('p', [
      h('strong.name', 'URL '), // en space
      h('input.copyable.autoselect', {
        attrs: {
          readonly: true,
          spellcheck: false,
          value: ctrl.makeEditorUrl(sfen),
        },
      }),
    ]),
  ]);
}

// can be 'pointer', 'trash', or [color, role]
function selectedToClass(s: Selected): string {
  return s === 'pointer' || s === 'trash' ? s : s.join(' ');
}

function sparePieces(ctrl: EditorCtrl, color: Color, _orientation: Color, position: 'top' | 'bottom'): VNode {
  const selectedClass = selectedToClass(ctrl.selected());

  const pieces = allRoles(ctrl.rules).map(function (role) {
    return [color, role];
  });

  return h(
    'div',
    {
      attrs: {
        class: ['spare', 'spare-' + position, 'spare-' + color].join(' '),
      },
    },
    [...pieces, 'trash', 'pointer'].map((s: Selected) => {
      const className = selectedToClass(s);
      const attrs = {
        class: className,
        ...(s !== 'pointer' && s !== 'trash'
          ? {
              'data-color': s[0],
              'data-role': s[1],
            }
          : {}),
      };
      return h(
        'div',
        {
          class: {
            'no-square': true,
            pointer: s === 'pointer',
            trash: s === 'trash',
            'selected-square': selectedClass === className,
          },
          on: {
            mousedown: selectSPStart(ctrl, s),
            mouseup: selectSPEnd(ctrl, s),
            touchstart: selectSPStart(ctrl, s),
            touchend: selectSPEnd(ctrl, s),
          },
        },
        [h('div', [h('piece', { attrs })])]
      );
    })
  );
}

function selectSPStart(ctrl: EditorCtrl, s: Selected): (e: MouchEvent) => void {
  return function (e: MouchEvent): void {
    e.preventDefault();
    ctrl.shogiground.selectPiece(null);
    ctrl.initTouchMovePos = ctrl.lastTouchMovePos = undefined;
    if (typeof s !== 'string') {
      const piece = {
        color: s[0],
        role: s[1],
      };
      dragNewPiece(ctrl.shogiground.state, piece, e, true);
    }
    ctrl.redraw();
  };
}

function selectSPEnd(ctrl: EditorCtrl, s: Selected): (e: MouchEvent) => void {
  return function (e: MouchEvent): void {
    e.preventDefault();
    const cur = ctrl.selected(),
      pos = eventPosition(e) || ctrl.lastTouchMovePos;
    ctrl.shogiground.selectPiece(null);
    // default to pointer if we click on selected
    if (
      cur === s ||
      (typeof cur !== 'string' &&
        typeof s !== 'string' &&
        samePiece({ color: cur[0], role: cur[1] }, { color: s[0], role: s[1] }))
    ) {
      ctrl.selected('pointer');
    } else if (
      !pos ||
      !ctrl.initTouchMovePos ||
      (Math.abs(pos[0] - ctrl.initTouchMovePos[0]) < 20 && Math.abs(pos[1] - ctrl.initTouchMovePos[1]) < 20)
    ) {
      ctrl.selected(s);
      ctrl.shogiground.state.selectable.deleteOnTouch = s === 'trash' ? true : false;
    }
    ctrl.redraw();
  };
}

function createHandHelpers(ctrl: EditorCtrl, position: 'top' | 'bottom'): [VNode, VNode] {
  function getPiece(e: Event): Piece | undefined {
    const role: Role | undefined = ((e.target as HTMLElement).dataset.role as Role) || undefined;
    if (role) {
      const color: Color =
        (e.target as HTMLElement).dataset.pos === 'bottom'
          ? ctrl.shogiground.state.orientation
          : opposite(ctrl.shogiground.state.orientation);
      return { role, color };
    }
    return;
  }

  const removePiece = (e: Event) => {
    const piece = getPiece(e);
    if (piece) {
      ctrl.removeFromHand(piece.color, piece.role, true);
    }
  };
  const addPiece = (e: Event) => {
    const piece = getPiece(e);
    if (piece) {
      ctrl.addToHand(piece.color, piece.role, true);
    }
  };
  const pluses: VNode[] = [];
  const minuses: VNode[] = [];

  const reversedRoles = handRoles(ctrl.rules).reverse();
  for (const r of reversedRoles) {
    pluses.push(h('div.plus', { attrs: { 'data-role': r, 'data-pos': position } }));
    minuses.push(h('div.minus', { attrs: { 'data-role': r, 'data-pos': position } }));
  }
  return [
    h('div.pluses', { on: { click: addPiece } }, pluses),
    h('div.minuses', { on: { click: removePiece } }, minuses),
  ];
}

function createHandWrap(ctrl: EditorCtrl, position: 'top' | 'bottom'): VNode {
  const helpers = createHandHelpers(ctrl, position);
  return h(`div.editor-hand.pos-${position}`, [helpers[0], ground.renderHand(ctrl, position), helpers[1]]);
}

export default function (ctrl: EditorCtrl): VNode {
  const state = ctrl.getState();
  const color = ctrl.bottomColor();

  return h(`div.board-editor.variant-${ctrl.rules}`, [
    sparePieces(ctrl, opposite(color), color, 'top'),
    ctrl.rules === 'chushogi' ? null : createHandWrap(ctrl, 'top'),
    h('div.main-board', [ground.renderBoard(ctrl)]),
    ctrl.rules === 'chushogi' ? null : createHandWrap(ctrl, 'bottom'),
    sparePieces(ctrl, color, color, 'bottom'),
    controls(ctrl, state),
    inputs(ctrl, state.sfen),
  ]);
}
