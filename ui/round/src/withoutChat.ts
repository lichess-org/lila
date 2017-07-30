import * as main from './main';
import { RoundOpts } from './interfaces';
import { init } from 'snabbdom';
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import boot from './boot';

export function app(opts: RoundOpts): main.RoundApi {
  return main.app(opts, init([klass, attributes]));
}
export { boot };
