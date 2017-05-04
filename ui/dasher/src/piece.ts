import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Open, bind, header } from './util'

type Piece = string;

interface PieceDimData {
  current: Piece
  list: Piece[]
}

export interface PieceData {
  d2: PieceDimData
  d3: PieceDimData
}

export interface PieceCtrl {
  dimension: () => keyof PieceData
  data: () => PieceDimData
  set(t: Piece): void
  open: Open
}

export function ctrl(data: PieceData, dimension: () => keyof PieceData, redraw: Redraw, open: Open): PieceCtrl {

  function dimensionData() {
    return data[dimension()];
  }

  return {
    dimension,
    data: dimensionData,
    set(t: Piece) {
      const d = dimensionData();
      d.current = t;
      applyPiece(t, d.list);
      $.post('/pref/pieceSet' + (dimension() === 'd3' ? '3d' : ''), {
        set: t
      }, window.lichess.reloadOtherTabs);
      redraw();
    },
    open
  };
}

export function view(ctrl: PieceCtrl): VNode {

  const d = ctrl.data();

  return h('div.sub.piece.' + ctrl.dimension(), [
    header('Piece set', () => ctrl.open('links')),
    h('div.list', {
      attrs: { method: 'post', action: '/pref/soundSet' }
    }, d.list.map(pieceView(d.current, ctrl.set))),
    h('div.subs', [
      h('a', {
        hook: bind('click', () => ctrl.open('theme')),
        attrs: { 'data-icon': 'H' }
      }, 'Board theme')
    ])
  ]);
}

function pieceView(current: Piece, set: (t: Piece) => void) {
  return (t: Piece) => h('a.no-square', {
    hook: bind('click', () => set(t)),
    class: { active: current === t }
  }, [
    h('piece.' + t)
  ]);
}

function applyPiece(t: Piece, list: Piece[]) {

  const sprite = $('#piece-sprite');
  sprite.attr('href', sprite.attr('href').replace(/\w+\.css/, t + '.css'));
}
