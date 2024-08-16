import { attributesModule, classModule, init } from 'snabbdom';
import type { RoundController } from 'round';
import { GameCtrl } from './gameCtrl';
import { BotCtrl } from './botCtrl';
import { Assets } from './assets';
import { showSetupDialog } from './setupDialog';
import { env } from './localEnv';
import { renderGameView } from './gameView';
import type { LocalPlayOpts } from './types';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts): Promise<void> {
  opts.setup ??= JSON.parse(localStorage.getItem('local.setup') ?? '{}');

  env.redraw = redraw;
  env.assets = new Assets();
  env.bot = await new BotCtrl().init(opts.bots);
  env.game = new GameCtrl(opts);
  env.game.init();

  const el = document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);
  let vnode = patch(el, renderGameView());

  env.round = await site.asset.loadEsm<RoundController>('round', { init: env.game.proxy.roundOpts });
  redraw();
  if (!opts.setup?.go) {
    showSetupDialog(env.bot, opts.setup);
    return;
  }

  function redraw() {
    vnode = patch(vnode, renderGameView());
    env.round.redraw();
  }
}
