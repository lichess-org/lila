import { attributesModule, classModule, init } from 'snabbdom';
//import { RoundOpts, RoundData, RoundSocket } from 'round';
import { GameCtrl } from './gameCtrl';
import { BotCtrl } from './botCtrl';
import { AssetDb } from './assetDb';
import { SetupDialog } from './setupDialog';
import view from './gameView';
import type { MoveRootCtrl } from 'game';
import type { LocalPlayOpts, Libot } from './types';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts): Promise<void> {
  const botCtrl = await new BotCtrl().init();
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

  ctrl.round = await site.asset.loadEsm<MoveRootCtrl>('round', { init: ctrl.roundOpts });

  redraw();

  function redraw() {
    vnode = patch(vnode, view(ctrl));
    ctrl.round.redraw();
  }
}

// async stuff that must be serialized is done so here
/*const localCode = navigator.language;
  let i18n = opts.i18n;
  try {
    const store = await objectStorage<any>({ db: 'i18n', store: `local` });
    if (i18n) await store.put(localCode, i18n);
    else i18n = await store.get(localCode);
  } catch (e) {
    console.log('oh noes!', e);
  }*/
