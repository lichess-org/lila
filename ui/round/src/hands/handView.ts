import { h } from 'snabbdom';
import * as round from '../round';
import { drag, handKeys, selectToDrop, shadowDrop } from './handCtrl';
import * as cg from 'shogiground/types';
import RoundController from '../ctrl';
import { onInsert } from '../util';
import { Position } from '../interfaces';
import { lishogiVariantRules } from 'shogiops/compat';
import { handRoles, unpromote } from 'shogiops/variantUtil';
import { parseHands } from 'shogiops/sfen';
import { Hands } from 'shogiops/hand';

const eventNames1 = ['mousedown', 'touchmove'];
const eventNames2 = ['click'];
const eventNames3 = ['contextmenu'];

export default function pocket(ctrl: RoundController, color: Color, position: Position) {
  const step = round.plyStep(ctrl.data, ctrl.ply);
  const parsedHands = parseHands(step.sfen.split(' ')[2] || '');
  const hands = parsedHands.isErr ? Hands.empty() : parsedHands.value;
  const hand = hands[color];

  const droppedRole = ctrl.justDropped,
    dropMode = ctrl.shogiground?.state.dropmode,
    dropPiece = ctrl.shogiground?.state.dropmode.piece,
    shadowPiece = ctrl.shogiground?.state.drawable.piece,
    preDropRole = ctrl.preDrop,
    usablePos = position === (ctrl.flip ? 'top' : 'bottom'),
    usable = usablePos && !ctrl.replaying() && ctrl.isPlaying(),
    activeColor = color === ctrl.data.player.color;
  const capturedPiece = ctrl.justCaptured;
  const captured = capturedPiece && unpromote(lishogiVariantRules(ctrl.data.game.variant.key))(capturedPiece.role);
  return h(
    'div.pocket.pocket-' + position,
    {
      class: { usable },
      hook: onInsert(el => {
        eventNames1.forEach(name =>
          el.addEventListener(name, (e: cg.MouchEvent) => {
            if (position === (ctrl.flip ? 'top' : 'bottom') && handKeys.length == 0) drag(ctrl, e);
          })
        );
        eventNames2.forEach(name =>
          el.addEventListener(name, (e: cg.MouchEvent) => {
            if (position === (ctrl.flip ? 'top' : 'bottom') && handKeys.length == 0) selectToDrop(ctrl, e);
          })
        );
        eventNames3.forEach(name => {
          el.addEventListener(name, e => {
            shadowDrop(ctrl, e as cg.MouchEvent);
          });
        });
      }),
    },
    handRoles(lishogiVariantRules(ctrl.data.game.variant.key))
      .reverse()
      .map(role => {
        let nb = hand[role];
        const sp = role == shadowPiece?.role && color == shadowPiece?.color;
        const selectedSquare = dropMode?.active && dropPiece?.role == role && dropPiece?.color == color;
        if (activeColor) {
          if (droppedRole === role) nb--;
          if (captured === role) nb++;
        }
        return h(
          'div.pocket-c1',
          h(
            'div.pocket-c2',
            {
              class: {
                'shadow-piece': sp,
              },
            },
            h('piece.' + role + '.' + color, {
              class: {
                premove: activeColor && preDropRole === role,
                'selected-square': selectedSquare,
              },
              attrs: {
                'data-role': role,
                'data-color': color,
                'data-nb': nb,
              },
            })
          )
        );
      })
  );
}
