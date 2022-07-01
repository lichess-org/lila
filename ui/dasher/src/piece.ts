import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Redraw, bind, header, Close } from './util';

type Piece = string;

export interface PieceData {
  current: Piece;
  list: Piece[];
}

export interface PieceCtrl {
  data: PieceData;
  trans: Trans;
  set(t: Piece): void;
  close: Close;
}

export function ctrl(data: PieceData, trans: Trans, redraw: Redraw, close: Close): PieceCtrl {
  return {
    trans,
    data: data,
    set(t: Piece) {
      data.current = t;
      applyPiece(t);
      $.post('/pref/pieceSet', {
        set: t,
      }).fail(() => window.lishogi.announce({ msg: 'Failed to save piece set preference' }));
      redraw();
    },
    close,
  };
}

export function view(ctrl: PieceCtrl): VNode {
  return h('div.sub.piece.', [
    header(ctrl.trans.noarg('pieceSet'), () => ctrl.close()),
    h('div.list', ctrl.data.list.map(pieceView(ctrl.data.current, ctrl.set))),
  ]);
}

function pieceImage(t: Piece) {
  return `piece/${t}/0KI.${isPngPiece(t) ? 'png' : 'svg'}`;
}

function isPngPiece(t: Piece): boolean {
  return t === 'Portella' || t === 'Portella_2Kanji' || t === 'Intl_Portella';
}

function pieceView(current: Piece, set: (t: Piece) => void) {
  return (t: Piece) =>
    h(
      'a.no-square',
      {
        attrs: { title: t },
        hook: bind('click', () => set(t)),
        class: { active: current === t },
      },
      [
        h('piece', {
          attrs: {
            style: `background-image:url(${window.lishogi.assetUrl(pieceImage(t))}); will-change: ${
              isPngPiece(t) ? 'transform' : 'auto'
            }`,
          },
        }),
      ]
    );
}

function applyPiece(t: Piece) {
  const sprite = $('#piece-sprite');
  sprite.attr('href', sprite.attr('href').replace(/\w+\.css/, t + '.css'));
}
