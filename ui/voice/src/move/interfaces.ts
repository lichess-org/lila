import { PromotionCtrl } from 'chess/promotion';
import { VoiceModule } from '../interfaces';
import * as cg from 'chessground/types';

export interface VoiceMove extends VoiceModule {
  update: (fen: string, canMove: boolean, chessground?: CgApi) => void;
  promotionHook: () => (ctrl: PromotionCtrl, roles: cg.Role[] | false) => void;
  listenForResponse: (request: string, action: (v: boolean) => void) => void;
  question: () => QuestionOpts | false;
}

export interface VoiceMoveOpts {}
export interface Match {
  cost: number;
  isSan?: boolean;
}
