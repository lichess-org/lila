import type { Api as SgApi } from 'shogiground/api';
import type { Config } from 'shogiground/config';
import { usiToSquareNames } from 'shogiops/compat';
import { forsythToRole, roleToForsyth } from 'shogiops/sfen';
import { handRoles } from 'shogiops/variant/util';
import { loadChushogiPieceSprite, loadKyotoshogiPieceSprite } from './assets';
import * as domData from './data';

export const initAll = (): void => {
  const minis = Array.from(
    document.getElementsByClassName('parse-sfen') as HTMLCollectionOf<HTMLElement>,
  );
  minis.forEach(innerInit);

  const liveMinis = minis.map(m => m.dataset.live).filter(m => m !== undefined);
  if (liveMinis.length) startWatching(liveMinis);
};

export const initOneWithState = (node: HTMLElement, state: MiniBoardState): void => {
  node.dataset.sfen = state.sfen;
  node.dataset.color = state.orientation;
  node.dataset.variant = state.variant;
  if (state.lastMove) node.dataset.lastmove = state.lastMove;
  if (state.playable) node.dataset.playable = '1';
  if (state.noHands) node.dataset.nohands = '1';
  if (state.live) node.dataset.live = state.live;
  initOne(node);
};

export const initOne = (node: HTMLElement): void => {
  innerInit(node);
  const live = node.dataset.live;
  if (live) startWatching([live]);
};

const innerInit = (node: HTMLElement): void => {
  const state = parseNode(node);
  if (state) {
    let sgWrap = node.firstChild as HTMLElement | null;
    if (!sgWrap) {
      sgWrap = document.createElement('div');
      sgWrap.classList.add('sg-wrap');
      node.appendChild(sgWrap);
    }

    if (state.variant === 'chushogi') loadChushogiPieceSprite();
    else if (state.variant === 'kyotoshogi') loadKyotoshogiPieceSprite();

    const splitSfen = state.sfen.split(' '),
      config: Config = {
        orientation: state.orientation,
        coordinates: {
          enabled: false,
        },
        viewOnly: !state.playable,
        sfen: { board: splitSfen[0], hands: splitSfen[2] },
        hands: {
          inlined: state.variant !== 'chushogi' && !state.noHands,
          roles: handRoles(state.variant),
        },
        lastDests: state.lastMove ? usiToSquareNames(state.lastMove) : undefined,
        forsyth: {
          fromForsyth: forsythToRole(state.variant),
          toForsyth: roleToForsyth(state.variant),
        },
        drawable: {
          enabled: false,
          visible: false,
        },
      };
    domData.set<SgApi>(node, 'shogiground', window.Shogiground(config, { board: sgWrap }));
    sgWrap.classList.remove('preload');
    node.classList.remove('parse-sfen');
  }
};

export function update(node: HTMLElement, sfen: Sfen, lm?: Usi): void {
  const sg = domData.get<SgApi>(node, 'shogiground');
  if (sg) {
    node.dataset.sfen = sfen;
    node.dataset.lastmove = lm;
    const splitSfen = sfen.split(' ');
    sg.set({
      sfen: { board: splitSfen[0], hands: splitSfen[2] },
      lastDests: lm ? usiToSquareNames(lm) : undefined,
    });
  } else {
    console.warn('No sg for update', node);
  }
}

const parseNode = (node: HTMLElement): MiniBoardState | undefined => {
  const sfen = node.dataset.sfen;
  if (!sfen) return undefined;

  return {
    sfen,
    orientation: (node.dataset.color || 'sente') as Color,
    variant: (node.dataset.variant || 'standard') as VariantKey,
    lastMove: node.dataset.lastmove,
    playable: !!node.dataset.playable,
    noHands: !!node.dataset.nohands,
    live: node.dataset.live,
  };
};

const startWatching = (ids: string[]) => {
  if (!window.lishogi.socket) return;
  if (!window.lishogi.socket?.isOpen) {
    setTimeout(() => startWatching(ids), 500);
  } else window.lishogi.socket.send('startWatching', ids.join(' '));
};

export interface MiniBoardState {
  variant: VariantKey;
  sfen: string;
  orientation: Color;
  lastMove?: string;
  playable?: boolean;
  noHands?: boolean;
  live?: string;
}
