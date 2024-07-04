import { attributesModule, classModule, init } from 'snabbdom';
//import { RoundOpts, RoundData, RoundSocket } from 'round';
import { MoveRootCtrl } from 'game';
import { GameCtrl } from '../gameCtrl';
import { DevCtrl } from './devCtrl';
import { renderDevView } from './devView';
import { LocalPlayOpts, Libot } from '../types';
import { BotCtrl } from '../botCtrl';
import { AssetDb } from '../assetDb';
import { SetupDialog } from '../setupDialog';
import renderGameView from '../gameView';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts) {
  const botCtrl = await new BotCtrl(opts.assets).init();
  if (opts.setup) {
    if (!opts.setup.go) {
      new SetupDialog(botCtrl, opts.setup, true);
      return;
    }
    botCtrl.setPlayer('white', opts.setup.white);
    botCtrl.setPlayer('black', opts.setup.black);
  }

  const gameCtrl = new GameCtrl(opts, botCtrl, redraw);
  const devCtrl = new DevCtrl(gameCtrl, redraw);

  const el = document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);

  let vnode = patch(el, renderGameView(gameCtrl, renderDevView(devCtrl)));

  gameCtrl.round = await site.asset.loadEsm<MoveRootCtrl>('round', { init: gameCtrl.roundOpts });
  redraw();

  function redraw() {
    vnode = patch(vnode, renderGameView(gameCtrl, renderDevView(devCtrl)));
    gameCtrl.round.redraw();
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
