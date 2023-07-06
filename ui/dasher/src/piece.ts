import { VNode, h } from 'snabbdom';
import { Close, Redraw, bind, header } from './util';

type Piece = string;
type Tab = 'default' | 'chushogi' | 'kyotoshogi';

export interface PieceData {
  current: Piece;
  list: Piece[];
}

export interface PieceCtrl {
  trans: Trans;
  std: PieceData;
  chu: PieceData;
  kyo: PieceData;
  activeTab: Tab;
  setActiveTab(s: Tab): void;
  set(t: Piece): void;
  close: Close;
}

export function ctrl(
  std: PieceData,
  chu: PieceData,
  kyo: PieceData,
  trans: Trans,
  redraw: Redraw,
  close: Close
): PieceCtrl {
  const isChushogi = !!document.body.querySelector(
      'main.variant-chushogi, div.round__app.variant-chushogi, div.board-editor.variant-chushogi'
    ),
    isKyotoshogi =
      !isChushogi &&
      !!document.body.querySelector(
        'main.variant-kyotoshogi, div.round__app.variant-kyotoshogi, div.board-editor.variant-kyotoshogi'
      );
  return {
    trans,
    std: std,
    chu: chu,
    kyo: kyo,
    activeTab: isChushogi ? 'chushogi' : isKyotoshogi ? 'kyotoshogi' : 'default',
    setActiveTab(s: Tab) {
      this.activeTab = s;
      redraw();
      if (s !== 'default') window.scrollTo(0, 0);
    },
    set(t: Piece) {
      if (isChu(t)) chu.current = t;
      else if (isKyo(t)) kyo.current = t;
      else std.current = t;

      applyPiece(t);
      $.post(`/pref/${isChu(t) ? 'chuPieceSet' : isKyo(t) ? 'kyoPieceSet' : 'pieceSet'}`, {
        set: t,
      }).fail(() => window.lishogi.announce({ msg: 'Failed to save piece set preference' }));
      redraw();
    },
    close,
  };
}

export function view(ctrl: PieceCtrl): VNode {
  return h('div.sub.piece', [
    header(ctrl.trans.noarg('pieceSet'), () => ctrl.close()),
    h(
      'div.list-wrap',
      {
        hook: bind('click', e => {
          const pieceSet = (e.target as HTMLElement).dataset.value;
          if (pieceSet) ctrl.set(pieceSet);
        }),
      },
      ctrl.activeTab === 'chushogi'
        ? h('div.list', ctrl.chu.list.map(pieceView(ctrl.chu.current)))
        : ctrl.activeTab === 'kyotoshogi'
        ? h('div.list', ctrl.kyo.list.map(pieceView(ctrl.kyo.current)))
        : h('div.list', ctrl.std.list.map(pieceView(ctrl.std.current)))
    ),
    h(
      'a.piece-tabs',
      {
        hook: bind('click', e => {
          const tab = ((e.target as HTMLElement).dataset.tab || 'default') as Tab;
          ctrl.setActiveTab(tab);
        }),
      },
      ['default', 'chushogi', 'kyotoshogi'].map((v: Tab) =>
        h('div', { attrs: { 'data-tab': v }, class: { active: ctrl.activeTab === v } }, ctrl.trans.noarg(v))
      )
    ),
  ]);
}

function pieceImage(t: Piece) {
  const piece = isChu(t) ? '0_KIRIN' : isKyo(t) ? '0KY' : '0KI';
  return `piece/${t}/${piece}.${isPngPiece(t) ? 'png' : 'svg'}`;
}

function isChu(t: Piece): boolean {
  return t.startsWith('Chu_');
}
function isKyo(t: Piece): boolean {
  return t.startsWith('Kyo_');
}

function isPngPiece(t: Piece): boolean {
  return t === 'Portella' || t === 'Portella_2Kanji' || t === 'Intl_Portella' || t === 'joyful';
}

function pieceView(current: Piece) {
  return (t: Piece) =>
    h(
      'a.no-square',
      {
        attrs: { title: t, 'data-value': t },
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
  const sprite = $(`#${isChu(t) ? 'chu-' : isKyo(t) ? 'kyo-' : ''}piece-sprite`);
  if (sprite.length) sprite.attr('href', sprite.attr('href').replace(/\w+\.css/, t + '.css'));

  if (isChu(t)) document.body.dataset.chuPieceSet = t;
  else if (isKyo(t)) document.body.dataset.kyoPieceSet = t;
  else document.body.dataset.pieceSet = t;
}
