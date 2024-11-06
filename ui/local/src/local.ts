import { attributesModule, classModule, init } from 'snabbdom';
import type { RoundController } from 'round';
import { GameCtrl } from './gameCtrl';
import { BotCtrl } from './botCtrl';
import { Assets } from './assets';
import { showSetupDialog } from './setupDialog';
import { env, initEnv } from './localEnv';
import { renderGameView } from './gameView';
import type { LocalPlayOpts } from './types';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts): Promise<void> {
  initEnv({ redraw, bot: new BotCtrl(), assets: new Assets(), game: new GameCtrl(opts) });
  await Promise.all([env.bot.init(opts.bots), env.assets.init()]);
  await env.game.init();

  const el = document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);
  let vnode = patch(el, renderGameView());

  env.round = await site.asset.loadEsm<RoundController>('round', { init: env.game.proxy.roundOpts });
  redraw();
  if (!opts.setup?.go || location.hash === '#new') {
    showSetupDialog(env.bot, opts.setup);
    return;
  }

  function redraw() {
    vnode = patch(vnode, renderGameView());
    env.round.redraw();
  }
}
