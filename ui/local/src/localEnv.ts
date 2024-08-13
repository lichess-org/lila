import type { BotCtrl } from './botCtrl';
import type { GameCtrl } from './gameCtrl';
import type { DevCtrl } from './dev/devCtrl';
import type { Assets } from './assets';
import type { ShareCtrl } from './dev/shareCtrl';
import type { DevAssets } from './dev/devAssets';

// currently unused, but the more instanciation in local.dev.ts, local.ts begins to look like
// a tangled mess, the more something like this will improve things

export class LocalEnv {
  bot: BotCtrl;
  game: GameCtrl;
  dev: DevCtrl;
  assets: Assets;
  share: ShareCtrl;
  get repo(): DevAssets {
    // delete me
    return this.assets as DevAssets;
  }
}

export const env: LocalEnv = new LocalEnv();

/*
local.dev.ts:


  opts.setup ??= JSON.parse(localStorage.getItem('local.dev.setup') ?? '{}');

  const botCtrl = new BotCtrl();
  const assetRepo = new DevAssets(opts.assets, botCtrl, new ShareCtrl(botCtrl));
  await botCtrl.init(opts.bots, assetRepo);
  const devCtrl = new DevCtrl(redraw);
  const gameCtrl = new GameCtrl(opts, botCtrl, redraw, devCtrl);

  const el = document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);

  let vnode = patch(el, renderGameView(gameCtrl, renderDevView(devCtrl)));

  gameCtrl.round = await site.asset.loadEsm<RoundController>('round', { init: gameCtrl.proxy.roundOpts });
  redraw();

local.ts:

  opts.setup ??= JSON.parse(localStorage.getItem('local.setup') ?? '{}');

  const botCtrl = await new BotCtrl().init(opts.bots, new Assets());
  const gameCtrl = new GameCtrl(opts, botCtrl, redraw);

  const el = document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);
  let vnode = patch(el, view(gameCtrl));

  gameCtrl.round = await site.asset.loadEsm<RoundController>('round', { init: gameCtrl.proxy.roundOpts });



local.setup.ts:

  if (localStorage.getItem('local.setup')) {
    opts = { ...opts, ...JSON.parse(localStorage.getItem('local.setup')!) };
  }
  opts.bots ??= (await fetch('/local/bots').then(res => res.json())).bots;
  showSetupDialog(await new BotCtrl().initBots(opts.bots!, new Assets()), opts);

*/
