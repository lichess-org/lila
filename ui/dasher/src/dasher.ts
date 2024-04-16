import { Redraw } from 'common/snabbdom';
import { DasherCtrl } from './ctrl';
import * as xhr from 'common/xhr';
import { spinnerVdom } from 'common/spinner';
import { init as initSnabbdom, VNode, classModule, attributesModule, h } from 'snabbdom';

const patch = initSnabbdom([classModule, attributesModule]);

export function load() {
  return site.asset.loadEsm<DasherCtrl>('dasher');
}

export default async function initModule() {
  let vnode: VNode,
    ctrl: DasherCtrl | undefined = undefined;

  const $el = $('#dasher_app').html(`<div class="initiating">${site.spinnerHtml}</div>`);
  const element = $el.empty()[0] as HTMLElement;
  const toggle = $('#top .dasher')[0] as HTMLElement;

  const redraw: Redraw = () => {
    vnode = patch(
      vnode || element,
      h('div#dasher_app.dropdown', ctrl?.render() ?? h('div.initiating', spinnerVdom())),
    );
  };

  redraw();

  const data = await xhr.json('/dasher');
  ctrl = new DasherCtrl(data, redraw);
  redraw();

  new MutationObserver(_ => site.pubsub.emit('dasher.toggle', toggle.classList.contains('shown'))).observe(
    toggle,
    {
      attributes: true,
    },
  );

  return ctrl;
}
