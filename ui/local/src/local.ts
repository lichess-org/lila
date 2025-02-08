import { attributesModule, classModule, init } from 'snabbdom';
import type { RoundController } from 'round';
import { LocalDb } from 'game/localDb';
import { GameCtrl } from './gameCtrl';
import { BotCtrl } from './botCtrl';
import { Assets } from './assets';
import { showSetupDialog } from './setupDialog';
import { env, makeEnv } from './localEnv';
import { renderGameView } from './gameView';
import type { LocalPlayOpts } from './types';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts): Promise<void> {
  makeEnv({
    redraw,
    bot: new BotCtrl(),
    assets: new Assets(),
    db: new LocalDb(),
    game: new GameCtrl(opts),
  });
  await Promise.all([env.db.init(), env.bot.init(opts.bots), env.assets.init()]);
  await env.game.init();

  const el = document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);
  let vnode = patch(el, renderGameView());

  env.round = await site.asset.loadEsm<RoundController>('round', { init: env.game.proxy.roundOpts });
  redraw();
  if (!opts.localGameId && (location.hash === '#new' || !opts.setup?.go)) {
    showSetupDialog(opts.setup);
    return;
  }

  function redraw() {
    vnode = patch(vnode, renderGameView());
    env.round.redraw();
  }
}
