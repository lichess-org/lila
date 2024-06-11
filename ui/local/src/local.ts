import { attributesModule, classModule, init } from 'snabbdom';
//import { RoundOpts, RoundData, RoundSocket } from 'round';
import { MoveRootCtrl } from 'game';
import { GameCtrl } from './gameCtrl';
import { TestCtrl } from './testCtrl';
import { renderTestView } from './testView';
import { LocalPlayOpts, Libot } from './types';
import { BotCtrl } from './botCtrl';
import { LocalDialog } from './setupDialog';
import view from './gameView';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts) {
  const botCtrl = await new BotCtrl().init();
  if (opts.setup) {
    if (!opts.setup.go) {
      new LocalDialog(botCtrl.bots, opts.setup, true);
      return;
    }
    botCtrl.setPlayer('white', opts.setup.white);
    botCtrl.setPlayer('black', opts.setup.black);
  }

  const ctrl = new GameCtrl(opts, botCtrl, redraw);
  const testCtrl = opts.testUi && new TestCtrl(ctrl, redraw);
  const el = document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);
  const renderSide = testCtrl ? () => renderTestView(testCtrl) : () => undefined;
  let vnode = patch(el, view(ctrl, renderSide()));

  ctrl.round = await site.asset.loadEsm<MoveRootCtrl>('round', { init: ctrl.roundOpts });

  redraw();

  function redraw() {
    vnode = patch(vnode, view(ctrl, renderSide()));
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
