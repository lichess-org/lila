import { Api } from '../../../node_modules/chessground/dist/api';
import { Config } from '../../../node_modules/chessground/dist/config';
import * as cg from '../../../node_modules/chessground/dist/types';

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
