import menuHover from 'common/menuHover';
import { Shogiground } from 'shogiground';
import { VNode, attributesModule, classModule, init } from 'snabbdom';
import StormCtrl from './ctrl';
import { StormOpts } from './interfaces';
import view from './view/main';

const patch = init([classModule, attributesModule]);

export function start(opts: StormOpts) {
  const element = document.querySelector('.storm-app') as HTMLElement;

  let vnode: VNode;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  const ctrl = new StormCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  menuHover();
  $('script').remove();
}

// that's for the rest of lishogi to access shogiground
// without having to include it a second time
window.Shogiground = Shogiground;
