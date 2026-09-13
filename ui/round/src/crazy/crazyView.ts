import type { MouchEvent } from '@lichess-org/chessground/types';
import { h } from 'snabbdom';

import type { TopOrBottom } from 'lib/game';
import { MoveEvent } from 'lib/prefs';
import { onInsert, type LooseVNode } from 'lib/view';

import type RoundController from '../ctrl';
import { plyStep } from '../util';
import { click, crazyKeys, drag, isClicked, pieceRoles } from './crazyCtrl';

const dragEventNames = ['mousedown', 'touchstart'];

export default function pocket(ctrl: RoundController, color: Color, position: TopOrBottom): LooseVNode {
  const step = plyStep(ctrl.data, ctrl.ply);
  if (!step.crazy) return undefined;
  const droppedRole = ctrl.justDropped,
    preDropRole = ctrl.preDrop,
    pocket = step.crazy.pockets[color === 'white' ? 0 : 1],
    usablePos = position === (ctrl.flip ? 'top' : 'bottom'),
    usable = usablePos && !ctrl.replaying() && ctrl.isPlaying(),
    activeColor = color === ctrl.data.player.color;
  const capturedPiece = ctrl.justCaptured;
  const captured = capturedPiece && (capturedPiece.promoted ? 'pawn' : capturedPiece.role);
  return h(
    'div.pocket.is2d.pocket-' + position,
    {
      class: { usable },
      hook: onInsert(el => {
        if (ctrl.data.pref.moveEvent !== MoveEvent.Click)
          dragEventNames.forEach(name =>
            el.addEventListener(name, (e: MouchEvent) => {
              if (usablePos && crazyKeys.length === 0) drag(ctrl, e);
            }),
          );
        if (ctrl.data.pref.moveEvent !== MoveEvent.Drag)
          el.addEventListener('click', (e: MouchEvent) => {
            if (usablePos) click(ctrl, e);
          });
      }),
    },
    pieceRoles.map(role => {
      let nb = pocket[role] || 0;
      if (activeColor) {
        if (droppedRole === role) nb--;
        if (captured === role) nb++;
      }
      return h(
        'div.pocket-c1',
        h(
          'div.pocket-c2',
          h('piece.' + role + '.' + color, {
            class: { premove: activeColor && preDropRole === role, selected: isClicked(color, role) },
            attrs: { 'data-role': role, 'data-color': color, 'data-nb': nb },
          }),
        ),
      );
    }),
  );
}
