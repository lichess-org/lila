import type { PromotionCtrl } from 'chess/promotion';
import type { MoveUpdate } from 'chess/moveRootCtrl';
import type { VoiceModule } from '../interfaces';

export interface VoiceMove extends VoiceModule {
  update: (up: MoveUpdate) => void;
  promotionHook: () => (ctrl: PromotionCtrl, roles: Role[] | false) => void;
  listenForResponse: (request: string, action: (v: boolean) => void) => void;
  question: () => QuestionOpts | false;
}

export interface Match {
  cost: number;
  isSan?: boolean;
}
