import { init, classModule, attributesModule } from 'snabbdom';

import { BotOpts } from './interfaces';
import { BotPlayCtrl } from './ctrl';

// door #1 - right now you need a pref json and a list of BotInfo[] from the server.
//   with those, you can:
//
//   const env = await loadEsm<LocalEnv>('local', { init: { pref, bots } })
//
//   this gives you full access to your nemisis, LocalEnv.
//   the quickest way to bring things up right now.
//
// door #2 - move all the non-dev stuff directly into this module
//   ultimately there's no reason for the botPlay module to be separate from
//   the bot code (aside from wanting to keep it clean, which i totally get).
//
// door #3 - construct an env right here in botPlay.ts (using local.ts
//   as a model), import controllers directly and let esbuild bundle it all.
//   that way you have more control over RoundController, sides, redraws.
//
// right now botPlay.ts has a duplicative purpose with local.ts
// so feel free to make edit, maim, or delete it
//
// env.game.load({ ... }) is the way to interact with gameCtrl.
//
// burn after reading

export async function initModule(opts: BotOpts) {
  const element = document.querySelector('.bot-play-app') as HTMLElement,
    patch = init([classModule, attributesModule]);

  const ctrl = new BotPlayCtrl(opts, redraw);

  const blueprint = ctrl.view();
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, ctrl.view());
  }

  redraw();
}
