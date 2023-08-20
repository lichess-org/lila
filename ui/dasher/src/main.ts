import { Redraw } from './util';
import { DasherCtrl, makeCtrl } from './dasher';
import { loading, loaded } from './view';
import * as xhr from 'common/xhr';
import { init as initSnabbdom, VNode, classModule, attributesModule } from 'snabbdom';

const patch = initSnabbdom([classModule, attributesModule]);

export function load() {
  return lichess.loadEsm<DasherCtrl>('dasher');
}

export async function initModule() {
  let vnode: VNode,
    ctrl: DasherCtrl | undefined = undefined;

  const $el = $('#dasher_app').html(`<div class="initiating">${lichess.spinnerHtml}</div>`);
  const element = $el.empty()[0] as HTMLElement;
  const toggle = $('#top .dasher')[0] as HTMLElement;

  const redraw: Redraw = () => {
    vnode = patch(vnode || element, ctrl ? loaded(ctrl) : loading());
  };

  redraw();

  const data = await xhr.json('/dasher');
  ctrl = makeCtrl(data, redraw);
  redraw();

  new MutationObserver(_ => lichess.pubsub.emit('dasher.toggle', toggle.classList.contains('shown'))).observe(
    toggle,
    {
      attributes: true,
    },
  );

  return ctrl;
}
