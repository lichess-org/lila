import { attributesModule, classModule, init } from 'snabbdom';
import { RoundOpts, RoundData, RoundSocket } from 'round';
import { MoveRootCtrl } from 'game';
import { LocalCtrl } from './ctrl';
import { Libots } from './bots/interfaces';
import { ZfBot } from './bots/zfbot';
import { makeCtrl } from './bots/ctrl';
import makeZerofish from 'zerofish';
import { objectStorage } from 'common/objectStorage';
import view from './view';
import { LocalPlayOpts } from './interfaces';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts) {
  const localCode = navigator.language;
  let i18n = opts.i18n;
  try {
    const store = await objectStorage<any>({ db: 'i18n', store: `local` });
    if (i18n) await store.put(localCode, i18n);
    else i18n = await store.get(localCode);
  } catch (e) {
    console.log('oh noes!', e);
  }

  const zfAsync = makeZerofish({
    root: site.asset.url('npm', { documentOrigin: true }),
    wasm: site.asset.url('npm/zerofishEngine.wasm'),
  });

  const botsAsync = fetch(site.asset.url('bots.json')).then(x => x.json());

  const [zf, bots] = await Promise.all([zfAsync, botsAsync]);

  const libots: Libots = {};
  for (const bot of bots) {
    libots[bot.uid.slice(1)] = new ZfBot(bot, zf);
  }
  const libotCtrl = await makeCtrl(libots, zf);

  const ctrl = new LocalCtrl({ ...opts, i18n }, libotCtrl, () => {});
  ctrl.round = await site.asset.loadEsm<MoveRootCtrl>('round', { init: ctrl.roundOpts });
  const blueprint = view(ctrl);
  const el = document.createElement('main');
  document.body.appendChild(el);
  let vnode = patch(el, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }
  redraw();
}
