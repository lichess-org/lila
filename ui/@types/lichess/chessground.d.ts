import { Api } from '../../../node_modules/chessground/dist/api';
import { Config } from '../../../node_modules/chessground/dist/config';
import { FEN } from '../../../node_modules/chessground/dist/types';

declare global {
  type CgApi = Api;
  type CgConfig = Config;
  type CgFEN = FEN;
}
