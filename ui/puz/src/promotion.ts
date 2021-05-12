import { h } from 'snabbdom';
import { bind, onInsert } from './util';
import { Api as CgApi } from 'shogiground/api';
import { Config as CgConfig } from 'shogiground/config';
import * as cgUtil from 'shogiground/util';
import { Role } from 'shogiground/types';
import { MaybeVNode, Redraw, Promotion } from './interfaces';
import {defined} from 'common';
import {canPiecePromote, promote as sPromote} from 'shogiops/util';
import {parseChessSquare} from 'shogiops/compat';

export default function (
  withGround: <A>(f: (cg: CgApi) => A) => A | false,
  makeCgOpts: () => CgConfig,
  redraw: Redraw
): Promotion {
  let promoting: any = false;

  function start(orig: Key, dest: Key, callback: (orig: Key, key: Key, prom: boolean) => void) {
    return !!withGround(g => {
      const piece = g.state.pieces.get(dest);
      if (!defined(piece)) return false;
      if (canPiecePromote(piece, parseChessSquare(orig)!, parseChessSquare(dest)!)) {
        promoting = {
          orig: orig,
          dest: dest,
          role: piece.role,
          callback: callback,
        };
        redraw();
        return true;
      }
      return false;
    });
  }

  function promote(g: CgApi, key: Key, role: Role): void {
    const piece = g.state.pieces.get(key);
    if (piece && ['pawn', 'lance', 'knight', 'silver', 'bishop', 'rook'].includes(piece.role)) {
      g.setPieces(
        new Map([
          [
            key,
            {
              color: piece.color,
              role,
              promoted: true,
            },
          ],
        ])
      );
    }
  }

  function finish(role: Role): void {
    const promoted = !['pawn', 'lance', 'knight', 'silver', 'bishop', 'rook'].includes(role);
    if (promoting && promoted) withGround(g => promote(g, promoting.dest, role));
    if (promoting.callback) promoting.callback(promoting.orig, promoting.dest, promoted);
    promoting = false;
  }

  function cancel(): void {
    if (promoting) {
      promoting = false;
      withGround(g => g.set(makeCgOpts()));
      redraw();
    }
  }

  function renderPromotion(dest: Key, pieces: Role[], color: Color, orientation: Color): MaybeVNode {
    if (!promoting) return;

    let left = (8 - cgUtil.key2pos(dest)[0]) * 11.11;

    if (orientation === 'sente') left = cgUtil.key2pos(dest)[0] * 11.11;
    const vertical = color === orientation ? 'top' : 'bottom';

    return h(
      'div#promotion-choice.' + vertical,
      {
        hook: onInsert(el => {
          el.addEventListener('click', cancel);
          el.oncontextmenu = () => false;
        }),
      },
      pieces.map(function (serverRole, i) {
        let top = (color === orientation ? i : 8 - i) * 11.11;
        if (orientation === 'sente') top = (9 - (i + cgUtil.key2pos(dest)[1])) * 11.11 + 0.35;

        return h(
          'square',
          {
            attrs: {
              style: 'top: ' + top + '%;left: ' + left + '%',
            },
            hook: bind('click', e => {
              e.stopPropagation();
              finish(serverRole);
            }),
          },
          [h('piece.' + serverRole + '.' + color)]
        );
      })
    );
  }

  return {
    start,
    cancel,
    view() {
      if (!promoting) return;
      let pieces: Role[] = [];
      withGround(g => {
        pieces = g.state.turnColor === 'sente'
          ? [sPromote(promoting.role), promoting.role]
          : [promoting.role, sPromote(promoting.role)];
      });
      return (
        withGround(g =>
          renderPromotion(promoting.dest, pieces, cgUtil.opposite(g.state.turnColor), g.state.orientation)
        ) || null
      );
    },
  };
}
