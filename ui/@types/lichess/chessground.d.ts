import { Api } from '@lichess-org/chessground/api';
import { Config } from '@lichess-org/chessground/config';
import * as cg from '@lichess-org/chessground/types';

declare global {
  type CgApi = Api;
  type CgConfig = Config;
  type Color = cg.Color;
  type Role = cg.Role;
  type Key = cg.Key;
  type FEN = cg.FEN;
  type Files = cg.File;
  type Ranks = cg.Rank;
  type Dests = cg.Dests;
  type Piece = cg.Piece;
}
