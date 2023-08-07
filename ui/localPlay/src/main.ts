import { attributesModule, classModule, init } from 'snabbdom';
import { Ctrl } from './ctrl';
import view from './view';
import initBvb from './botVsBot/bvbMain';
import { LocalPlayOpts } from './interfaces';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts) {
  if (opts.mode === 'botVsBot') return initBvb(opts);

  const ctrl = new Ctrl(opts, () => {});
  const blueprint = view(ctrl);
  const element = document.querySelector('#bot-view') as HTMLElement;
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }
  redraw();
}
