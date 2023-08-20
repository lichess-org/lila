import { Api } from '../../../node_modules/chessground/dist/api';
import { Config } from '../../../node_modules/chessground/dist/config';

declare global {
  type CgApi = Api;
  type CgConfig = Config;
}
