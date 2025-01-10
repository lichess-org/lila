import { i18n, i18nFormatCapitalized } from 'i18n';
import { colorName } from 'shogi/color-name';
import { isHandicap } from 'shogiops/handicaps';
import { initialSfen } from 'shogiops/sfen';
import { opposite } from 'shogiops/util';
import { handRoles } from 'shogiops/variant/util';
import { type VNode, h } from 'snabbdom';
import type EditorCtrl from '../ctrl';
import type { EditorState } from '../interfaces';

export function actions(ctrl: EditorCtrl, state: EditorState): VNode {
  return h('div.actions', [
    initialPosition(ctrl, state),
    clearBoard(ctrl, state),
    colorTurn(ctrl, state),
    fillGotesHand(ctrl),
    flipBoard(ctrl),
  ]);
}

function initialPosition(ctrl: EditorCtrl, state: EditorState): VNode {
  return h(
    'span.action.text',
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
    i18n('startPosition'),
  );
}

function clearBoard(ctrl: EditorCtrl, state: EditorState): VNode {
  return h(
    'span.action.text',
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
    i18n('clearBoard'),
  );
}

function fillGotesHand(ctrl: EditorCtrl): VNode | null {
  return handRoles(ctrl.rules).length === 0
    ? null
    : h(
        'span.action.text',
        {
          attrs: { 'data-icon': 'S' },
          class: {
            disabled: !ctrl.canFillGoteHand(),
          },
          on: {
            click() {
              ctrl.fillGotesHand();
            },
          },
        },
        i18nFormatCapitalized('fillXHand', colorName('gote', false)),
      );
}

function flipBoard(ctrl: EditorCtrl): VNode {
  return h(
    'span.action.text',
    {
      attrs: { 'data-icon': 'B' },
      on: {
        click() {
          ctrl.setOrientation(opposite(ctrl.shogiground.state.orientation));
        },
      },
    },
    `${i18n('flipBoard')} (${colorName(ctrl.shogiground.state.orientation, false)})`,
  );
}

function colorTurn(ctrl: EditorCtrl, state: EditorState): VNode {
  const handicap = isHandicap({ rules: ctrl.data.variant, sfen: state.sfen });
  return h(
    `div.action.text.color-icon.${ctrl.turn}`,
    {
      on: {
        click: () => {
          ctrl.setTurn(opposite(ctrl.turn));
          ctrl.redraw();
        },
      },
      attrs: { 'data-icon': '' },
    },
    h('span', i18nFormatCapitalized('xPlays', colorName(ctrl.turn, handicap))),
  );
}
