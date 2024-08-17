import { attributesModule, classModule, init } from 'snabbdom';
import { GameCtrl } from '../gameCtrl';
import { DevCtrl } from './devCtrl';
import { DevAssets, AssetList } from './devAssets';
import { renderDevView } from './devView';
import { BotCtrl } from '../botCtrl';
import { ShareCtrl } from './shareCtrl';
import { env, initEnv } from '../localEnv';
import { renderGameView } from '../gameView';
import type { RoundController } from 'round';
import type { LocalPlayOpts } from '../types';

const patch = init([classModule, attributesModule]);

interface LocalPlayDevOpts extends LocalPlayOpts {
  assets?: AssetList;
  pgn?: string;
  name?: string;
}

export async function initModule(opts: LocalPlayDevOpts): Promise<void> {
  if (opts.pgn && opts.name) {
    initEnv();
    env.bot = new BotCtrl();
    env.assets = new DevAssets();
    await Promise.all([env.bot.initBots(), env.assets.init()]);
    env.repo.importBook(opts.pgn, opts.name);
    return;
  }
  if (window.screen.width < 1260) return;

  opts.setup ??= JSON.parse(localStorage.getItem('local.dev.setup') ?? '{}');
  opts.dev = true;

  console.log(opts);
  initEnv(opts.userId, opts.username);

  env.redraw = redraw;
  env.bot = new BotCtrl();
  env.share = new ShareCtrl();
  env.assets = new DevAssets(opts.assets);
  env.dev = new DevCtrl();
  env.game = new GameCtrl(opts);
  await Promise.all([env.bot.init(opts.bots), env.dev.init(), env.assets.init()]);
  await env.game.init();

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
