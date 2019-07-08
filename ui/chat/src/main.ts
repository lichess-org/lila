import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'

import makeCtrl from './ctrl';
import view from './view';
import { ChatOpts, Ctrl } from './interfaces'
import { PresetCtrl } from './preset'

import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

export { Ctrl as ChatCtrl, ChatPlugin } from './interfaces';

export default function LichessChat(element: Element, opts: ChatOpts): {
  preset: PresetCtrl
} {
  const patch = init([klass, attributes]);

  const container = element.parentNode as HTMLElement;

  let vnode: VNode, ctrl: Ctrl

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  window.Mousetrap.bind('c', () => {
    (container.querySelector('.mchat__say') as HTMLElement).focus();
    return false;
  });

  window.Mousetrap(container).bind('esc', () => {
    (container.querySelector('.mchat__say') as HTMLElement).blur();
  });

  return ctrl;
};
