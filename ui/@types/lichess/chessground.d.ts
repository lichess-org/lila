import { Api } from '../../../node_modules/chessground/dist/api';
import { Config } from '../../../node_modules/chessground/dist/config';
import * as cg from '../../../node_modules/chessground/dist/types';

declare global {
  type CgApi = Api;
  type CgConfig = Config;
  namespace Cg {
    type Role = cg.Role;
    type Key = cg.Key;
    type FEN = cg.FEN;
  }
}
