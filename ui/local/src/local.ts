import { attributesModule, classModule, init } from 'snabbdom';
import type { RoundController } from 'round';
import { GameCtrl } from './gameCtrl';
import { BotCtrl } from './botCtrl';
import { Assets } from './assets';
import { showSetupDialog } from './setupDialog';
import view from './gameView';
import type { LocalPlayOpts } from './types';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts): Promise<void> {
  opts.setup ??= JSON.parse(localStorage.getItem('local.setup') ?? '{}');

  const botCtrl = await new BotCtrl().init(opts.bots, new Assets());
  const gameCtrl = new GameCtrl(opts, botCtrl, redraw);

  const el = document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);
  let vnode = patch(el, view(gameCtrl));

  gameCtrl.round = await site.asset.loadEsm<RoundController>('round', { init: gameCtrl.proxy.roundOpts });

  redraw();
  if (!opts.setup?.go) {
    showSetupDialog(botCtrl, opts.setup, gameCtrl);
    return;
  }

  function redraw() {
    vnode = patch(vnode, view(gameCtrl));
    gameCtrl.round.redraw();
  }
}
