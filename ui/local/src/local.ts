import { attributesModule, classModule, init } from 'snabbdom';
import type { RoundController } from 'round';
import { LocalDb } from './localDb';
import { GameCtrl } from './gameCtrl';
import { BotCtrl } from './botCtrl';
import { Assets } from './assets';
import { showSetupDialog } from './setupDialog';
import { env, makeEnv } from './localEnv';
import { renderGameView } from './gameView';
import type { LocalPlayOpts, LocalSetup } from './types';

const patch = init([classModule, attributesModule]);

type SetupOpts = LocalSetup & { id?: string; go?: true };

export async function initModule(opts: LocalPlayOpts): Promise<void> {
  const setup = setupOpts();
  makeEnv({
    redraw,
    bot: new BotCtrl(),
    assets: new Assets(),
    db: new LocalDb(),
    game: new GameCtrl(opts),
  });
  await Promise.all([env.db.init(), env.bot.init(opts.bots), env.assets.init()]);
  env.game.load('id' in setup ? await env.db.get(setup.id) : setup);

  const el = document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);
  let vnode = patch(el, renderGameView());

  env.round = await site.asset.loadEsm<RoundController>('round', { init: env.game.proxy.roundOpts });
  redraw();

  if ('go' in setup || 'id' in setup) return;

  const lastSetup = localStorage.getItem('local.setup');
  showSetupDialog(lastSetup ? JSON.parse(lastSetup) : {});

  function redraw() {
    vnode = patch(vnode, renderGameView());
    env.round.redraw();
  }
}

function setupOpts(): SetupOpts {
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
