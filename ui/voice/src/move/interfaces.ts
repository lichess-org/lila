import { Api as CgApi } from 'chessground/api';
import * as cg from 'chessground/types';
import { PromotionCtrl } from 'chess/promotion';
import { VoiceModule } from '../interfaces';

export interface RootCtrl {
  chessground: CgApi;
  auxMove: (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined) => void;
  redraw: () => void;
  flipNow: () => void;
  offerDraw?: (v: boolean, immediately?: boolean) => void;
  takebackYes?: () => void;
  resign?: (v: boolean, immediately?: boolean) => void;
  rematch?: (accept?: boolean) => boolean;
  next?: () => void;
  vote?: (v: boolean) => void;
  solve?: () => void;
}

export interface VoiceMove extends VoiceModule {
  update: (fen: string, yourMove?: boolean) => void;
  promotionHook: () => (ctrl: PromotionCtrl, roles: cg.Role[] | false) => void;
  confirm: (request: string, callback: (v: boolean) => void) => void;
}

export interface VoiceMoveOpts {}
export interface Match {
  cost: number;
  isSan?: boolean;
}
