import { attributesModule, classModule, init } from 'snabbdom';
import { showSetupDialog } from './setupDialog';
import { GameCtrl } from './gameCtrl';
import { LocalDb } from './localDb';
import { DevBotCtrl } from './devBotCtrl';
import { DevAssets } from './devAssets';
import { env, makeEnv } from './devEnv';
import { renderGameView } from './gameView';
import type { LocalPlayOpts, LocalSetup } from 'lib/bot/types';
import type { RoundController } from 'round';

const patch = init([classModule, attributesModule]);

type SetupOpts = LocalSetup & { id?: string; go?: true };

export default async function initModule(opts: LocalPlayOpts): Promise<void> {
  makeEnv({
    redraw: () => {},
    bot: new DevBotCtrl(),
    db: new LocalDb(),
    game: new GameCtrl(opts as LocalPlayOpts),
    assets: new DevAssets(),
  });
  await Promise.all([env.db.init(), env.bot.init()]);
  const setup = hashOpts();
  env.game.load({
    ...JSON.parse(localStorage.getItem('botdev.user') ?? '{}'),
    ...(setup.id || !Object.keys(setup).length ? await env.db.get(setup.id) : setup),
  });

  const el = document.querySelector('main') ?? document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);

  let vnode = patch(el, renderGameView());

  env.round = await site.asset.loadEsm<RoundController>('round', { init: env.game.proxy.roundOpts });
  redraw();

  if ('go' in setup || 'id' in setup) return;
  showSetupDialog(JSON.parse(localStorage.getItem('botdev.user') || '{}'));

  function redraw() {
    vnode = patch(vnode, renderGameView());
    env.round.redraw();
  }
}

function hashOpts(): SetupOpts {
  const params = location.hash
    .slice(1)
    .split('&')
    .map(p => decodeURIComponent(p).split('='))
    .filter(p => p.length === 2);
  const opts = Object.fromEntries(params);
  if ('initial' in opts) opts.initial = Number(opts.initial);
  if ('increment' in opts) opts.increment = Number(opts.increment);
  return opts;
}
