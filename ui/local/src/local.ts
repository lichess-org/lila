import { attributesModule, classModule, init } from 'snabbdom';
import type { RoundController } from 'round';
import { GameCtrl } from './gameCtrl';
import { BotCtrl } from './botCtrl';
import { AssetDb } from './assetDb';
import { SetupDialog } from './setupDialog';
import view from './gameView';
import type { LocalPlayOpts, Libot } from './types';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts): Promise<void> {
  const botCtrl = await new BotCtrl(new AssetDb()).init(opts.bots);

  if (localStorage.getItem('local.setup')) {
    if (!opts.setup) opts.setup = JSON.parse(localStorage.getItem('local.setup')!);
  }
  if (opts.setup) {
    if (!opts.setup.go) {
      new SetupDialog(botCtrl, opts.setup, true);
      return;
    }
    botCtrl.whiteUid = opts.setup.white;
    botCtrl.blackUid = opts.setup.black;
  }

  const ctrl = new GameCtrl(opts, botCtrl, redraw);
  const el = document.createElement('main');

  document.getElementById('main-wrap')?.appendChild(el);
  let vnode = patch(el, view(ctrl));

  ctrl.round = await site.asset.loadEsm<RoundController>('round', { init: ctrl.roundOpts });

  redraw();

  function redraw() {
    vnode = patch(vnode, view(ctrl));
    ctrl.round.redraw();
  }
}
