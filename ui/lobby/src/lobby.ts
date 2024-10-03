import { LobbyOpts } from './interfaces';
import { init, classModule, attributesModule, eventListenersModule } from 'snabbdom';
import { requestIdleCallback } from 'common';
import makeCtrl from './ctrl';
import { renderApp as appView } from './view/app';
import { renderButtons } from './view/buttons';
import { renderCounters } from './view/counters';
import { rotateBlogs } from './view/blog';

const patch = init([classModule, attributesModule, eventListenersModule]);

export function initModule(opts: LobbyOpts) {
  const ctrl = new makeCtrl(opts, redraw);

  const appElement = document.querySelector('.lobby__app') as HTMLElement;
  const countersElement = document.querySelector('.lobby__counters') as HTMLElement;
  const startElement = document.querySelector('.lobby__start') as HTMLElement;
  appElement.innerHTML = '';
  countersElement.innerHTML = '';
  startElement.innerHTML = '';
  let appVNode = patch(appElement, appView(ctrl));
  let countersVNode = patch(countersElement, renderCounters(ctrl));
  let buttonsVNode = patch(startElement, renderButtons(ctrl));

  let animationFrameId: number;

  requestIdleCallback(() => {
    layoutChanged();
    window.addEventListener('resize', layoutChanged);
  });

  function redraw() {
    appVNode = patch(appVNode, appView(ctrl));
    buttonsVNode = patch(buttonsVNode, renderButtons(ctrl));
    countersVNode = patch(countersVNode, renderCounters(ctrl));
  }

  function layoutChanged() {
    cancelAnimationFrame(animationFrameId);
    animationFrameId = requestAnimationFrame(() => rotateBlogs());
  }
}
