import { attributesModule, classModule, init } from 'snabbdom';
import { RoundOpts, RoundData, RoundSocket } from 'round';
import { MoveRootCtrl } from 'game';
import { LocalCtrl } from './ctrl';
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

  const ctrl = new LocalCtrl({ ...opts, i18n }, () => {});
  await ctrl.loaded;
  ctrl.round = await site.asset.loadEsm<MoveRootCtrl>('round', { init: ctrl.roundOpts });
  const blueprint = view(ctrl);
  const element = document.querySelector('#bot-view') as HTMLElement;
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }
  redraw();
}
