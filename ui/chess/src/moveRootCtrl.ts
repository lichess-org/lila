import * as cg from 'chessground/types';

export interface MoveRootCtrl {
  pluginMove: (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined) => void;
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

export interface MoveUpdate {
  fen: string;
  canMove: boolean;
  cg?: CgApi;
}
