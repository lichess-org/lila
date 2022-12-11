import m from './mithrilFix';
import chessground from 'chessground';
import * as ground from './ground';
import type { Square as Key } from 'chess.js';
import { LevelCtrl } from './level';
import { Ctrl } from './run/runCtrl';
import { PromotionRole } from './util';
const opposite = chessground.util.opposite;
const key2pos = chessground.util.key2pos;

let promoting:
  | false
  | {
      orig: Key;
      dest: Key;
      callback: PromotionCallback;
    } = false;

type PromotionCallback = (orig: Key, dest: Key, role: PromotionRole) => void;

export function start(orig: Key, dest: Key, callback: PromotionCallback) {
  const piece = ground.pieces()[dest];
  if (
    piece &&
    piece.role == 'pawn' &&
    ((dest[1] == '1' && piece.color == 'black') || (dest[1] == '8' && piece.color == 'white'))
  ) {
    promoting = {
      orig: orig,
      dest: dest,
      callback: callback,
    };
    m.redraw();
    return true;
  }
  return false;
}

function finish(role: PromotionRole) {
  if (promoting) {
    ground.promote(promoting.dest, role);
    promoting.callback(promoting.orig, promoting.dest, role);
  }
  promoting = false;
}

function renderPromotion(
  ctrl: Ctrl,
  dest: Key,
  pieces: PromotionRole[],
  color: Color,
  orientation: Color,
  explain: boolean
) {
  if (!promoting) return;

  let left = (8 - key2pos(dest)[0]) * 12.5;
  if (orientation === 'white') left = 87.5 - left;

  const vertical = color === orientation ? 'top' : 'bottom';

  return m('div#promotion-choice.' + vertical, [
    ...pieces.map(function (serverRole, i) {
      return m(
        'square',
        {
          style: vertical + ': ' + i * 12.5 + '%;left: ' + left + '%',
          onclick: function (e: Event) {
            e.stopPropagation();
            finish(serverRole);
          },
        },
        m('piece.' + serverRole + '.' + color)
      );
    }),
    explain ? renderExplanation(ctrl) : null,
  ]);
}

function renderExplanation(ctrl: Ctrl) {
  return m('div.explanation', [
    m('h2', ctrl.trans.noarg('pawnPromotion')),
    m('p', ctrl.trans.noarg('yourPawnReachedTheEndOfTheBoard')),
    m('p', ctrl.trans.noarg('itNowPromotesToAStrongerPiece')),
    m('p', ctrl.trans.noarg('selectThePieceYouWant')),
  ]);
}

export function view(ctrl: Ctrl, stage: LevelCtrl) {
  if (!promoting) return;
  const pieces: PromotionRole[] = ['queen', 'knight', 'rook', 'bishop'];

  return renderPromotion(
    ctrl,
    promoting.dest,
    pieces,
    opposite(ground.data().turnColor),
    ground.data().orientation,
    !!stage.blueprint.explainPromotion
  );
}

export function reset() {
  promoting = false;
}
