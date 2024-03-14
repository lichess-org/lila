import { VNode, h } from 'snabbdom';
import EditorCtrl from '../ctrl';
import { EditorState } from '../interfaces';
import { initialSfen } from 'shogiops/sfen';
import { handRoles } from 'shogiops/variant/util';
import { opposite } from 'shogiops/util';
import { standardColorName, transWithColorName } from 'common/colorName';
import { isHandicap } from 'shogiops/handicaps';

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
    ctrl.trans.noarg('startPosition')
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
    ctrl.trans.noarg('clearBoard')
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
        transWithColorName(ctrl.trans, 'fillXHand', 'gote', false)
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
    `${ctrl.trans.noarg('flipBoard')} (${standardColorName(ctrl.trans, ctrl.shogiground.state.orientation)})`
  );
}

function colorTurn(ctrl: EditorCtrl, state: EditorState): VNode {
  const handicap = isHandicap({ rules: ctrl.data.variant, sfen: state.sfen });
  return h(
    'div.action.text.color-icon.' + ctrl.turn,
    {
      on: {
        click: () => {
          ctrl.setTurn(opposite(ctrl.turn));
          ctrl.redraw();
        },
      },
      attrs: { 'data-icon': '' },
    },
    h('span', transWithColorName(ctrl.trans, 'xPlays', ctrl.turn, handicap))
  );
}
