import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { copyMeInput } from 'common/copyMe';
import { MouchEvent, NumberPair } from 'chessground/types';
import { dragNewPiece } from 'chessground/drag';
import { eventPosition, opposite } from 'chessground/util';
import { Rules } from 'chessops/types';
import { parseFen } from 'chessops/fen';
import { parseSquare, makeSquare } from 'chessops/util';
import EditorCtrl from './ctrl';
import chessground from './chessground';
import { Selected, CastlingToggle, EditorState } from './interfaces';
import { dataIcon } from 'common/snabbdom';
import { domDialog } from 'common/dialog';

function castleCheckBox(ctrl: EditorCtrl, id: CastlingToggle, label: string, reversed: boolean): VNode {
  const input = h('input', {
    attrs: { type: 'checkbox' },
    props: { checked: ctrl.castlingToggles[id] },
    on: {
      change(e) {
        ctrl.setCastlingToggle(id, (e.target as HTMLInputElement).checked);
      },
    },
  });
  return h('label', reversed ? [input, label] : [label, input]);
}

function optgroup(name: string, opts: VNode[]): VNode {
  return h('optgroup', { attrs: { label: name } }, opts);
}

function studyButton(ctrl: EditorCtrl, state: EditorState): VNode {
  return h('form', { attrs: { method: 'post', action: '/study/as' } }, [
    h('input', { attrs: { type: 'hidden', name: 'orientation', value: ctrl.bottomColor() } }),
    h('input', { attrs: { type: 'hidden', name: 'variant', value: ctrl.rules } }),
    h('input', { attrs: { type: 'hidden', name: 'fen', value: state.legalFen || '' } }),
    h(
      'button',
      {
        attrs: { type: 'submit', 'data-icon': licon.StudyBoard, disabled: !state.legalFen },
        class: { button: true, 'button-empty': true, text: true, disabled: !state.legalFen },
      },
      ctrl.trans.noarg('toStudy'),
    ),
  ]);
}

function variant2option(key: Rules, name: string, ctrl: EditorCtrl): VNode {
  return h(
    'option',
    { attrs: { value: key, selected: key == ctrl.rules } },
    `${ctrl.trans.noarg('variant')} | ${name}`,
  );
}

const allVariants: Array<[Rules, string]> = [
  ['chess', 'Standard'],
  ['antichess', 'Antichess'],
  ['atomic', 'Atomic'],
  ['crazyhouse', 'Crazyhouse'],
  ['horde', 'Horde'],
  ['kingofthehill', 'King of the Hill'],
  ['racingkings', 'Racing Kings'],
  ['3check', 'Three-check'],
];

function controls(ctrl: EditorCtrl, state: EditorState): VNode {
  const endgamePosition2option = function (pos: Editor.EndgamePosition): VNode {
    return h('option', { attrs: { value: pos.epd || pos.fen, 'data-fen': pos.fen } }, pos.name);
  };

  const buttonStart = (icon?: string) =>
    h(
      `a.button.button-empty${icon ? '.text' : ''}`,
      { on: { click: ctrl.startPosition }, attrs: icon ? dataIcon(icon) : {} },
      ctrl.trans.noarg('startPosition'),
    );
  const buttonClear = (icon?: string) =>
    h(
      `a.button.button-empty${icon ? '.text' : ''}`,
      { on: { click: ctrl.clearBoard }, attrs: icon ? dataIcon(icon) : {} },
      ctrl.trans.noarg('clearBoard'),
    );

  return h('div.board-editor__tools', [
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
            props: { value: ctrl.turn },
          },
          ['whitePlays', 'blackPlays'].map(function (key) {
            return h(
              'option',
              {
                attrs: { value: key[0] === 'w' ? 'white' : 'black', selected: key[0] === ctrl.turn[0] },
              },
              ctrl.trans(key),
            );
          }),
        ),
      ),
      h('div.castling', [
        h('strong', ctrl.trans.noarg('castling')),
        h('div', [
          castleCheckBox(ctrl, 'K', ctrl.trans.noarg('whiteCastlingKingside'), !!ctrl.options.inlineCastling),
          castleCheckBox(ctrl, 'Q', 'O-O-O', true),
        ]),
        h('div', [
          castleCheckBox(ctrl, 'k', ctrl.trans.noarg('blackCastlingKingside'), !!ctrl.options.inlineCastling),
          castleCheckBox(ctrl, 'q', 'O-O-O', true),
        ]),
      ]),
      h('div.enpassant', [
        h('label', { attrs: { for: 'enpassant-select' } }, 'En passant'),
        h(
          'select#enpassant-select',
          {
            on: {
              change(e) {
                ctrl.setEnPassant(parseSquare((e.target as HTMLSelectElement).value));
              },
            },
            props: { value: ctrl.epSquare ? makeSquare(ctrl.epSquare) : '' },
          },
          ['', ...[ctrl.turn === 'black' ? 3 : 6].flatMap(r => 'abcdefgh'.split('').map(f => f + r))].map(
            key =>
              h(
                'option',
                {
                  attrs: {
                    value: key,
                    selected: (key ? parseSquare(key) : undefined) === ctrl.epSquare,
                    hidden: Boolean(key && !state.enPassantOptions.includes(key)),
                    disabled: Boolean(key && !state.enPassantOptions.includes(key)) /*Safari*/,
                  },
                },
                key,
              ),
          ),
        ),
      ]),
    ]),
    ...(ctrl.cfg.embed || !ctrl.cfg.positions || !ctrl.cfg.endgamePositions
      ? []
      : [
          (() => {
            const positionOption = (pos: Editor.OpeningPosition): VNode =>
              h(
                'option',
                { attrs: { value: pos.epd || pos.fen, 'data-fen': pos.fen } },
                pos.eco ? `${pos.eco} ${pos.name}` : pos.name,
              );
            const epd = state.fen.split(' ').slice(0, 4).join(' ');
            const value =
              (
                ctrl.cfg.positions.find(p => p.fen.startsWith(epd)) ||
                ctrl.cfg.endgamePositions.find(p => p.epd == epd)
              )?.epd || '';
            return h(
              'select.positions',
              {
                props: { value },
                on: {
                  insert(vnode) {
                    (vnode.elm as HTMLSelectElement).value = state.fen.split(' ').slice(0, 4).join(' ');
                  },
                  change(e) {
                    const el = e.target as HTMLSelectElement;
                    const value = el.selectedOptions[0].getAttribute('data-fen');
                    if (!value || !ctrl.setFen(value)) el.value = '';
                  },
                },
              },
              [
                h('option', { attrs: { value: '' } }, ctrl.trans.noarg('setTheBoard')),
                optgroup(ctrl.trans.noarg('popularOpenings'), ctrl.cfg.positions.map(positionOption)),
                optgroup(
                  ctrl.trans.noarg('endgamePositions'),
                  ctrl.cfg.endgamePositions.map(endgamePosition2option),
                ),
              ],
            );
          })(),
        ]),
    ...(ctrl.cfg.embed
      ? [h('div.actions', [buttonStart(), buttonClear()])]
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
              allVariants.map(x => variant2option(x[0], x[1], ctrl)),
            ),
          ]),
          h('div.actions', [
            buttonStart(licon.Reload),
            buttonClear(licon.Trash),
            h(
              'button.button.button-empty.text',
              {
                attrs: { 'data-icon': licon.ChasingArrows },
                on: {
                  click() {
                    ctrl.chessground!.toggleOrientation();
                    ctrl.onChange();
                  },
                },
              },
              ctrl.trans.noarg('flipBoard'),
            ),
            h(
              'a',
              {
                attrs: {
                  'data-icon': licon.Microscope,
                  rel: 'nofollow',
                  ...(state.legalFen
                    ? { href: ctrl.makeAnalysisUrl(state.legalFen, ctrl.bottomColor()) }
                    : {}),
                },
                class: {
                  button: true,
                  'button-empty': true,
                  text: true,
                  disabled: !state.legalFen,
                },
              },
              ctrl.trans.noarg('analysis'),
            ),
            h(
              'button',
              {
                class: { button: true, 'button-empty': true, disabled: !state.playable },
                on: {
                  click: () => {
                    if (state.playable) domDialog({ cash: $('.continue-with'), show: 'modal' });
                  },
                },
              },
              [
                h(
                  'span.text',
                  { attrs: { 'data-icon': licon.Swords } },
                  ctrl.trans.noarg('continueFromHere'),
                ),
              ],
            ),
            studyButton(ctrl, state),
          ]),
          h('div.continue-with.none', [
            h(
              'a.button',
              { attrs: { href: '/?fen=' + state.legalFen + '#ai', rel: 'nofollow' } },
              ctrl.trans.noarg('playWithTheMachine'),
            ),
            h(
              'a.button',
              { attrs: { href: '/?fen=' + state.legalFen + '#friend', rel: 'nofollow' } },
              ctrl.trans.noarg('playWithAFriend'),
            ),
          ]),
        ]),
  ]);
}

function inputs(ctrl: EditorCtrl, fen: string): VNode | undefined {
  if (ctrl.cfg.embed) return;
  return h('div.copyables', [
    h('p', [
      h('strong', 'FEN'),
      h('input', {
        attrs: { spellcheck: 'false', enterkeyhint: 'done' },
        props: { value: fen },
        on: {
          change(e) {
            const el = e.target as HTMLInputElement;
            ctrl.setFen(el.value.trim());
            el.reportValidity();
          },
          input(e) {
            const el = e.target as HTMLInputElement;
            const valid = parseFen(el.value.trim()).isOk;
            el.setCustomValidity(valid ? '' : 'Invalid FEN');
          },
          blur(e) {
            const el = e.target as HTMLInputElement;
            el.value = ctrl.getFen();
            el.setCustomValidity('');
          },
          keypress(e) {
            const el = e.target as HTMLInputElement;
            if (e.key === 'Enter') {
              el.blur();
            }
          },
        },
      }),
    ]),
    h('p', [h('strong.name', 'URL'), copyMeInput(ctrl.makeEditorUrl(fen, ctrl.bottomColor()))]),
    h('a', { attrs: { href: ctrl.makeImageUrl(fen) } }, 'SCREENSHOT'),
  ]);
}

// can be 'pointer', 'trash', or [color, role]
function selectedToClass(s: Selected): string {
  return s === 'pointer' || s === 'trash' ? s : s.join(' ');
}

let lastTouchMovePos: NumberPair | undefined;

function sparePieces(ctrl: EditorCtrl, color: Color, _orientation: Color, position: 'top' | 'bottom'): VNode {
  const selectedClass = selectedToClass(ctrl.selected());

  const pieces = ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].map(function (role) {
    return [color, role];
  });

  return h(
    'div',
    { attrs: { class: ['spare', 'spare-' + position, 'spare-' + color].join(' ') } },
    ['pointer', ...pieces, 'trash'].map((s: Selected) => {
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
      const selectedSquare =
        selectedClass === className &&
        (!ctrl.chessground ||
          !ctrl.chessground.state.draggable.current ||
          !ctrl.chessground.state.draggable.current.newPiece);
      return h(
        'div',
        {
          class: {
            'no-square': true,
            pointer: s === 'pointer',
            trash: s === 'trash',
            'selected-square': selectedSquare,
          },
          on: {
            mousedown: onSelectSparePiece(ctrl, s, 'mouseup'),
            touchstart: onSelectSparePiece(ctrl, s, 'touchend'),
            touchmove: e => {
              lastTouchMovePos = eventPosition(e as any);
            },
          },
        },
        [h('div', [h('piece', { attrs })])],
      );
    }),
  );
}

function onSelectSparePiece(ctrl: EditorCtrl, s: Selected, upEvent: string): (e: MouchEvent) => void {
  return function (e: MouchEvent): void {
    e.preventDefault();
    if (s === 'pointer' || s === 'trash') {
      ctrl.selected(s);
      ctrl.redraw();
    } else {
      ctrl.selected('pointer');

      dragNewPiece(ctrl.chessground!.state, { color: s[0], role: s[1] }, e, true);

      document.addEventListener(
        upEvent,
        (e: MouchEvent) => {
          const eventPos = eventPosition(e) || lastTouchMovePos;
          if (eventPos && ctrl.chessground!.getKeyAtDomPos(eventPos)) ctrl.selected('pointer');
          else ctrl.selected(s);
          ctrl.redraw();
        },
        { once: true },
      );
    }
  };
}

function makeCursor(selected: Selected): string {
  if (selected === 'pointer') return 'pointer';

  const name = selected === 'trash' ? 'trash' : selected.join('-');
  const url = site.asset.url('cursors/' + name + '.cur');

  return `url('${url}'), default !important`;
}

export default function (ctrl: EditorCtrl): VNode {
  const state = ctrl.getState();
  const color = ctrl.bottomColor();

  return h('div.board-editor', { attrs: { style: `cursor: ${makeCursor(ctrl.selected())}` } }, [
    sparePieces(ctrl, opposite(color), color, 'top'),
    h('div.main-board', [chessground(ctrl)]),
    sparePieces(ctrl, color, color, 'bottom'),
    controls(ctrl, state),
    inputs(ctrl, state.legalFen || state.fen),
  ]);
}
