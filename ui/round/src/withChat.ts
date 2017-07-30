import * as chat from 'chat';
import * as main from './main';
import { RoundOpts } from './interfaces';
import boot from './boot';

export function app(opts: RoundOpts): main.RoundApi {
  return main.app(opts, chat.patch);
}
export { boot };

window.LichessChat = chat;
