import { dragNewPiece } from '@lichess-org/chessground/drag';
import type { MouchEvent, NumberPair } from '@lichess-org/chessground/types';
import { eventPosition, opposite } from '@lichess-org/chessground/util';
import { lichessRules } from 'chessops/compat';
import { parseFen } from 'chessops/fen';
import { parseSquare, makeSquare } from 'chessops/util';

import { fenToEpd } from 'lib/game/chess';
import { licon, type LiconValue } from 'lib/licon';
import {
  copyMeInput,
  dataIcon,
  domDialog,
  enter,
  input,
  optgroup,
  label,
  form,
  button,
  option,
  div,
  select,
  strong,
  a,
  span,
  p,
  makeExoticTag,
  type VNode,
  type MaybeVNode,
} from 'lib/view';
import { url as xhrUrl } from 'lib/xhr';

import { fenToChess960Id, isValidPositionId } from './chess960';
import chessground from './chessground';
import type EditorCtrl from './ctrl';
import type { Selected, CastlingToggle, EditorState, EndgamePosition, OpeningPosition } from './interfaces';

function castleCheckBox(ctrl: EditorCtrl, id: CastlingToggle, inputLabel: string, reversed: boolean): VNode {
  const inputElement = input('checkbox')({
    class: { 'not-allowed': !ctrl.enabledCastlingToggles[id] },
    props: {
      checked: ctrl.castlingToggles[id] && ctrl.enabledCastlingToggles[id],
      disabled: !ctrl.enabledCastlingToggles[id],
    },
    on: {
      change(e) {
        ctrl.setCastlingToggle(id, (e.target as HTMLInputElement).checked);
      },
    },
  });
  return label(reversed ? [inputElement, inputLabel] : [inputLabel, inputElement]);
}

function studyButton(ctrl: EditorCtrl, state: EditorState): VNode {
  return form({ method: 'post', action: '/study/as' }, [
    input('hidden')({ name: 'orientation', value: ctrl.bottomColor() }),
    input('hidden')({ name: 'variant', value: lichessRules(ctrl.variant) }),
    input('hidden')({ name: 'fen', value: state.legalFen || '' }),
    button(
      {
        type: 'submit',
        ...dataIcon(licon.StudyBoard),
        disabled: !state.legalFen,
        class: { button: true, 'button-empty': true, text: true, disabled: !state.legalFen },
      },
      i18n.site.toStudy,
    ),
  ]);
}

function variantOption(key: VariantKey, name: string, ctrl: EditorCtrl): VNode {
  return option({ value: key, selected: key === ctrl.variant }, `${i18n.site.variant} | ${name}`);
}

function endgamePositionOption(pos: EndgamePosition): VNode {
  return option({ value: pos.epd || pos.fen, 'data-fen': pos.fen }, pos.name);
}

function positionOption(pos: OpeningPosition): VNode {
  return option(
    { value: pos.epd || pos.fen, 'data-fen': pos.fen },
    pos.eco ? `${pos.eco} ${pos.name}` : pos.name,
  );
}

const ALL_VARIANTS: Array<[VariantKey, string]> = [
  ['standard', i18n.variant.standard],
  ['chess960', i18n.variant.chess960],
  ['kingOfTheHill', i18n.variant.kingOfTheHill],
  ['threeCheck', i18n.variant.threeCheck],
  ['crazyhouse', i18n.variant.crazyhouse],
  ['antichess', i18n.variant.antichess],
  ['atomic', i18n.variant.atomic],
  ['horde', i18n.variant.horde],
  ['racingKings', i18n.variant.racingKings],
];

function controlsButtonStart(ctrl: EditorCtrl, icon?: LiconValue) {
  return button(
    `.button.button-empty${icon ? '.text' : ''}`,
    {
      on: {
        click(e) {
          e.preventDefault();
          ctrl.startPosition();
        },
      },
      type: 'button',
      ...(icon ? dataIcon(icon) : {}),
    },
    i18n.site.startPosition,
  );
}

function controlsButtonClear(ctrl: EditorCtrl, icon?: LiconValue) {
  return button(
    `.button.button-empty${icon ? '.text' : ''}`,
    {
      on: {
        click(e) {
          e.preventDefault();
          ctrl.clearBoard();
        },
      },
      type: 'button',
      ...(icon ? dataIcon(icon) : {}),
    },
    i18n.site.clearBoard,
  );
}

function controls(ctrl: EditorCtrl, state: EditorState): VNode {
  const chess960PositionIdSelector =
    ctrl.variant !== 'chess960'
      ? null
      : div('.metadata', [
          div('.chess960-position-row', [
            label('.form-label', { for: 'chess960-position-id' }, 'Chess960 position'),
            input('number')('#chess960-position-id', {
              minlength: 1,
              maxlength: 3,
              min: '0',
              max: '959',
              props: {
                value: ctrl.chess960PositionId,
              },
              on: {
                change(e) {
                  const value = (e.target as HTMLSelectElement).value;
                  if (!/^\d+$/.test(value)) return;
                  const candidateId = parseInt(value);
                  if (!isValidPositionId(candidateId)) return;
                  ctrl.set960Position(candidateId);
                },
                keydown: enter(target => target.blur()),
              },
            }),
            button('.button.button-empty', {
              type: 'button',
              title: i18n.site.randomChess960Position,
              ...dataIcon(licon.DieSix),
              on: {
                click(e) {
                  e.preventDefault();
                  ctrl.setRandom960Position();
                },
              },
            }),
          ]),
        ]);

  return div('.board-editor__tools', [
    div('.metadata', [
      div(
        '.color',
        select(
          {
            on: {
              change(e) {
                ctrl.setTurn((e.target as HTMLSelectElement).value as Color);
              },
            },
            props: { value: ctrl.turn },
          },
          (['whitePlays', 'blackPlays'] as const).map(key =>
            option(
              {
                value: key.startsWith('w') ? 'white' : 'black',
                selected: key.startsWith(ctrl.turn[0]),
              },
              i18n.site[key],
            ),
          ),
        ),
      ),
      div('.castling', [
        strong(i18n.site.castling),
        div([
          castleCheckBox(ctrl, 'K', i18n.site.whiteCastlingKingside, !!ctrl.options.inlineCastling),
          castleCheckBox(ctrl, 'Q', 'O-O-O', true),
        ]),
        div([
          castleCheckBox(ctrl, 'k', i18n.site.blackCastlingKingside, !!ctrl.options.inlineCastling),
          castleCheckBox(ctrl, 'q', 'O-O-O', true),
        ]),
      ]),
      div('.enpassant', [
        label({ for: 'enpassant-select' }, i18n.site.enPassant),
        select(
          '#enpassant-select',
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
              option(
                {
                  value: key,
                  selected: (key ? parseSquare(key) : undefined) === ctrl.epSquare,
                  hidden: Boolean(key && !state.enPassantOptions.includes(key)),
                  disabled: Boolean(key && !state.enPassantOptions.includes(key)) /*Safari*/,
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
            const epd = fenToEpd(state.fen);
            const value =
              (
                ctrl.cfg.positions.find(p => p.fen.startsWith(epd)) ||
                ctrl.cfg.endgamePositions.find(p => p.epd === epd)
              )?.epd || '';
            return select(
              '.positions',
              {
                props: { value },
                on: {
                  insert(vnode) {
                    (vnode.elm as HTMLSelectElement).value = fenToEpd(state.fen);
                  },
                  change(e) {
                    const el = e.target as HTMLSelectElement;
                    const value = el.selectedOptions[0].getAttribute('data-fen');
                    if (!value || !ctrl.setFen(value)) el.value = '';
                  },
                },
              },
              [
                option({ value: '' }, i18n.site.setTheBoard),
                optgroup(i18n.site.popularOpenings)(ctrl.cfg.positions.map(positionOption)),
                optgroup(i18n.site.endgamePositions)(ctrl.cfg.endgamePositions.map(endgamePositionOption)),
              ],
            );
          })(),
        ]),
    ...(ctrl.cfg.embed
      ? [div('.actions', [chess960PositionIdSelector, controlsButtonStart(ctrl), controlsButtonClear(ctrl)])]
      : [
          div([
            select(
              {
                id: 'variants',
                on: {
                  change(e) {
                    const value = (e.target as HTMLSelectElement).value;
                    if (value === 'chess960') ctrl.setRandom960Position();
                    ctrl.setVariant(value as VariantKey);
                  },
                },
              },
              ALL_VARIANTS.map(x => variantOption(x[0], x[1], ctrl)),
            ),
          ]),
          chess960PositionIdSelector,
          div('.actions', [
            controlsButtonStart(ctrl, licon.Reload),
            controlsButtonClear(ctrl, licon.Trash),
            button(
              '.button.button-empty.text',
              {
                ...dataIcon(licon.ChasingArrows),
                on: {
                  click() {
                    ctrl.chessground!.toggleOrientation();
                    ctrl.onChange();
                  },
                },
              },
              i18n.site.flipBoard,
            ),
            a(state.legalFen ? ctrl.makeAnalysisUrl(state.legalFen, ctrl.bottomColor()) : '')(
              {
                ...dataIcon(licon.Microscope),
                rel: 'nofollow',
                class: {
                  button: true,
                  'button-empty': true,
                  text: true,
                  disabled: !state.legalFen,
                },
              },
              i18n.site.analysis,
            ),
            button(
              {
                class: { button: true, 'button-empty': true, disabled: !state.playable },
                disabled: !state.playable,
                on: {
                  click: () => {
                    if (state.playable)
                      domDialog({
                        cash: $('.continue-with'),
                        modal: true,
                        easyClose: 'clickOutside',
                        show: true,
                      });
                  },
                },
              },
              [span('.text', dataIcon(licon.Swords), i18n.site.continueFromHere)],
            ),
            studyButton(ctrl, state),
          ]),
          div('.continue-with.none', [
            a('/?fen=' + state.legalFen + '#ai')(
              '.button',
              { rel: 'nofollow' },
              i18n.site.playAgainstComputer,
            ),
            a('/?fen=' + state.legalFen + '#friend')(
              '.button',
              { rel: 'nofollow' },
              i18n.site.challengeAFriend,
            ),
          ]),
        ]),
  ]);
}

function inputs(ctrl: EditorCtrl, fen: FEN): MaybeVNode {
  if (ctrl.cfg.embed) return undefined;

  return div('.copyables', [
    p([
      strong('FEN'),
      copyMeInput(fen, {
        inputAttrs: { enterkeyhint: 'done' },
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
          keypress: enter<HTMLInputElement>(el => {
            const candidateChess960Id = fenToChess960Id(el.value.trim());
            if (candidateChess960Id !== undefined) {
              ctrl.chess960PositionId = candidateChess960Id;
            }
            el.blur();
          }),
        },
      }),
    ]),
    p([
      strong('.name', 'URL'),
      copyMeInput(ctrl.makeEditorUrl(fen, ctrl.bottomColor()), { inputAttrs: { readonly: true } }),
    ]),
    a(
      xhrUrl(`${site.asset.baseUrl()}/export/fen.gif`, {
        fen: ctrl.urlFen(fen),
        color: ctrl.bottomColor(),
        theme: document.body.dataset.board,
        piece: document.body.dataset.pieceSet,
      }),
    )(
      {
        download: true,
      },
      'SCREENSHOT',
    ),
  ]);
}

// can be 'pointer', 'trash', or [color, role]
function selectedToClass(s: Selected): string {
  return s === 'pointer' || s === 'trash' ? s : s.join(' ');
}

let lastTouchMovePos: NumberPair | undefined;
const piece = makeExoticTag('piece');

function sparePieces(ctrl: EditorCtrl, color: Color, position: 'top' | 'bottom'): VNode {
  const selectedClass = selectedToClass(ctrl.selected());
  const pieces = ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].map(role => [color, role]);

  return div(
    {
      class: {
        spare: true,
        ['spare-' + position]: true,
        ['spare-' + color]: true,
      },
    },
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
        selectedClass === className && !ctrl.chessground?.state.draggable.current?.newPiece;
      return div(
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
              lastTouchMovePos = eventPosition(e);
            },
          },
        },
        div(piece({ attrs })),
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

  return div(`.board-editor.board-editor--${ctrl.variant}`, [
    sparePieces(ctrl, opposite(color), 'top'),
    div('.main-board', { attrs: { style: `cursor: ${makeCursor(ctrl.selected())}` } }, chessground(ctrl)),
    sparePieces(ctrl, color, 'bottom'),
    controls(ctrl, state),
    inputs(ctrl, state.legalFen || state.fen),
  ]);
}
