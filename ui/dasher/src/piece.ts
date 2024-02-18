import { VNode, h } from 'snabbdom';
import { Close, Redraw, bind, header } from './util';

type PieceSetKey = string;
type PieceSet = {
  key: PieceSetKey;
  name: string;
};
type Tab = 'standard' | 'chushogi' | 'kyotoshogi';

export interface PieceSetData {
  current: PieceSetKey;
  list: PieceSet[];
}

export interface PieceCtrl {
  trans: Trans;
  std: PieceSetData;
  chu: PieceSetData;
  kyo: PieceSetData;
  activeTab: Tab;
  setActiveTab(s: Tab): void;
  set(t: PieceSetKey): void;
  close: Close;
}

export function ctrl(
  std: PieceSetData,
  chu: PieceSetData,
  kyo: PieceSetData,
  trans: Trans,
  redraw: Redraw,
  close: Close
): PieceCtrl {
  const isChushogi = !!document.body.querySelector('.main-v-chushogi'),
    isKyotoshogi = !isChushogi && !!document.body.querySelector('.main-v-kyotoshogi');
  return {
    trans,
    std: std,
    chu: chu,
    kyo: kyo,
    activeTab: isChushogi ? 'chushogi' : isKyotoshogi ? 'kyotoshogi' : 'standard',
    setActiveTab(s: Tab) {
      this.activeTab = s;
      redraw();
      if (s !== 'standard') window.scrollTo(0, 0);
    },
    set(key: PieceSetKey) {
      if (isChu(key)) chu.current = key;
      else if (isKyo(key)) kyo.current = key;
      else std.current = key;

      applyPiece(key);
      $.post(`/pref/${isChu(key) ? 'chuPieceSet' : isKyo(key) ? 'kyoPieceSet' : 'pieceSet'}`, {
        set: key,
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
          const tab = ((e.target as HTMLElement).dataset.tab || 'standard') as Tab;
          ctrl.setActiveTab(tab);
        }),
      },
      ['standard', 'chushogi', 'kyotoshogi'].map((v: Tab) =>
        h('div', { attrs: { 'data-tab': v }, class: { active: ctrl.activeTab === v } }, ctrl.trans.noarg(v))
      )
    ),
  ]);
}

function pieceImage(key: PieceSetKey) {
  const piece = isChu(key) ? '0_KIRIN' : isKyo(key) ? '0KY' : '0KI';
  return `piece/${key}/${piece}.${isPngPiece(key) ? 'png' : 'svg'}`;
}

function isChu(key: PieceSetKey): boolean {
  return key.startsWith('Chu_');
}
function isKyo(key: PieceSetKey): boolean {
  return key.startsWith('Kyo_');
}

function isPngPiece(key: PieceSetKey): boolean {
  return [
    'Portella',
    'Portella_2Kanji',
    'Intl_Portella',
    'joyful',
    'pixel',
    'Chu_Eigetsu_Gyoryu',
    'Kyo_joyful',
  ].includes(key);
}

function pieceView(current: PieceSetKey) {
  return (p: PieceSet) =>
    h(
      'a.no-square',
      {
        attrs: { title: p.name, 'data-value': p.key },
        class: { active: current === p.key },
      },
      [
        h('piece', {
          attrs: {
            style: `background-image:url(${window.lishogi.assetUrl(pieceImage(p.key))}); will-change: ${
              isPngPiece(p.key) && p.key !== 'pixel' ? 'transform' : 'auto'
            }`,
          },
        }),
      ]
    );
}

function applyPiece(key: PieceSetKey) {
  const sprite = $(`#${isChu(key) ? 'chu-' : isKyo(key) ? 'kyo-' : ''}piece-sprite`);
  if (sprite.length) sprite.attr('href', sprite.attr('href').replace(/\w+\.css/, key + '.css'));

  if (isChu(key)) document.body.dataset.chuPieceSet = key;
  else if (isKyo(key)) document.body.dataset.kyoPieceSet = key;
  else document.body.dataset.pieceSet = key;
}
