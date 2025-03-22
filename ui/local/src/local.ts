import { attributesModule, classModule, init } from 'snabbdom';
import type { RoundController } from 'round';
import { LocalDb } from './localDb';
import { GameCtrl } from './gameCtrl';
import { BotCtrl } from './botCtrl';
import { Assets } from './assets';
//import { showSetupDialog } from './dev/setupDialog';
import { type LocalEnv, env, makeEnv } from './localEnv';
import { renderGameView } from './gameView';
import type { LocalPlayOpts } from './types';
import makeZerofish from 'zerofish';

const patch = init([classModule, attributesModule]);
const zerofish = makeZerofish({
  locator: (file: string) => site.asset.url(`npm/${file}`, { documentOrigin: file.endsWith('js') }),
  nonce: document.body.dataset.nonce,
});

// type SetupOpts = LocalSetup & { id?: string; go?: true };

export async function initModule(opts: LocalPlayOpts): Promise<LocalEnv> {
  makeEnv({
    redraw,
    bot: new BotCtrl(await zerofish),
    assets: new Assets(),
    db: new LocalDb(),
    game: new GameCtrl(opts),
  });
  await Promise.all([env.db.init(), env.bot.init(opts.bots)]);
  //const setup = hashOpts();
  //env.game.load('id' in setup ? await env.db.get(setup.id) : setup);

  const el = document.querySelector('#main-wrap > main') as HTMLElement;
  let vnode = patch(el, renderGameView());

  function redraw() {
    vnode = patch(vnode, renderGameView());
    env.round.redraw();
  }

  env.round = await site.asset.loadEsm<RoundController>('round', { init: env.game.proxy.roundOpts });
  redraw();

  //if ('go' in setup || 'id' in setup)
  return env;

  //showSetupDialog(JSON.parse(localStorage.getItem('local.setup') ?? '{}'));
}

// function hashOpts(): SetupOpts {
//   const params = location.hash
//     .slice(1)
//     .split('&')
//     .map(p => decodeURIComponent(p).split('='))
//     .filter(p => p.length === 2);
//   const opts = Object.fromEntries(params);
//   if ('initial' in opts) opts.initial = Number(opts.initial);
//   if ('increment' in opts) opts.increment = Number(opts.increment);
//   return opts;
// }
