import { attributesModule, classModule, init } from 'snabbdom';
import { RoundOpts, RoundData, RoundSocket } from 'round';
import { MoveRootCtrl } from 'game';
import { LocalCtrl } from './localCtrl';
import { TestCtrl } from './testCtrl';
import { renderTestView } from './testView';
import { Libots } from './bots/interfaces';
import { BotCtrl } from './bots/botCtrl';
import { LocalDialog } from './setupDialog';
import makeZerofish from 'zerofish';
import { objectStorage } from 'common/objectStorage';
import view from './view';
import { LocalPlayOpts } from './interfaces';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts) {
  // async stuff that must be serialized is done so here
  const localCode = navigator.language;
  let i18n = opts.i18n;
  try {
    const store = await objectStorage<any>({ db: 'i18n', store: `local` });
    if (i18n) await store.put(localCode, i18n);
    else i18n = await store.get(localCode);
  } catch (e) {
    console.log('oh noes!', e);
  }

  const zfAsync = () =>
    makeZerofish({
      root: site.asset.url('npm', { documentOrigin: true }),
      wasm: site.asset.url('npm/zerofishEngine.wasm'),
    });

  const botsAsync = fetch(site.asset.url('bots.json')).then(x => x.json());

  const [zfWhite, zfBlack, bots] = await Promise.all([zfAsync(), zfAsync(), botsAsync]);

  const botCtrl = new BotCtrl(bots, { white: zfWhite, black: zfBlack });
  if (!opts.setup?.time) {
    new LocalDialog(bots, opts.setup, true);
    return;
  }
  await Promise.all([botCtrl.setBot('white', opts.setup.white), botCtrl.setBot('black', opts.setup.black)]);
  const ctrl = new LocalCtrl({ ...opts, i18n }, botCtrl, redraw);
  const testCtrl = opts.testUi && new TestCtrl(ctrl, redraw);
  const renderSide = testCtrl ? () => renderTestView(testCtrl) : () => undefined;
  const el = document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);
  let vnode = patch(el, view(ctrl, renderSide()));
  ctrl.round = await site.asset.loadEsm<MoveRootCtrl>('round', { init: ctrl.roundOpts });

  redraw();

  function redraw() {
    vnode = patch(vnode, view(ctrl, renderSide()));
    ctrl.round.redraw();
  }
}
