import { drag, shadowDrop, selectToDrop } from './handCtrl';
import { h } from 'snabbdom';
import { MouchEvent } from 'shogiground/types';
import { onInsert } from 'puz/util';
import { PocketRole } from 'shogiops/types';
import { opposite } from 'shogiops/util';
import StormCtrl from '../ctrl';

const eventNames1 = ['mousedown', 'touchmove'];
const eventNames2 = ['click'];
const eventNames3 = ['contextmenu'];

const oKeys = ['pawn', 'lance', 'knight', 'silver', 'gold', 'bishop', 'rook'];

type Position = 'top' | 'bottom';

export default function (ctrl: StormCtrl, position: Position) {
  const shogi = ctrl.run.current.position();
  // We are solving from the bottom, initial color is our color
  const color = position === 'bottom' ? ctrl.run.pov : opposite(ctrl.run.pov);
  const pocket = shogi.pockets[color];

  const usable = color === shogi.turn;
  return h(
    `div.pocket.is2d.pocket-${position}`,
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
    oKeys.map(role => {
      const nb = pocket[role as PocketRole] ?? 0;
      const g = ctrl.ground();
      const selectedPiece = g && role == g.state.drawable.piece?.role && color == g.state.drawable.piece?.color;
      const selectedSquare: boolean =
        g &&
        g.state.dropmode.active &&
        g.state.dropmode.piece?.color === color &&
        g.state.dropmode.piece?.role === role &&
        g.state.movable.color == color;
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
