import { Api as CgApi } from 'chessground/api';
import * as cg from 'chessground/types';

export type InputMoveHandler = (fen: Fen, dests?: cg.Dests, yourMove?: boolean) => void;

export interface CrazyPocket {
  [role: string]: number;
}
export interface RootData {
  crazyhouse?: { pockets: [CrazyPocket, CrazyPocket] };
  game: { variant: { key: VariantKey } };
  player: { color: Color };
}

export interface ClockCtrl {
  millisOf: (color: Color) => number;
}

export interface RootCtrl {
  chessground: CgApi;
  clock?: ClockCtrl;
  crazyValid?: (role: cg.Role, key: cg.Key) => boolean;
  data: RootData;
  offerDraw?: (v: boolean, immediately?: boolean) => void;
  resign?: (v: boolean, immediately?: boolean) => void;
  sendMove: (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined, meta?: cg.MoveMetadata) => void;
  sendNewPiece?: (role: cg.Role, key: cg.Key, isPredrop: boolean) => void;
  submitMove?: (v: boolean) => void;
  userJumpPlyDelta?: (plyDelta: Ply) => void;
  redraw: () => void;
  next?: () => void;
  vote?: (v: boolean) => void;
}
