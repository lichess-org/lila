import { standardColorName, transWithColorName } from 'common/colorName';
import { getPerfIcon } from 'common/perfIcons';
import { bindMobileMousedown } from 'common/mobile';
import { onInsert } from 'common/snabbdom';
import { dragNewPiece } from 'shogiground/drag';
import { MouchEvent, colors } from 'shogiground/types';
import { eventPosition, opposite, samePiece } from 'shogiground/util';
import { findHandicaps, isHandicap } from 'shogiops/handicaps';
import { roleToKanji } from 'shogiops/notation/util';
import { initialSfen, makeSfen, parseSfen, roleToForsyth } from 'shogiops/sfen';
import { Handicap, Piece, RULES, Role, Rules } from 'shogiops/types';
import { allRoles, handRoles, promote } from 'shogiops/variant/util';
import { defaultPosition } from 'shogiops/variant/variant';
import { VNode, VNodes, h } from 'snabbdom';
import EditorCtrl from './ctrl';
import { EditorState, Selected } from './interfaces';
import * as ground from './shogiground';
import { makeKifHeader, parseKifHeader } from 'shogiops/notation/kif/kif';
import { makeCsaHeader, parseCsaHeader } from 'shogiops/notation/csa/csa';

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
        optgroup(ctrl.trans.noarg('handicaps'), findHandicaps({ rules: ctrl.rules }).map(position2option)),
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

function mselect(id: string, current: VNode, items: VNodes) {
  return h('div.mselect', [
    h('input.mselect__toggle.fullscreen-toggle', { attrs: { type: 'checkbox', id: 'mselect-' + id } }),
    h('label.mselect__label', { attrs: { for: 'mselect-' + id } }, current),
    h('label.fullscreen-mask', { attrs: { for: 'mselect-' + id } }),
    h('nav.mselect__list', items),
  ]);
}

function colorChoice(ctrl: EditorCtrl, state: EditorState, color: Color, position: 'top' | 'bottom'): VNode {
  const handicap = isHandicap({ rules: ctrl.data.variant, sfen: state.sfen });
  return h(
    'div.color-choice.' + color + '.color-choice-' + position,
    {
      on: {
        click: () => {
          ctrl.setTurn(color);
          ctrl.redraw();
        },
      },
      attrs: { title: transWithColorName(ctrl.trans, 'xPlays', color, handicap) },
      class: {
        selected: ctrl.turn === color,
      },
    },
    h('span', transWithColorName(ctrl.trans, 'xPlays', color, handicap))
  );
}

function validation(state: EditorState): VNode {
  return h('div.position-validation', {
    class: {
      valid: !!state.legalSfen && state.playable,
      invalid: !!state.legalSfen && !state.playable,
      illegal: !state.legalSfen,
    },
  });
}

function side(ctrl: EditorCtrl, state: EditorState): VNode {
  return h('div.board-editor__side', [
    h(
      'div.msel-' + ctrl.rules,
      mselect(
        'board-editor-variant',
        h('span.text', { attrs: { 'data-icon': getPerfIcon(ctrl.rules) } }, ctrl.trans.noarg(ctrl.rules)),
        RULES.map(rules =>
          h(
            'a',
            {
              attrs: { 'data-icon': getPerfIcon(rules) },
              class: { current: rules === ctrl.rules },
              on: {
                click() {
                  ctrl.setRules(rules);
                },
              },
            },
            ctrl.trans.noarg(rules)
          )
        )
      )
    ),
    validation(state),
    h('div.positions-wrap', [positions(ctrl, state), pieceCounter(ctrl)]),
  ]);
}

function tools(ctrl: EditorCtrl, state: EditorState): VNode {
  return h('div.board-editor__tools', [
    // metadata(ctrl),
    h('div.ceval'),
    ...(ctrl.data.embed
      ? [
          h('div.actions', [
            h(
              'button.button.button-empty',
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
              'button.button.button-empty',
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
                  'button.button.button-empty',
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
            handRoles(ctrl.rules).length === 0
              ? null
              : h(
                  'a.button.button-empty.text.gote.color-icon',
                  {
                    attrs: { 'data-icon': '' },
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
            h('div', studyButton(ctrl, state)),
          ]),
          h('div.continue-with.none', [
            h(
              'a',
              {
                class: {
                  button: true,
                  disabled: ['chushogi', 'annansogi'].includes(ctrl.rules),
                },
                attrs: {
                  href:
                    '/?sfen=' +
                    ctrl.encodeSfen(state.legalSfen || '') +
                    `&variant=${ctrl.encodeVariant(ctrl.rules)}` +
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
                    `&variant=${ctrl.encodeVariant(ctrl.rules)}` +
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
  const pos = parseSfen(ctrl.rules, sfen);
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
      h('strong.name', 'URL'),
      h('input.copyable.autoselect', {
        attrs: {
          readonly: true,
          spellcheck: false,
          value: ctrl.makeEditorUrl(sfen),
        },
      }),
    ]),
    h('p', [
      h('strong', 'KIF'),
      h('textarea.copyable.autoselect.kif', {
        attrs: {
          spellcheck: false,
        },
        props: {
          value: pos.isOk ? makeKifHeader(pos.value) : '',
        },
        on: {
          change(e) {
            const el = e.target as HTMLTextAreaElement;
            const pos = parseKifHeader(el.value);
            if (pos.isOk) {
              const sfen = makeSfen(pos.value);
              ctrl.setSfen(sfen);
            }
            el.reportValidity();
          },
          input(e) {
            const el = e.target as HTMLTextAreaElement;
            const valid = parseKifHeader(el.value).isOk;
            el.setCustomValidity(valid ? '' : ctrl.trans.noarg('invalidNotation'));
          },
          blur(e) {
            const el = e.target as HTMLTextAreaElement;
            const pos = parseSfen(ctrl.rules, ctrl.getSfen());
            if (pos.isOk) el.value = makeKifHeader(pos.value);
            else el.value = '';
            el.setCustomValidity('');
          },
        },
      }),
    ]),
    ctrl.rules === 'standard'
      ? h('p', [
          h('strong', 'CSA'),
          h('textarea.copyable.autoselect.csa', {
            attrs: {
              spellcheck: false,
            },
            props: {
              value: pos.isOk ? makeCsaHeader(pos.value) : '',
            },
            on: {
              change(e) {
                const el = e.target as HTMLTextAreaElement;
                const pos = parseCsaHeader(el.value);
                if (pos.isOk) {
                  const sfen = makeSfen(pos.value);
                  ctrl.setSfen(sfen);
                }
                el.reportValidity();
              },
              input(e) {
                const el = e.target as HTMLTextAreaElement;
                const valid = parseCsaHeader(el.value).isOk;
                el.setCustomValidity(valid ? '' : ctrl.trans.noarg('invalidNotation'));
              },
              blur(e) {
                const el = e.target as HTMLTextAreaElement;
                const pos = parseSfen(ctrl.rules, ctrl.getSfen());
                if (pos.isOk) el.value = makeCsaHeader(pos.value);
                else el.value = '';
                el.setCustomValidity('');
              },
            },
          }),
        ])
      : null,
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

  // initial pieces first
  const roles: Role[] =
    ctrl.rules === 'kyotoshogi' ? ['pawn', 'gold', 'king', 'silver', 'tokin'] : allRoles(ctrl.rules);

  roles.forEach(r => {
    if (!promotedRoles.includes(r)) {
      baseRoles.push(r);
      const promoted = promote(ctrl.rules)(r);
      if (promoted) {
        promotedRoles.push(promoted);
      } else if (r !== 'king') promotedRoles.push('skip');
    }
  });
  if (position === 'bottom') {
    baseRoles.reverse();
    promotedRoles.reverse();
  }
  const spares =
    position === 'top' ? [...baseRoles, ...promotedRoles, 'trash'] : [...baseRoles, 'pointer', ...promotedRoles];
  // const spares = [...baseRoles, ...promotedRoles, position === 'bottom' ? 'pointer' : 'trash'];

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
        },
        [
          h(
            'div',
            {
              on: {
                mousedown: selectSPStart(ctrl, sel),
                mouseup: selectSPEnd(ctrl, sel),
                touchstart: selectSPStart(ctrl, sel),
                touchend: selectSPEnd(ctrl, sel),
              },
            },
            [h('piece', { attrs })]
          ),
        ]
      );
    })
  );
}

let initSpareEvent: Selected | undefined = undefined;
function selectSPStart(ctrl: EditorCtrl, s: Selected): (e: MouchEvent) => void {
  return function (e: MouchEvent): void {
    initSpareEvent = s;
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
    if (!initSpareEvent) return;

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
    initSpareEvent = undefined;
    ctrl.redraw();
  };
}

function createHandHelpers(ctrl: EditorCtrl, position: 'top' | 'bottom'): VNode {
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

  const hRoles = handRoles(ctrl.rules);
  if (position === 'bottom') hRoles.reverse();

  for (const r of hRoles) {
    pluses.push(h('div.plus', { attrs: { 'data-role': r, 'data-pos': position } }));
    minuses.push(h('div.minus', { attrs: { 'data-role': r, 'data-pos': position } }));
  }
  return h('div.hand-spare.hand-spare-' + position, [
    h('div.pluses', { on: { click: addPiece } }, pluses),
    h('div.minuses', { on: { click: removePiece } }, minuses),
  ]);
}

function dataAct(e: Event): string | null {
  const target = e.target as HTMLElement;
  return target.getAttribute('data-act') || (target.parentNode as HTMLElement).getAttribute('data-act');
}

function jumpButton(icon: string, effect: string, enabled: boolean): VNode {
  return h('button.fbt', {
    class: { disabled: !enabled },
    attrs: { 'data-act': effect, 'data-icon': icon },
  });
}

function controls(ctrl: EditorCtrl): VNode {
  return h(
    'div.editor-controls',
    {
      hook: onInsert(el => {
        bindMobileMousedown(
          el,
          e => {
            const action = dataAct(e);
            if (action === 'prev') ctrl.backward();
            if (action === 'next') ctrl.forward();
            else if (action === 'first') ctrl.first();
            else if (action === 'last') ctrl.last();
          },
          ctrl.redraw
        );
      }),
    },
    h('div.jumps', [
      jumpButton('W', 'first', ctrl.backStack.length > 0),
      jumpButton('Y', 'prev', ctrl.backStack.length > 0),
      jumpButton('X', 'next', ctrl.forwardStack.length > 0),
      jumpButton('V', 'last', ctrl.forwardStack.length > 0),
    ])
  );
}

export default function (ctrl: EditorCtrl): VNode {
  const state = ctrl.getState(),
    color = ctrl.bottomColor();

  return h(`div.board-editor.variant-${ctrl.rules}`, [
    h('div.main-board', [
      colorChoice(ctrl, state, opposite(color), 'top'),
      sparePieces(ctrl, opposite(color), 'top'),
      createHandHelpers(ctrl, 'top'),
      ground.renderBoard(ctrl),
      colorChoice(ctrl, state, color, 'bottom'),
      sparePieces(ctrl, color, 'bottom'),
      createHandHelpers(ctrl, 'bottom'),
    ]),
    tools(ctrl, state),
    controls(ctrl),
    ctrl.data.embed ? metadata(ctrl) : side(ctrl, state),
    inputs(ctrl, state.sfen),
  ]);
}
