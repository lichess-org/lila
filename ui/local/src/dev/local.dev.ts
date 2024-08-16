import { attributesModule, classModule, init } from 'snabbdom';
import { GameCtrl } from '../gameCtrl';
import { DevCtrl } from './devCtrl';
import { DevAssets, AssetList } from './devAssets';
import { renderDevView } from './devView';
import { BotCtrl } from '../botCtrl';
import { ShareCtrl } from './shareCtrl';
import { env } from '../localEnv';
import { renderGameView } from '../gameView';
import type { RoundController } from 'round';
import type { LocalPlayOpts } from '../types';

const patch = init([classModule, attributesModule]);

interface LocalPlayDevOpts extends LocalPlayOpts {
  assets: AssetList;
}

export async function initModule(opts: LocalPlayDevOpts): Promise<void> {
  if (window.screen.width < 1260) return;

  opts.setup ??= JSON.parse(localStorage.getItem('local.dev.setup') ?? '{}');
  opts.dev = true;

  env.redraw = redraw;
  env.bot = new BotCtrl();
  env.share = new ShareCtrl();
  env.assets = new DevAssets(opts.assets);
  env.dev = new DevCtrl();
  env.game = new GameCtrl(opts);
  await Promise.all([env.bot.init(opts.bots), env.dev.init()]);
  env.game.init();

  const el = document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);

  let vnode = patch(el, renderGameView(renderDevView()));

  env.round = await site.asset.loadEsm<RoundController>('round', { init: env.game.proxy.roundOpts });
  redraw();

  function redraw() {
    vnode = patch(vnode, renderGameView(renderDevView()));
    env.round.redraw();
  }
}
