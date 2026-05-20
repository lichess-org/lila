import { init as initSnabbdom, type VNode, classModule, attributesModule, h } from 'snabbdom';

import { spinnerVdom, spinnerHtml } from 'lib/view';
import { json as xhrJson } from 'lib/xhr';

import { DasherCtrl } from './ctrl';

const patch = initSnabbdom([classModule, attributesModule]);

export function load(): Promise<DasherCtrl> {
  return site.asset.loadEsm<DasherCtrl>('dasher');
}

export default async function initModule(): Promise<DasherCtrl> {
  let vnode: VNode,
    ctrl: DasherCtrl | undefined = undefined;

  const $el = $('#dasher_app').html(`<div class="initiating">${spinnerHtml}</div>`);
  const element = $el.empty()[0] as HTMLElement;

  const redraw: Redraw = () => {
    vnode = patch(
      vnode || element,
      h('div#dasher_app.dropdown', ctrl?.render() ?? h('div.initiating', spinnerVdom())),
    );
  };

  redraw();

  const data = await xhrJson('/dasher');
  ctrl = new DasherCtrl(data, redraw);
  redraw();

  return ctrl;
}
