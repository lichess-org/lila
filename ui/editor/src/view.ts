import { standardColorName, transWithColorName } from 'common/colorName';
import { dragNewPiece } from 'shogiground/drag';
import { colors, MouchEvent } from 'shogiground/types';
import { eventPosition, opposite, samePiece } from 'shogiground/util';
import { findHandicaps, isHandicap } from 'shogiops/handicaps';
import { roleToKanji } from 'shogiops/notation/util';
import { initialSfen, parseSfen, roleToForsyth } from 'shogiops/sfen';
import { Handicap, Piece, Role, RULES, Rules } from 'shogiops/types';
import { allRoles, handRoles, promote } from 'shogiops/variant/util';
import { defaultPosition } from 'shogiops/variant/variant';
import { VNode, h } from 'snabbdom';
import EditorCtrl from './ctrl';
import { EditorState, Selected } from './interfaces';
import * as ground from './shogiground';

const pieceValueOrder: Role[] = ['pawn', 'lance', 'knight', 'silver', 'gold', 'bishop', 'rook', 'tokin', 'king'];
function pieceCounter(ctrl: EditorCtrl): VNode {
  function singlePieceCounter(cur: number, total: number, name: string, suffix: string = ''): VNode {
    return h('span', [`${cur.toString()}/${total.toString()}`, h('strong', ` ${name}`), `${suffix}`]);
  }
  const defaultBoard = defaultPosition(ctrl.rules).board,
    initialRoles = defaultBoard.presentRoles().sort((a, b) => {
      const indexA = pieceValueOrder.indexOf(a),
        indexB = pieceValueOrder.indexOf(b);
      return indexA - indexB;
    });

  const pieceCount: [Role, number, string][] = initialRoles.map((r: Role) => [
    r,
    defaultBoard.role(r).size(),
    `${roleToKanji(r)}(${roleToForsyth(ctrl.rules)(r)?.toUpperCase()})`,
  ]);

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

function positions(ctrl: EditorCtrl, state: EditorState): VNode {
  const position2option = function (handicap: Omit<Handicap, 'rules'>): VNode {
    return h(
      'option',
      {
        attrs: {
          value: handicap.sfen,
          'data-sfen': handicap.sfen,
          selected: state.sfen === handicap.sfen,
        },
      },
      `${handicap.japaneseName} (${handicap.englishName})`
    );
  };
  return h('div.positions', [
    h(
      'select.positions',
      {
        props: {
          value: isHandicap({ sfen: state.sfen, rules: ctrl.rules }) ? state.sfen.split(' ').slice(0, 4).join(' ') : '',
        },
        on: {
          change(e) {
            const el = e.target as HTMLSelectElement,
              value = el.selectedOptions[0].getAttribute('data-sfen');
            if (!value || !ctrl.setSfen(value)) el.value = '';
          },
        },
      },
      [
        h(
          'option',
          {
            attrs: {
              value: '',
            },
          },
          `- ${ctrl.trans.noarg('boardEditor')}  -`
        ),
        optgroup(ctrl.trans.noarg('handicaps'), findHandicaps({ rules: ctrl.rules })!.map(position2option)),
      ]
    ),
  ]);
}

function variants(ctrl: EditorCtrl): VNode {
  function variant2option(key: Rules, name: string, ctrl: EditorCtrl): VNode {
    return h(
      'option',
      {
        attrs: {
          value: key,
          selected: key === ctrl.rules,
        },
      },
      `${ctrl.trans.noarg('variant')} | ${name}`
    );
  }
  return h('div.variants', [
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
      RULES.map(rules => variant2option(rules, ctrl.trans(rules), ctrl))
    ),
  ]);
}

function color(ctrl: EditorCtrl): VNode {
  return h(
    'div.color',
    h(
      'select',
      {
        on: {
          change(e) {
            ctrl.setTurn((e.target as HTMLSelectElement).value as Color);
          },
        },
        props: {
          value: ctrl.turn,
        },
      },
      colors.map(function (color) {
        return h(
          'option',
          {
            attrs: {
              value: color,
              selected: ctrl.turn === color,
            },
          },
          transWithColorName(ctrl.trans, 'xPlays', color, false)
        );
      })
    )
  );
}

function metadata(ctrl: EditorCtrl): VNode {
  return h('div.metadata', [variants(ctrl), color(ctrl)]);
}

function additionControls(ctrl: EditorCtrl, state: EditorState): VNode {
  return h('div.additional-controls', [positions(ctrl, state), pieceCounter(ctrl)]);
}

function controls(ctrl: EditorCtrl, state: EditorState): VNode {
  return h('div.board-editor__tools', [
    metadata(ctrl),
    ...(ctrl.data.embed
      ? [
          h('div.actions', [
            h(
              'a.button.button-empty',
              {
                class: {
                  disabled: state.sfen === initialSfen(ctrl.rules),
                },
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
                class: {
                  disabled: /^[0-9\/]+$/.test(state.sfen.split(' ')[0]),
                },
                on: {
                  click() {
                    ctrl.clearBoard();
                  },
                },
              },
              ctrl.trans.noarg('clearBoard')
            ),
            ctrl.rules === 'chushogi'
              ? null
              : h(
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
                  transWithColorName(ctrl.trans, 'fillXHand', 'gote', false)
                ),
          ]),
        ]
      : [
          h('div.actions', [
            h(
              'a.button.button-empty.text',
              {
                attrs: { 'data-icon': 'W' },
                class: {
                  disabled: state.sfen === initialSfen(ctrl.rules),
                },
                on: {
                  click() {
                    ctrl.startPosition();
                  },
                },
              },
              ctrl.trans.noarg('startPosition')
            ),
            h(
              'a.button.button-empty.text',
              {
                attrs: { 'data-icon': 'q' },
                class: {
                  disabled: /^[0-9\/]+$/.test(state.sfen.split(' ')[0]),
                },
                on: {
                  click() {
                    ctrl.clearBoard();
                  },
                },
              },
              ctrl.trans.noarg('clearBoard')
            ),
            ctrl.rules === 'chushogi'
              ? null
              : h(
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
                  transWithColorName(ctrl.trans, 'fillXHand', 'gote', false)
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
              `${ctrl.trans.noarg('flipBoard')} - (${standardColorName(
                ctrl.trans,
                ctrl.shogiground.state.orientation
              )} POV)`
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
            el.setCustomValidity(valid ? '' : ctrl.trans.noarg('invalidSfen'));
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

function sparePieces(ctrl: EditorCtrl, color: Color, position: 'top' | 'bottom'): VNode {
  const selectedClass = selectedToClass(ctrl.selected());

  // Assumes correct css flex-basis
  const baseRoles: (Role | 'skip')[] = [],
    promotedRoles: (Role | 'skip')[] = [];
  allRoles(ctrl.rules).forEach(r => {
    if (!promotedRoles.includes(r)) {
      baseRoles.push(r);
      const promoted = promote(ctrl.rules)(r);
      if (promoted) {
        promotedRoles.push(promoted);
      } else promotedRoles.push('skip');
    }
  });

  const spares =
    position === 'top'
      ? [...promotedRoles, 'trash', ...baseRoles, 'pointer']
      : [...baseRoles, 'pointer', ...promotedRoles, 'trash'];

  return h(
    'div',
    {
      attrs: {
        class: ['spare', 'spare-' + position, 'spare-' + color].join(' '),
      },
    },
    spares.map((s: 'pointer' | 'trash' | 'skip' | Role) => {
      if (s === 'skip') return h('div.no-square');
      const sel: Selected = s !== 'trash' && s !== 'pointer' ? [color, s] : s;
      const className = selectedToClass(sel);
      const attrs = {
        class: className,
        ...(sel !== 'pointer' && sel !== 'trash'
          ? {
              'data-color': sel[0],
              'data-role': sel[1],
            }
          : {}),
      };
      return h(
        'div',
        {
          class: {
            'no-square': true,
            pointer: sel === 'pointer',
            trash: sel === 'trash',
            'selected-square': selectedClass === className,
          },
          on: {
            mousedown: selectSPStart(ctrl, sel),
            mouseup: selectSPEnd(ctrl, sel),
            touchstart: selectSPStart(ctrl, sel),
            touchend: selectSPEnd(ctrl, sel),
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
    sparePieces(ctrl, opposite(color), 'top'),
    ctrl.rules === 'chushogi' ? null : createHandWrap(ctrl, 'top'),
    h('div.main-board', [ground.renderBoard(ctrl)]),
    ctrl.rules === 'chushogi' ? null : createHandWrap(ctrl, 'bottom'),
    sparePieces(ctrl, color, 'bottom'),
    controls(ctrl, state),
    ctrl.data.embed ? null : additionControls(ctrl, state),
    inputs(ctrl, state.sfen),
  ]);
}
