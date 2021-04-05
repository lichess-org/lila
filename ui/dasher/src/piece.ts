import { h, VNode } from 'snabbdom';

import * as xhr from 'common/xhr';
import { Redraw, Open, bind, header } from './util';

type Piece = string;

interface PieceDimData {
  current: Piece;
  list: Piece[];
}

export interface PieceData {
  d2: PieceDimData;
  d3: PieceDimData;
}

export interface PieceCtrl {
  dimension: () => keyof PieceData;
  data: () => PieceDimData;
  trans: Trans;
  set(t: Piece): void;
  open: Open;
}

export function ctrl(
  data: PieceData,
  trans: Trans,
  dimension: () => keyof PieceData,
  redraw: Redraw,
  open: Open
): PieceCtrl {
  function dimensionData() {
    return data[dimension()];
  }

  return {
    dimension,
    trans,
    data: dimensionData,
    set(t: Piece) {
      const d = dimensionData();
      d.current = t;
      applyPiece(t, d.list, dimension() === 'd3');
      xhr
        .text('/pref/pieceSet' + (dimension() === 'd3' ? '3d' : ''), {
          body: xhr.form({ set: t }),
          method: 'post',
        })
        .catch(() => lichess.announce({ msg: 'Failed to save piece set  preference' }));
      redraw();
    },
    open,
  };
}

export function view(ctrl: PieceCtrl): VNode {
  const d = ctrl.data();

  return h('div.sub.piece.' + ctrl.dimension(), [
    header(ctrl.trans.noarg('pieceSet'), () => ctrl.open('links')),
    h('div.list', d.list.map(pieceView(d.current, ctrl.set, ctrl.dimension() == 'd3'))),
  ]);
}

function pieceImage(t: Piece, is3d: boolean) {
  if (is3d) {
    const preview = t == 'Staunton' ? '-Preview' : '';
    return `images/staunton/piece/${t}/White-Knight${preview}.png`;
  }
  return `piece/${t}/wN.svg`;
}

function pieceView(current: Piece, set: (t: Piece) => void, is3d: boolean) {
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
          attrs: { style: `background-image:url(${lichess.assetUrl(pieceImage(t, is3d))})` },
        }),
      ]
    );
}

function applyPiece(t: Piece, list: Piece[], is3d: boolean) {
  if (is3d) {
    $('body').removeClass(list.join(' ')).addClass(t);
  } else {
    const sprite = document.getElementById('piece-sprite') as HTMLLinkElement;
    sprite.href = sprite.href.replace(/\w+\.css/, t + '.css');
  }
}
