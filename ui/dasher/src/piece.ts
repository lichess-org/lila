import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Redraw, Open, bind, header } from './util';

type Piece = string;

export interface PieceData {
  current: Piece;
  list: Piece[];
}

export interface PieceCtrl {
  data: PieceData;
  trans: Trans;
  set(t: Piece): void;
  open: Open;
}

export function ctrl(data: PieceData, trans: Trans, redraw: Redraw, open: Open): PieceCtrl {
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
    open,
  };
}

export function view(ctrl: PieceCtrl): VNode {
  return h('div.sub.piece.', [
    header(ctrl.trans.noarg('pieceSet'), () => ctrl.open('links')),
    h('div.list', ctrl.data.list.map(pieceView(ctrl.data.current, ctrl.set))),
  ]);
}

function pieceImage(t: Piece) {
  return `piece/${t}/0KI.svg`;
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
            style: `background-image:url(${window.lishogi.assetUrl(pieceImage(t))})`,
          },
        }),
      ]
    );
}

function applyPiece(t: Piece) {
  const sprite = $('#piece-sprite');
  sprite.attr('href', sprite.attr('href').replace(/\w+\.css/, t + '.css'));
}
