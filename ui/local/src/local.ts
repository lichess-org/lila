import { attributesModule, classModule, init } from 'snabbdom';
import { RoundOpts, RoundData, RoundSocket } from 'round';
import { MoveRootCtrl } from 'game';
import { LocalCtrl } from './ctrl';
import view from './view';
import { LocalPlayOpts } from './interfaces';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts) {
  const ctrl = new LocalCtrl(opts, () => {});
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
