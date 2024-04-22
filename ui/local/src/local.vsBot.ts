import { attributesModule, classModule, init } from 'snabbdom';
import { RoundOpts, RoundData, RoundSocket } from 'round';
import { MoveRootCtrl } from 'game';
import { VsBotCtrl } from './vsBotCtrl';
import view from './view';
import { LocalPlayOpts } from './interfaces';
import { makeVsBotSocket } from './vsBotSocket';
import { fakeData as data } from './data';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts) {
  const ctrl = new VsBotCtrl(opts, () => {});
  const moves: string[] = [];
  for (const from in ctrl.dests) {
    moves.push(from + ctrl.dests[from]);
  }
  const [round, _] = await Promise.all([
    site.asset.loadEsm<MoveRootCtrl>('round', { init: ctrl.roundOpts }),
    ctrl.loaded,
  ]);
  ctrl.round = round;
  const blueprint = view(ctrl);
  const element = document.querySelector('#bot-view') as HTMLElement;
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }
  redraw();
}
