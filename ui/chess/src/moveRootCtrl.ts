import { Api as CgApi } from 'chessground/api';
import * as cg from 'chessground/types';

export interface MoveRootCtrl {
  chessground: CgApi;
  auxMove: (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined) => void;
  redraw: () => void;
  flipNow: () => void;
  offerDraw?: (v: boolean, immediately?: boolean) => void;
  takebackYes?: () => void;
  resign?: (v: boolean, immediately?: boolean) => void;
  rematch?: (accept?: boolean) => boolean;
  nextPuzzle?: () => void;
  vote?: (v: boolean) => void;
  solve?: () => void;
  blindfold?: (v?: boolean) => boolean;
  speakClock?: () => void;
  goBerserk?: () => void;
}
