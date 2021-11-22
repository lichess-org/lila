import { drag, shadowDrop, selectToDrop } from './handCtrl';
import { h } from 'snabbdom';
import { MouchEvent } from 'shogiground/types';
import { onInsert } from '../util';
import { Controller } from '../interfaces';
import { opposite } from 'shogiops/util';
import { handRoles } from 'shogiops/variantUtil';
import { defined } from 'common';

const eventNames1 = ['mousedown', 'touchmove'];
const eventNames2 = ['click'];
const eventNames3 = ['contextmenu'];

type Position = 'top' | 'bottom';

export default function (ctrl: Controller, position: Position) {
  const shogi = ctrl.position();
  // We are solving from the bottom, initial color is our color
  const color = position === 'bottom' ? ctrl.vm.pov : opposite(ctrl.vm.pov);
  const pocket = shogi.hands[color];

  const usable = color === shogi.turn;
  return h(
    `div.pocket.pocket-${position}`,
    {
      class: { usable },
      hook: onInsert(el => {
        eventNames1.forEach(name => {
          el.addEventListener(name, e => drag(ctrl, e as MouchEvent));
        });
        eventNames2.forEach(name => {
          el.addEventListener(name, e => {
            selectToDrop(ctrl, e as MouchEvent);
          });
        });
        eventNames3.forEach(name => {
          el.addEventListener(name, e => {
            shadowDrop(ctrl, e as MouchEvent);
          });
        });
      }),
    },
    handRoles('shogi')
      .reverse()
      .map(role => {
        let nb = pocket[role] ?? 0;
        const ground = ctrl.ground();
        const selectedPiece =
          defined(ground) && role == ground.state.drawable.piece?.role && color == ground.state.drawable.piece?.color;
        const selectedSquare: boolean =
          defined(ground) &&
          ground.state.dropmode.active &&
          ground.state.dropmode.piece?.color === color &&
          ground.state.dropmode.piece?.role === role &&
          ground.state.movable.color == color;
        return h(
          'div.pocket-c1',
          h(
            'div.pocket-c2',
            {
              class: {
                'shadow-piece': selectedPiece,
              },
            },
            h('piece.' + role + '.' + color, {
              class: {
                'selected-square': selectedSquare,
              },
              attrs: {
                'data-role': role,
                'data-color': color,
                'data-nb': nb,
                cursor: 'pointer',
              },
            })
          )
        );
      })
  );
}
